package org.uecide.gui.swing;

import org.uecide.gui.*;
import org.uecide.*;
import org.uecide.actions.*;
import org.uecide.gui.swing.laf.*;

import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.uecide.Compiler;
import org.uecide.Package;
import org.uecide.gui.swing.laf.LookAndFeel;


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

    ArrayList<AutoTab> panes = new ArrayList<AutoTab>();

    MainToolbar toolbar;

    JMenuBar menu;

    FileMenu fileMenu;
    EditMenu editMenu;
    SketchMenu sketchMenu;
    HardwareMenu hardwareMenu;
    ToolsMenu toolsMenu;
    JMenu helpMenu;

    SketchTreePanel sketchTree;

    Console console;

    public SwingGui(Context c) {
        super(c);
        ctx.listenForEvent("sketchLoaded", this);
        ctx.listenForEvent("fileDataRead", this);
    }

    @Override
    public void open() {

        window = new JFrame();
        window.setLayout(new BorderLayout());

        menu = new JMenuBar();

        fileMenu = new FileMenu(ctx);
        editMenu = new EditMenu(ctx);
        sketchMenu = new SketchMenu(ctx);
        hardwareMenu = new HardwareMenu(ctx);
        toolsMenu = new ToolsMenu(ctx);
        helpMenu = new JMenu("Help");

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

        window.setTitle("UECIDE :: " + ctx.getSketch().getName());
        window.setSize(300, 300);
        window.setVisible(true);

        midright.setRightSize(150);
        leftmid.setLeftSize(200);
        topbottom.setBottomSize(250);

        sketchTree = new SketchTreePanel(ctx, leftPane);
        leftPane.add(sketchTree);

        console = new Console(ctx, bottomPane);
        bottomPane.add(console);

        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ctx.action("closeSession");
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
            ex.printStackTrace();
        }
    }

    public static void setLookAndFeel() {
        String lafname = Preferences.get("theme.laf");

        LookAndFeel laf = null;

        try {

            if (lafname.equals("gnome")) {
                new GnomeLAF().applyLAF();
            } else if (lafname.equals("acryl")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.acryl.AcrylLookAndFeel.setTheme(jTattooTheme("acryl"));
                com.jtattoo.plaf.acryl.AcrylLookAndFeel.setTheme(p);
                new JTattooAcrylLAF().applyLAF();
            } else if (lafname.equals("aero")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.aero.AeroLookAndFeel.setTheme(jTattooTheme("aero"));
                com.jtattoo.plaf.aero.AeroLookAndFeel.setTheme(p);
                new JTattooAeroLAF().applyLAF();
            } else if (lafname.equals("aluminium")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.aluminium.AluminiumLookAndFeel.setTheme(jTattooTheme("aluminium"));
                com.jtattoo.plaf.aluminium.AluminiumLookAndFeel.setTheme(p);
                new JTattooAluminiumLAF().applyLAF();
            } else if (lafname.equals("bernstein")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.bernstein.BernsteinLookAndFeel.setTheme(jTattooTheme("bernstein"));
                com.jtattoo.plaf.bernstein.BernsteinLookAndFeel.setTheme(p);
                new JTattooBernsteinLAF().applyLAF();
            } else if (lafname.equals("fast")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.fast.FastLookAndFeel.setTheme(jTattooTheme("fast"));
                com.jtattoo.plaf.fast.FastLookAndFeel.setTheme(p);
                new JTattooFastLAF().applyLAF();
            } else if (lafname.equals("graphite")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.graphite.GraphiteLookAndFeel.setTheme(jTattooTheme("graphite"));
                com.jtattoo.plaf.graphite.GraphiteLookAndFeel.setTheme(p);
                new JTattooGraphiteLAF().applyLAF();
            } else if (lafname.equals("hifi")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.hifi.HiFiLookAndFeel.setTheme(jTattooTheme("hifi"));
                com.jtattoo.plaf.hifi.HiFiLookAndFeel.setTheme(p);
                new JTattooHiFiLAF().applyLAF();
            } else if (lafname.equals("luna")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.luna.LunaLookAndFeel.setTheme(jTattooTheme("luna"));
                com.jtattoo.plaf.luna.LunaLookAndFeel.setTheme(p);
                new JTattooLunaLAF().applyLAF();
            } else if (lafname.equals("mcwin")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.mcwin.McWinLookAndFeel.setTheme(jTattooTheme("mcwin"));
                com.jtattoo.plaf.mcwin.McWinLookAndFeel.setTheme(p);
                new JTattooMcWinLAF().applyLAF();
            } else if (lafname.equals("mint")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.mint.MintLookAndFeel.setTheme(jTattooTheme("mint"));
                com.jtattoo.plaf.mint.MintLookAndFeel.setTheme(p);
                new JTattooMintLAF().applyLAF();
            } else if (lafname.equals("noire")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.noire.NoireLookAndFeel.setTheme(jTattooTheme("noire"));
                com.jtattoo.plaf.noire.NoireLookAndFeel.setTheme(p);
                new JTattooNoireLAF().applyLAF();
            } else if (lafname.equals("smart")) {
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.smart.SmartLookAndFeel.setTheme(jTattooTheme("smart"));
                com.jtattoo.plaf.smart.SmartLookAndFeel.setTheme(p);
                new JTattooSmartLAF().applyLAF();
            }
            else if (lafname.equals("liquid")) { new LiquidLAF().applyLAF(); }
            else if (lafname.equals("metal")) { new MetalLAF().applyLAF(); }
            else if (lafname.equals("motif")) { new MotifLAF().applyLAF(); }
            else if (lafname.equals("nimbus")) { new NimbusLAF().applyLAF(); }
            else if (lafname.equals("office2003")) { new Office2003LAF().applyLAF(); }
            else if (lafname.equals("officexp")) { new OfficeXPLAF().applyLAF(); }
            else if (lafname.equals("systemdefault")) { new SystemDefaultLAF().applyLAF(); }
            else if (lafname.equals("tinyforest")) { new TinyForestLAF().applyLAF(); }
            else if (lafname.equals("tinygolden")) { new TinyGoldenLAF().applyLAF(); }
            else if (lafname.equals("tinynightly")) { new TinyNightlyLAF().applyLAF(); }
            else if (lafname.equals("tinyplastic")) { new TinyPlasticLAF().applyLAF(); }
            else if (lafname.equals("tinysilver")) { new TinySilverLAF().applyLAF(); }
            else if (lafname.equals("tinyunicode")) { new TinyUnicodeLAF().applyLAF(); }
            else if (lafname.equals("vs2005")) { new VisualStudio2005LAF().applyLAF(); }
            else if (lafname.equals("material")) { new MaterialLAF().applyLAF(); }
            else if (lafname.equals("arduino")) { new ArduinoLAF().applyLAF(); }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    public void contextEventTriggered(ContextEvent e) {
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
        try { i = IconManager.getIcon(48, "misc.input"); } catch (IOException ignored) {}
        FancyDialog dialog = new FancyDialog(window, "Excuse me, but...", question, i, FancyDialog.INPUT_OKCANCEL);
        if (dialog.getResult() == FancyDialog.ANSWER_OK) {
            return dialog.getText();
        } else {
            return null;
        }

//        Icon i = null;
//        try { i = IconManager.getIcon(48, "misc.question"); } catch (IOException ignored) {}
//        return (String)JOptionPane.showInputDialog(window, question, "Excuse me, but...", JOptionPane.QUESTION_MESSAGE, i, null, defaultValue);
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
                        if (pane.isSeparateWindow()) {
                            if (pane.getTabCount() == 0) {
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
        try { i = IconManager.getIcon(48, "misc.question"); } catch (IOException ignored) {}
        FancyDialog dialog = new FancyDialog(window, "Excuse me, but...", question, i, FancyDialog.QUESTION_YESNO);
        return dialog.getResult() == FancyDialog.ANSWER_YES;

//        return (JOptionPane.showConfirmDialog(window, question, "Excuse me, but...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, i) == JOptionPane.YES_OPTION);
    }

    @Override
    public int askYesNoCancel(String question) {
System.err.println("YesNoCancel");
        CleverIcon i = null;
        try { i = IconManager.getIcon(48, "misc.question"); } catch (IOException ignored) {}
        FancyDialog dialog = new FancyDialog(window, "Excuse me, but...", question, i, FancyDialog.QUESTION_YESNOCANCEL);
        if (dialog.getResult() == FancyDialog.ANSWER_YES) return 0;
        if (dialog.getResult() == FancyDialog.ANSWER_NO) return 1;
        if (dialog.getResult() == FancyDialog.ANSWER_CANCEL) return 2;

//        Icon i = null;
//        try { i = IconManager.getIcon(48, "misc.question"); } catch (IOException ignored) {}
//        int rv = JOptionPane.showConfirmDialog(window, question, "Excuse me, but...", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, i);
//        if (rv == JOptionPane.YES_OPTION) return 0;
//        if (rv == JOptionPane.NO_OPTION) return 1;
//        if (rv == JOptionPane.CANCEL_OPTION) return 2;
        return -1;
    }

    public void close() {
        window.dispose();
        Base.cleanupSession(ctx);
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
                }
            });
            moveMenu.add(moveCenter);
            JMenuItem moveRight = new JMenuItem("Right panel");
            moveRight.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent inner) {
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
                                                ctx.triggerEvent("fileDataRead");
                                                while (at.getTabCount() > 0) {
                                                    TabPanel pan = (TabPanel)at.getComponentAt(0);
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

    AutoTab getPanelByTab(TabPanel p) {
        int i = leftPane.indexOfComponent(p);
        if (i > -1) return leftPane;
        i = centerPane.indexOfComponent(p);
        if (i > -1) return centerPane;
        i = rightPane.indexOfComponent(p);
        if (i > -1) return rightPane;
        i = bottomPane.indexOfComponent(p);
        if (i > -1) return bottomPane;
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
}
