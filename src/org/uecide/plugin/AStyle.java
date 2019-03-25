package org.uecide.plugin;

import org.uecide.*;
import org.uecide.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;
import java.beans.*;


public class AStyle extends Plugin {

    static final int BLOCK_MAXLEN = 1024;

    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) {
        pluginInfo = info;
    }
    public static String getInfo(String item) {
        return pluginInfo.get(item);
    }

    public AStyle(Editor e) {
        editor = e;
    }
    public AStyle(EditorBase e) {
        editorTab = e;
    }

    public void run() {
        try {
            if (editor == null) {
                return;
            }

            String text = editorTab.getText();
            String baseStyle = Preferences.get("plugins.astyle.format");

            File temp = File.createTempFile("astyle-reformat",".tmp");
            PrintWriter pw = new PrintWriter(temp);
            pw.print(text);
            pw.close();

            ArrayList<String> command = new ArrayList<String>();

            String exeFilename = extractProgram();
            if (exeFilename == null) {
                return;
            }

            command.add(exeFilename); //Base.preferences.get("astyle.path", "/usr/bin/astyle"));
            command.add("--style=" + baseStyle);

            if (Preferences.getBoolean("plugins.astyle.padblocks")) command.add("--break-blocks");
            if (Preferences.getBoolean("plugins.astyle.padoperators")) command.add("--pad-oper");
            if (Preferences.getBoolean("plugins.astyle.padheader")) command.add("--pad-header");
            if (Preferences.getBoolean("plugins.astyle.addbrackets")) command.add("--add-brackets");
            if (Preferences.getBoolean("plugins.astyle.addonelinebrackets")) command.add("--add-one-line-brackets");

            if (Preferences.getBoolean("editor.expandtabs")) {
                command.add("--indent=spaces=" + Preferences.getInteger("editor.tabsize"));
            } else {
                command.add("--indent=tab");
            }

            if (Preferences.getBoolean("plugins.astyle.padparenthesis")) {
                command.add("--pad-paren");
            } else {
                command.add("--unpad-paren");
            }
            command.add("--align-pointer=" + Preferences.get("plugins.astyle.pointeralign"));

            command.add(temp.getAbsolutePath());

            String out = "";
            for (String c : command) {
                if (!out.equals("")) {
                    out += "::";
                }
                out += c;
            }

            Sketch sketch = editor.getSketch();
            Context ctx = sketch.getContext();
            ctx.set("astyle.execute.command", out);
            ctx.executeKey("astyle.execute.command");

            BufferedReader reader = new BufferedReader(new FileReader(temp));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            int pos = editorTab.getCursorPosition();
            editorTab.setText(sb.toString());
            editorTab.setCursorPosition(pos);

            temp.delete();

        } catch (Exception e) {
            Base.error(e);
        }
    }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_TOOLS | Plugin.MENU_BOTTOM)) {
            JMenuItem item = new JMenuItem("Auto Format with Artistic Style");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int tn = editor.getActiveTab();
                    if (tn == -1) {
                        return;
                    }
                    editorTab = editor.getTab(tn);
                    run();
                }
            });
            menu.add(item);
        }
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
        if (flags == TOOLBAR_TAB) {

            try {
                toolbar.add(new ToolbarButton("apps.astyle", "Auto Format with Artistic Style", 16, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int tn = editor.getActiveTab();
                        if (tn == -1) {
                            return;
                        }
                        editorTab = editor.getTab(tn);
                        run();
                    }
                }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void populatePreferences(JPanel p) {
    }

    public static String getPreferencesTitle() {
        return null;
    }

    public static void savePreferences() {
    }

    public String extractProgram() {
        String res = null;
        String dest = null;
        if (Base.isWindows()) {
            res = "/org/uecide/plugin/AStyle/command/windows/astyle.exe";
            dest = "astyle.exe";
        } else if (Base.isMacOS()) {
            res = "/org/uecide/plugin/AStyle/command/macosx/astyle";
            dest = "astyle";
        } else if (Base.isLinux()) {
            if (Base.getOSArch().equals("armhf")) {
                res = "/org/uecide/plugin/AStyle/command/linux_armhf/astyle";
                dest = "astyle";
        } else {
                res = "/org/uecide/plugin/AStyle/command/linux/astyle";
                dest = "astyle";
            }
        } else {
            return null;
        }

        File dfile = new File(Base.getTmpDir(), dest);
        dfile.deleteOnExit();
        if (dfile.exists()) {
            return dfile.getAbsolutePath();
        }

        try {
            InputStream from = AStyle.class.getResourceAsStream(res);
            OutputStream to = new BufferedOutputStream(new FileOutputStream(dfile));
            byte[] buffer = new byte[16 * 1024];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }
            to.flush();
            from.close();
            from = null;
            to.close();
            to = null;

            dfile.setExecutable(true);
        } catch (Exception e) {
            Base.error(e);
        }


        return dfile.getAbsolutePath();
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

    public static PropertyFile getPreferencesTree() {
        PropertyFile f = new PropertyFile();

        f.set("plugins.name", "Plugins");
        f.set("plugins.type", "section");
        f.set("plugins.astyle.name", "Artistic Style");
        f.set("plugins.astyle.type", "section");

        f.set("plugins.astyle.format.name", "Base Format");
        f.set("plugins.astyle.format.type", "dropdown");
        f.set("plugins.astyle.format.default", "java");
        f.set("plugins.astyle.format.options.allman", "Allman / BSD / ANSI");
        f.set("plugins.astyle.format.options.java", "Java");
        f.set("plugins.astyle.format.options.kr", "Kernighan  &  Ritchie");
        f.set("plugins.astyle.format.options.stroustrup", "Stroustrup");
        f.set("plugins.astyle.format.options.whitesmith", "Whitesmith");
        f.set("plugins.astyle.format.options.banner", "Banner");
        f.set("plugins.astyle.format.options.gnu", "GNU");
        f.set("plugins.astyle.format.options.linux", "Linux");
        f.set("plugins.astyle.format.options.horstmann", "Horstmann");
        f.set("plugins.astyle.format.options.1tbs", "One True Brace");
        f.set("plugins.astyle.format.options.pico", "Pico");
        f.set("plugins.astyle.format.options.lisp", "Lisp / Python");

        f.set("plugins.astyle.padblocks.name", "Pad Blocks");
        f.set("plugins.astyle.padblocks.type", "checkbox");
        f.set("plugins.astyle.padblocks.default", "true");

        f.set("plugins.astyle.padparenthesis.name", "Pad Parenthesis");
        f.set("plugins.astyle.padparenthesis.type", "checkbox");
        f.set("plugins.astyle.padparenthesis.default", "false");

        f.set("plugins.astyle.padheader.name", "Pad Headers");
        f.set("plugins.astyle.padheader.type", "checkbox");
        f.set("plugins.astyle.padheader.default", "false");

        f.set("plugins.astyle.addbrackets.name", "Add Brackets");
        f.set("plugins.astyle.addbrackets.type", "checkbox");
        f.set("plugins.astyle.addbrackets.default", "true");

        f.set("plugins.astyle.addonelinebrackets.name", "Add One-Line Brackets");
        f.set("plugins.astyle.addonelinebrackets.type", "checkbox");
        f.set("plugins.astyle.addonelinebrackets.default", "true");

        f.set("plugins.astyle.pointeralign.name", "Pointer Alignment");
        f.set("plugins.astyle.pointeralign.type", "dropdown");
        f.set("plugins.astyle.pointeralign.default", "name");
        f.set("plugins.astyle.pointeralign.options.type", "Next to type");
        f.set("plugins.astyle.pointeralign.options.middle", "Equidistant");
        f.set("plugins.astyle.pointeralign.options.name", "Next to name");

        return f;
    }

    public void addPanelsToTabs(JTabbedPane pane,int flags) {}

    public void populateMenu(JPopupMenu menu, int flags) {
        if (flags == (Plugin.MENU_POPUP_EDITOR | Plugin.MENU_BOTTOM)) {
            JMenuItem item = new JMenuItem("Reformat with Artistic Style");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int tn = editor.getActiveTab();
                    if (tn == -1) {
                        return;
                    }
                    editorTab = editor.getTab(tn);
                    run();
                }
            });
            menu.add(item);
        }
    }
}
