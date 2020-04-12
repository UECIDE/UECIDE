package org.uecide.gui.swing;

import org.uecide.gui.Gui;
import org.uecide.UECIDE;
import org.uecide.Context;
import org.uecide.ContextEventListener;
import org.uecide.ContextEvent;
import org.uecide.Debug;
import org.uecide.Preferences;
import org.uecide.actions.*;
import org.uecide.gui.swing.laf.*;
import org.uecide.Compiler;
import org.uecide.Package;
import org.uecide.SketchFile;
import org.uecide.gui.swing.laf.LookAndFeel;

import java.util.ArrayList;

import java.io.IOException;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;



public class SwingGui extends Gui implements ContextEventListener, TabChangeListener, TabMouseListener {

    Splash splash;

    JFrame window;

    AbsoluteSplitPane leftmid;
    AbsoluteSplitPane midright;
    AbsoluteSplitPane topbottom;

    AutoTab leftPane;
    AutoTab centerPane;
    AutoTab rightPane;
    AutoTab bottomPane;

    TabPanel lastActiveTab = null;

    ArrayList<AutoTab> panes = new ArrayList<AutoTab>();

    static LookAndFeel laf = null;

    MainToolbar toolbar;

    JMenuBar menu;

    FileMenu fileMenu;
    EditMenu editMenu;
    SketchMenu sketchMenu;
    HardwareMenu hardwareMenu;
    ToolsMenu toolsMenu;
    JMenu helpMenu;
    JMenu debugMenu;
    StatusBar statusBar;

    SketchTreePanel sketchTree;

    Console console;
    OutputPanel output;

    public SwingGui(Context c) {
        super(c);
        ctx.listenForEvent("sketchLoaded", this);
        ctx.listenForEvent("fileDataRead", this);
        ctx.listenForEvent("sketchDataModified", this);
    }

    @Override
    public void open() {

        window = new JFrame();
        window.setLayout(new BorderLayout());

        try {
            window.setIconImage(IconManager.getIcon(64, "internal:uecide").getImage());
        } catch (Exception ex) {
            Debug.exception(ex);
        }

        menu = new JMenuBar();

        fileMenu = new FileMenu(ctx);
        editMenu = new EditMenu(ctx);
        sketchMenu = new SketchMenu(ctx);
        hardwareMenu = new HardwareMenu(ctx);
        toolsMenu = new ToolsMenu(ctx);
        helpMenu = new JMenu("Help");
        debugMenu = new JMenu("Debug");

        helpMenu.add(debugMenu);

        int defaultModifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        JMenuItem commandPrompt = new JMenuItem("Show / hide command prompt");
        commandPrompt.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
        commandPrompt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                console.showHideCommandPrompt();
            }
        });

        debugMenu.add(commandPrompt);

        menu.add(fileMenu);
        menu.add(editMenu);
        menu.add(sketchMenu);
        menu.add(hardwareMenu);
        menu.add(toolsMenu);
        menu.add(helpMenu);

        window.setJMenuBar(menu);

        leftPane = new AutoTab();
        centerPane = new AutoTab();
        rightPane = new AutoTab();
        bottomPane = new AutoTab();

        leftPane.addTabMouseListener(this);
        centerPane.addTabMouseListener(this);
        rightPane.addTabMouseListener(this);
        bottomPane.addTabMouseListener(this);

        panes.add(leftPane);
        panes.add(centerPane);
        panes.add(rightPane);
        panes.add(bottomPane);

        centerPane.addTabChangeListener(this);
        
        // This set of wrapper JPanels are needed for the themes to work properly.
        JPanel leftPaneContainer = new JPanel();
        leftPaneContainer.setLayout(new BorderLayout());
        leftPaneContainer.add(leftPane, BorderLayout.CENTER);

        JPanel centerPaneContainer = new JPanel();
        centerPaneContainer.setLayout(new BorderLayout());
        centerPaneContainer.add(centerPane, BorderLayout.CENTER);

        JPanel rightPaneContainer = new JPanel();
        rightPaneContainer.setLayout(new BorderLayout());
        rightPaneContainer.add(rightPane, BorderLayout.CENTER);

        JPanel bottomPaneContainer = new JPanel();
        bottomPaneContainer.setLayout(new BorderLayout());
        bottomPaneContainer.add(bottomPane, BorderLayout.CENTER);

        midright = new AbsoluteSplitPane(AbsoluteSplitPane.HORIZONTAL_SPLIT, centerPaneContainer, rightPaneContainer);
        leftmid = new AbsoluteSplitPane(AbsoluteSplitPane.HORIZONTAL_SPLIT, leftPaneContainer, midright);
        topbottom = new AbsoluteSplitPane(AbsoluteSplitPane.VERTICAL_SPLIT, leftmid, bottomPaneContainer);


        window.add(topbottom, BorderLayout.CENTER);

        toolbar = new MainToolbar(this);
        window.add(toolbar, BorderLayout.NORTH);

        statusBar = new StatusBar(ctx);
        window.add(statusBar, BorderLayout.SOUTH);

        if (ctx.getSketch() != null) {
            window.setTitle("UECIDE :: " + ctx.getSketch().getName());
        } else {
            window.setTitle("UECIDE");
        }

        window.setSize(1280, 768);
        window.setVisible(true);

        midright.setRightSize(150);
        leftmid.setLeftSize(200);
        topbottom.setBottomSize(250);
        midright.hideRight();

        sketchTree = new SketchTreePanel(ctx, leftPane);
        leftPane.add(sketchTree);

        output = new OutputPanel(ctx, bottomPane);
        bottomPane.add(output);
        console = new Console(ctx, bottomPane);
        bottomPane.add(console);

        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ctx.action("closeSession");
            }
        });

        ctx.listenForEvent("oneSecondTimer", new ContextEventListener() {
            public void contextEventTriggered(ContextEvent evt) {
                updateTabs();
            }
        });

        System.err.println("Sorry, Swing GUI not fully implemented yet.");
    }

    @Override
    public void message(String m) {
        console.message(m);
    }

    @Override
    public void streamError(String m) {
        console.streamError(m);
    }

    @Override
    public void streamWarning(String m) {
        console.streamWarning(m);
    }

    @Override
    public void streamMessage(String m) {
        console.streamMessage(m);
    }

    @Override
    public void warning(String m) {
        console.warning(m);
    }

    @Override
    public void error(String m) {
        console.error(m);
    }

    @Override
    public void error(Throwable m) {
        if (console == null) {
            m.printStackTrace();
            return;
        }
        console.error(m.getMessage());
    }

    @Override
    public void heading(String m) {
        console.heading(m);
    }

    @Override
    public void command(String m) {
        console.command(m);
    }

    @Override
    public void bullet(String m) {
        console.bullet(m);
    }

    @Override
    public void bullet2(String m) {
        console.bullet2(m);
    }

    @Override
    public void bullet3(String m) {
        console.bullet3(m);
    }

    @Override
    public void openSplash() {
        splash = new Splash();
    }

    @Override
    public void closeSplash() {
        splash.dispose();
    }

    @Override
    public void splashMessage(String message, int percent) {
        splash.setMessage(message, percent);
    }

    public static void init() {
    }

    public static void endinit() {
        try {
            IconManager.loadIconSets();
            IconManager.setIconFamily(Preferences.get("theme.icons"));
            setLookAndFeel();
        } catch (IOException ex) {
            Debug.exception(ex);
            ex.printStackTrace();
        }
    }

    public static void setLookAndFeel() {
        String lafname = Preferences.get("theme.laf");

        if (UECIDE.cli.isSet("laf")) {
            lafname = UECIDE.cli.getString("laf")[0];
        }

        laf = null;

        switch (lafname) {
            case "gnome": laf = new GnomeLAF(); break;
            case "acryl": laf = new JTattooAcrylLAF(); break;
            case "aero": laf = new JTattooAeroLAF(); break;
            case "aluminium": laf = new JTattooAluminiumLAF(); break;
            case "bernstein": laf = new JTattooBernsteinLAF(); break;
            case "fast": laf = new JTattooFastLAF(); break;
            case "graphite": laf = new JTattooGraphiteLAF(); break;
            case "hifi": laf = new JTattooHiFiLAF(); break;
            case "luna": laf = new JTattooLunaLAF(); break;
            case "mcwin": laf = new JTattooMcWinLAF(); break;
            case "mint": laf = new JTattooMintLAF(); break;
            case "noire": laf = new JTattooNoireLAF(); break;
            case "smart": laf = new JTattooSmartLAF(); break;
            case "liquid": laf = new LiquidLAF(); break;
            case "metal": laf = new MetalLAF(); break;
            case "motif": laf = new MotifLAF(); break;
            case "nimbus": laf = new NimbusLAF(); break;
            case "office2003": laf = new Office2003LAF(); break;
            case "officexp": laf = new OfficeXPLAF(); break;
            case "systemdefault": laf = new SystemDefaultLAF(); break;
            case "tinyforest": laf = new TinyForestLAF(); break;
            case "tinygolden": laf = new TinyGoldenLAF(); break;
            case "tinynightly": laf = new TinyNightlyLAF(); break;
            case "tinyplastic": laf = new TinyPlasticLAF(); break;
            case "tinysilver": laf = new TinySilverLAF(); break;
            case "tinyunicode": laf = new TinyUnicodeLAF(); break;
            case "vs2005": laf = new VisualStudio2005LAF(); break;
            case "material": laf = new MaterialLAF(); break;
            case "arduino": laf = new ArduinoLAF(); break;
            default:
                laf = new SystemDefaultLAF();
        }

        laf.applyLAF();
    }

    static String jTattooTheme(String name) {
        String fontData = Preferences.get("theme.jtattoo.aafont");
        String colourData = Preferences.get("theme.jtattoo.themes." + name);

        if (fontData == null) fontData = "Default";
        if (colourData == null) colourData = "Default";

        String full = colourData + "-" + fontData;

        full = full.replace("Default-", "");
        full = full.replace("-Default", "");

        return full;
    }

    boolean tabsNeedUpdating = false;

    public void updateTabs() {
        if (tabsNeedUpdating) {
            tabsNeedUpdating = false;
            for (AutoTab p : panes) {
                for (int i = 0; i < p.getTabCount(); i++) {
                    TabPanel pan = (TabPanel)p.getComponentAt(i);
                    p.setTabComponentAt(i, pan.getTab());
                }
            }
        }
    }

    public void contextEventTriggered(ContextEvent e) {
        if (e.getEvent().equals("sketchDataModified")) {
            tabsNeedUpdating = true;
        }
        
        if (e.getEvent().equals("fileDataRead")) {
            flushDocumentData();
            return;
        }

        if (e.getEvent().equals("sketchLoaded")) {
            window.setTitle("UECIDE :: " + ctx.getSketch().getName());
            return;
        }
    }

    @Override
    public String askString(String question, String defaultValue) {
        CleverIcon i = null;
        try { 
            i = IconManager.getIcon(48, "misc.input"); 
        } catch (IOException ignored) {
            Debug.exception(ignored);
        }
        FancyDialog dialog = new FancyDialog(window, "Excuse me, but...", question, i, FancyDialog.INPUT_OKCANCEL);
        if (dialog.getResult() == FancyDialog.ANSWER_OK) {
            return dialog.getText();
        } else {
            return null;
        }
    }

    @Override
    public String askPassword(String question, String defaultValue) {
        CleverIcon i = null;
        try { 
            i = IconManager.getIcon(48, "misc.input"); 
        } catch (IOException ignored) {
            Debug.exception(ignored);
        }
        FancyDialog dialog = new FancyDialog(window, "Excuse me, but...", question, i, FancyDialog.PASSWORD_OKCANCEL);
        if (dialog.getResult() == FancyDialog.ANSWER_OK) {
            return dialog.getText();
        } else {
            return null;
        }
    }

    @Override
    public void openSketchFileEditor(SketchFile f) {
        for (AutoTab pane : panes) {
            for (int i = 0; i < pane.getTabCount(); i++) {
                Component c = pane.getComponentAt(i);
                if (c instanceof CodeEditor) {
                    CodeEditor ce = (CodeEditor)c;
                    if (ce.getSketchFile() == f) {
                        pane.setSelectedIndex(i);
                        ce.requestFocus();
                        return;
                    }
                }
            }
        }

        CodeEditor ce = new CodeEditor(ctx, centerPane, f);
        centerPane.add(ce);
        centerPane.setSelectedComponent(ce);
        ce.requestFocus();
    }

    @Override
    public void closeSketchFileEditor(SketchFile f) {
        flushDocumentData();
        ArrayList<AutoTab> toRemove = new ArrayList<AutoTab>();
        for (AutoTab pane : panes) {
            for (int i = 0; i < pane.getTabCount(); i++) {
                Component c = pane.getComponentAt(i);
                if (c instanceof CodeEditor) {
                    CodeEditor ce = (CodeEditor)c;
                    if (ce.getSketchFile() == f) {
                        pane.remove(ce);
                        if (pane.getTabCount() == 0) {
                            if (pane.isSeparateWindow()) {
                                toRemove.add(pane);
                            }
                        }
                    }
                }
            }
        }
        for (AutoTab pane : toRemove) {
            panes.remove(pane);
            pane.getParentWindow().dispose();
        }

        if (rightPane.getTabCount() == 0) {
            midright.hideRight();
        }
    }

    public void flushDocumentData() {
        for (AutoTab pane : panes) {
            for (int i = 0; i < pane.getTabCount(); i++) {
                Component c = pane.getComponentAt(i);
                if (c instanceof CodeEditor) {
                    CodeEditor ce = (CodeEditor)c;
                    ce.flushData();
                }
            }
        }
    }

    @Override
    public File askSketchFilename(String question, File location) {
        SketchFolderFilter filter = new SketchFolderFilter();
        SketchFileView view = new SketchFileView();
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(filter);
        fc.setFileView(view);
        fc.setCurrentDirectory(location);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int rv = fc.showSaveDialog(window);

        if (rv == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }

    @Override
    public File askOpenSketch(String question, File location) {
        SketchFolderFilter filter = new SketchFolderFilter();
        SketchFileView view = new SketchFileView();
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(filter);
        fc.setFileView(view);
        fc.setCurrentDirectory(location);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int rv = fc.showOpenDialog(window);

        if (rv == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }

    @Override
    public boolean askYesNo(String question) {
        CleverIcon i = null;
        try { 
            i = IconManager.getIcon(48, "misc.question"); 
        } catch (IOException ignored) {
            Debug.exception(ignored);
        }
        FancyDialog dialog = new FancyDialog(window, "Excuse me, but...", question, i, FancyDialog.QUESTION_YESNO);
        return dialog.getResult() == FancyDialog.ANSWER_YES;

//        return (JOptionPane.showConfirmDialog(window, question, "Excuse me, but...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, i) == JOptionPane.YES_OPTION);
    }

    @Override
    public void alert(String message) {
        CleverIcon i = null;
        try { 
            i = IconManager.getIcon(48, "misc.error"); 
        } catch (IOException ignored) {
            Debug.exception(ignored);
        }
        FancyDialog dialog = new FancyDialog(window, "Excuse me, but...", message, i, FancyDialog.ALERT);
        return;
    }

    @Override
    public int askYesNoCancel(String question) {
        CleverIcon i = null;
        try { 
            i = IconManager.getIcon(48, "misc.question"); 
        } catch (IOException ignored) {
            Debug.exception(ignored);
        }
        FancyDialog dialog = new FancyDialog(window, "Excuse me, but...", question, i, FancyDialog.QUESTION_YESNOCANCEL);
        if (dialog.getResult() == FancyDialog.ANSWER_YES) return 0;
        if (dialog.getResult() == FancyDialog.ANSWER_NO) return 1;
        if (dialog.getResult() == FancyDialog.ANSWER_CANCEL) return 2;

        return -1;
    }

    public void close() {
        window.dispose();
        UECIDE.cleanupSession(ctx);
    }

    public void tabAdded(TabChangeEvent e) {
    }

    public void tabRemoved(TabChangeEvent e) {
    }

    public void mouseTabEntered(TabPanel p, MouseEvent evt) {
    }

    public void mouseTabExited(TabPanel p, MouseEvent evt) {
    }

    public void mouseTabPressed(TabPanel p, MouseEvent evt) {
        if (evt.getButton() == 3) {

    
            JPopupMenu menu = new JPopupMenu();
            JMenu moveMenu = new JMenu("Move to");
            menu.add(moveMenu);
            JMenuItem moveLeft = new JMenuItem("Left panel");
            moveLeft.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent inner) {
                    AutoTab parentTabs = null;
                    if (p.getParent() instanceof AutoTab) {
                        parentTabs = (AutoTab)p.getParent();
                    }
                    leftPane.add(p);
                    if ((parentTabs != null) && parentTabs.isSeparateWindow()) {
                        if (parentTabs.getTabCount() == 0) {
                            parentTabs.getParentWindow().dispose();
                        }
                    }
                    if (rightPane.getTabCount() == 0) {
                        midright.hideRight();
                    }
                }
            });
            moveMenu.add(moveLeft);
            JMenuItem moveCenter = new JMenuItem("Center panel");
            moveCenter.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent inner) {
                    AutoTab parentTabs = null;
                    if (p.getParent() instanceof AutoTab) {
                        parentTabs = (AutoTab)p.getParent();
                    }
                    centerPane.add(p);
                    if ((parentTabs != null) && parentTabs.isSeparateWindow()) {
                        if (parentTabs.getTabCount() == 0) {
                            parentTabs.getParentWindow().dispose();
                        }
                    }
                    if (rightPane.getTabCount() == 0) {
                        midright.hideRight();
                    }
                }
            });
            moveMenu.add(moveCenter);
            JMenuItem moveRight = new JMenuItem("Right panel");
            moveRight.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent inner) {
                    midright.showBoth();
                    AutoTab parentTabs = null;
                    if (p.getParent() instanceof AutoTab) {
                        parentTabs = (AutoTab)p.getParent();
                    }
                    rightPane.add(p);
                    if ((parentTabs != null) && parentTabs.isSeparateWindow()) {
                        if (parentTabs.getTabCount() == 0) {
                            parentTabs.getParentWindow().dispose();
                        }
                    }
                }
            });
            moveMenu.add(moveRight);
            JMenuItem moveBottom = new JMenuItem("Bottom panel");
            moveBottom.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent inner) {
                    AutoTab parentTabs = null;
                    if (p.getParent() instanceof AutoTab) {
                        parentTabs = (AutoTab)p.getParent();
                    }
                    bottomPane.add(p);
                    if ((parentTabs != null) && parentTabs.isSeparateWindow()) {
                        if (parentTabs.getTabCount() == 0) {
                            parentTabs.getParentWindow().dispose();
                        }
                    }
                    if (rightPane.getTabCount() == 0) {
                        midright.hideRight();
                    }
                }
            });
            moveMenu.add(moveBottom);
            moveMenu.addSeparator();
            JMenuItem newWindow = new JMenuItem("Separate window");
            newWindow.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent inner) {

                    AutoTab parentTabs = null;
                    if (p.getParent() instanceof AutoTab) {
                        parentTabs = (AutoTab)p.getParent();
                    }

                    JFrame f = new JFrame();
                    try {
                        f.setIconImage(IconManager.getIcon(64, "internal:uecide").getImage());
                    } catch (Exception ex) {
                        Debug.exception(ex);
                    }
 
                    JPanel pan = new JPanel();
                    AutoTab t = new AutoTab();
                    t.setSeparateWindow(true);
                    t.setParentWindow(f);
                    pan.setLayout(new BorderLayout());
                    pan.add(t, BorderLayout.CENTER);
                    f.setLayout(new BorderLayout());
                    f.add(pan, BorderLayout.CENTER);

                    panes.add(t);
                    t.add(p);
                    f.setVisible(true);

                    t.addTabMouseListener(SwingGui.this);
                    f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

                    f.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {

                            if (e.getSource() instanceof JFrame) {
                                JFrame win = (JFrame)e.getSource();
                                Component[] comps = win.getContentPane().getComponents();
                                for (Component comp : comps) {
                                    if (comp instanceof JPanel) {
                                        Component[] comps2 = ((JPanel)comp).getComponents();
                                        for (Component comp2 : comps2) {
                                            if (comp2 instanceof AutoTab) {
                                                AutoTab at = (AutoTab)comp2;
                                                while (at.getTabCount() > 0) {
                                                    TabPanel pan = (TabPanel)at.getComponentAt(0);
                                                    if (pan instanceof CodeEditor) {
                                                        CodeEditor ce = (CodeEditor)pan;
                                                        ctx.triggerEvent("fileDataRead", ce.getSketchFile());
                                                    }
                                                    pan.reset();
                                                }
                                                //for (int i = 0; i < at.getTabCount(); i++) {
                                                //    if (at.getComponentAt(i) instanceof CodeEditor) {
                                                //        SketchFile f = ((CodeEditor)at.getComponentAt(i)).getSketchFile();
                                                //        if (!(ctx.action("closeSketchFile", f))) {
                                                //            return;
                                                //        }
                                                //    }
                                                //}
                                                panes.remove(at);
                                            }
                                        }
                                    }
                                }
                                win.dispose();
                            }
                        }
                    });

                    if ((parentTabs != null) && parentTabs.isSeparateWindow()) {
                        if (parentTabs.getTabCount() == 0) {
                            panes.remove(parentTabs);
                            parentTabs.getParentWindow().dispose();
                        }
                    }
                    if (rightPane.getTabCount() == 0) {
                        midright.hideRight();
                    }
                }
            });
            moveMenu.add(newWindow);
            menu.show(p.getTab(), evt.getX(), evt.getY());
        }
    }

    public void mouseTabReleased(TabPanel p, MouseEvent evt) {
    }

    public void mouseTabClicked(TabPanel p, MouseEvent evt) {
        AutoTab t = getPanelByTab(p);
        if (t == null) return;
        t.setSelectedComponent(p);
    }

    public AutoTab getPanelByTab(TabPanel p) {
        for (AutoTab panel : panes) {
            int i = panel.indexOfComponent(p);
            if (i > -1) return panel;
        }
        return null;
    }

    public void navigateToLine(SketchFile f, Integer lineno) {
        for (AutoTab pane : panes) {
            for (int i = 0; i < pane.getTabCount(); i++) {
                Component c = pane.getComponentAt(i);
                if (c instanceof CodeEditor) {
                    CodeEditor ce = (CodeEditor)c;
                    if (ce.getSketchFile() == f) {
                        pane.setSelectedIndex(i);
                        ce.requestFocus();
                        ce.gotoLine(lineno);
                        return;
                    }
                }
            }
        }
    }

    public static LookAndFeel getLAF() {
        return laf;
    }

    public AutoTab getDefaultTab() {
        return centerPane;
    }

    public boolean shouldAutoOpen() {
        return true;
    }

    public void setActiveTab(TabPanel p) {
        lastActiveTab = p;
    }

    public TabPanel getActiveTab() {
        return lastActiveTab;
    }

    public static JFrame getFrameForComponent(Component c) {
        while (c != null) {
            if (c instanceof JFrame) return (JFrame)c;
            c = c.getParent();
        }
        return null;
    }

    public JFrame getFrame() {
        return window;
    }

    @Override
    public SketchFile getActiveSketchFile() {
        if (lastActiveTab == null) return null;
        if (lastActiveTab instanceof CodeEditor) {
            CodeEditor ce = (CodeEditor)lastActiveTab;
            return ce.getSketchFile();
        }
        return null;
    }

}
