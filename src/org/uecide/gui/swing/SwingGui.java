package org.uecide.gui.swing;

import org.uecide.gui.*;
import org.uecide.*;
import org.uecide.actions.*;
import org.uecide.gui.swing.laf.*;

import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;

import org.uecide.Compiler;
import org.uecide.Package;
import org.uecide.gui.swing.laf.LookAndFeel;


public class SwingGui extends Gui implements ContextEventListener {

    Splash splash;

    JFrame window;

    AbsoluteSplitPane leftmid;
    AbsoluteSplitPane midright;
    AbsoluteSplitPane topbottom;

    AutoTab leftPane;
    AutoTab centerPane;
    AutoTab rightPane;
    AutoTab bottomPane;

    MainToolbar toolbar;

    JMenuBar menu;

    FileMenu fileMenu;
    EditMenu editMenu;
    SketchMenu sketchMenu;
    HardwareMenu hardwareMenu;
    JMenu toolsMenu;
    JMenu helpMenu;

    SketchTreePanel sketchTree;

    Console console;

    public SwingGui(Context c) {
        super(c);
        ctx.listenForEvent("buildStart", this);
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
        toolsMenu = new JMenu("Tools");
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

        midright = new AbsoluteSplitPane(AbsoluteSplitPane.HORIZONTAL_SPLIT, centerPane, rightPane);
        leftmid = new AbsoluteSplitPane(AbsoluteSplitPane.HORIZONTAL_SPLIT, leftPane, midright);
        topbottom = new AbsoluteSplitPane(AbsoluteSplitPane.VERTICAL_SPLIT, leftmid, bottomPane);


        window.add(topbottom, BorderLayout.CENTER);

        toolbar = new MainToolbar(this);
        window.add(toolbar, BorderLayout.NORTH);

        window.setTitle("UECIDE :: " + ctx.getSketch().getName());
        window.setSize(300, 300);
        window.setVisible(true);

        midright.setRightSize(150);
        leftmid.setLeftSize(200);
        topbottom.setBottomSize(250);

        sketchTree = new SketchTreePanel(ctx);
        leftPane.add(sketchTree);

        console = new Console(ctx);
        bottomPane.add(console);

        System.err.println("Sorry, Swing GUI not implemented yet.");
    }

    @Override
    public void message(String m) {
        console.message(m);
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

    public void contextEventTriggered(String event, Context ctx) {
        flushDocumentData();
    }

    @Override
    public String askString(String question, String defaultValue) {
        Icon i = null;
        try { i = IconManager.getIcon(48, "misc.question"); } catch (IOException ignored) {}
        return (String)JOptionPane.showInputDialog(window, question, "Excuse me, but...", JOptionPane.QUESTION_MESSAGE, i, null, defaultValue);
    }

    @Override
    public void openSketchFileEditor(SketchFile f) {
        for (int i = 0; i < centerPane.getTabCount(); i++) {
            Component c = centerPane.getComponentAt(i);
            if (c instanceof CodeEditor) {
                CodeEditor ce = (CodeEditor)c;
                if (ce.getSketchFile() == f) {
                    centerPane.setSelectedIndex(i);
                    return;
                }
            }
        }

        CodeEditor ce = new CodeEditor(ctx, f);
        centerPane.add(ce);
    }

    public void flushDocumentData() {
        for (int i = 0; i < centerPane.getTabCount(); i++) {
            Component c = centerPane.getComponentAt(i);
            if (c instanceof CodeEditor) {
                CodeEditor ce = (CodeEditor)c;
                ce.flushData();
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
    public boolean askYesNo(String question) {
        Icon i = null;
        try { i = IconManager.getIcon(48, "misc.question"); } catch (IOException ignored) {}
        return (JOptionPane.showConfirmDialog(window, question, "Excuse me, but...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, i) == JOptionPane.YES_OPTION);
    }
}
