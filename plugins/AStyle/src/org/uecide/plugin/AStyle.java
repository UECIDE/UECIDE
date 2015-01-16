package org.uecide.plugin;

import org.uecide.*;
import org.uecide.debug.*;
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
            String text = editorTab.getText();
            String baseStyle = Base.preferences.get("astyle.style", "gnu");

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

            if (Base.preferences.getBoolean("astyle.pad.blocks")) command.add("--break-blocks");
            if (Base.preferences.getBoolean("astyle.pad.operators")) command.add("--pad-oper");
            if (Base.preferences.getBoolean("astyle.pad.header")) command.add("--pad-header");
            if (Base.preferences.getBoolean("astyle.add.brackets")) command.add("--add-brackets");
            if (Base.preferences.getBoolean("astyle.add.onelinebrackets")) command.add("--add-one-line-brackets");

            if (Base.preferences.getBoolean("editor.expandtabs")) {
                command.add("--indent=spaces=" + Base.preferences.getInteger("editor.tabsize"));
            } else {
                command.add("--indent=tab");
            }

            if (Base.preferences.getBoolean("astyle.pad.parenthesis")) {
                command.add("--pad-paren");
            } else {
                command.add("--unpad-paren");
            }
            command.add("--align-pointer=" + Base.preferences.get("astyle.align.pointer", "name"));

            command.add(temp.getAbsolutePath());

            ProcessBuilder proc = new ProcessBuilder(command);
            Process p = proc.start();
            p.waitFor();

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

            Version iconTest = new Version("0.8.7z30");
            if (Base.systemVersion.compareTo(iconTest) > 0) {
                editor.addToolbarButton(toolbar, "apps", "astyle", "Auto Format with Artistic Style", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int tn = editor.getActiveTab();
                        if (tn == -1) {
                            return;
                        }
                        editorTab = editor.getTab(tn);
                        run();
                    }
                });
            } else {
                JButton b = new JButton(Base.loadIconFromResource("/org/uecide/plugin/AStyle/astyle22.png"));
                b.setToolTipText("Auto Format with Artistic Style");
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int tn = editor.getActiveTab();
                        if (tn == -1) {
                            return;
                        }
                        editorTab = editor.getTab(tn);
                        run();
                    }
                });
                toolbar.add(b);
            }
        }
    }

    public static JComboBox astyleStyle;

    public static String[] styles = {
        "Allman / BSD / ANSI", "Java", "Kernighan  &  Ritchie",
        "Stroustrup", "Whitesmith", "Banner", "GNU", "Linux", 
        "Horstmann", "One True Brace", "Pico", "Lisp / Python"
    };

    public static String[] codes = {
        "allman", "java", "kr", "stroustrup", "whitesmith", "banner",
        "gnu", "linux", "horstmann", "1tbs", "pico", "lisp"
    };

    public static String[] descs = {
        "Allman style formatting/indenting uses broken brackets.",

        "Java style formatting/indenting uses attached brackets.",

        "Kernighan & Ritchie style formatting/indenting uses linux brackets. Brackets are broken from namespaces, classes, and function definitions. Brackets are attached to statements within a function.",

        "Stroustrup style formatting/indenting uses stroustrup brackets. Brackets are broken from function definitions only. Brackets are attached to namespaces, classes, and statements within a function. This style frequently is used with an indent of 5 spaces.",

        "Whitesmith style formatting/indenting uses broken, indented brackets. Class blocks and switch blocks are indented to prevent a 'hanging indent' with switch statements and C++ class modifiers (public, private, protected).",

        "Banner style formatting/indenting uses attached, indented brackets. Class blocks and switch blocks are indented to prevent a 'hanging indent' with switch statements and C++ class modifiers (public, private, protected).",

        "GNU style formatting/indenting uses broken brackets and indented blocks. This style frequently is used with an indent of 2 spaces. Extra indentation is added to blocks within a function. The opening bracket for namespaces, classes, and functions is not indented.",

        "Linux style formatting/indenting uses linux style brackets. Brackets are broken from namespace, class, and function definitions. Brackets are attached to statements within a function. Minimum conditional indent is one-half indent. If you want a different minimum conditional indent use the K&R style instead. This style works best with a large indent. It frequently is used with an indent of 8 spaces. Also known as Kernel Normal Form (KNF) style, this is the style used in the Linux kernel.",

        "Horstmann style formatting/indenting uses run-in brackets, brackets are broken and allow run-in statements. Switches are indented to allow a run-in to the opening switch block. This style frequently is used with an indent of 3 spaces.",

        "'One True Brace Style' formatting / indenting uses linux brackets and adds brackets to unbracketed one line conditional statements.",

        "Pico style formatting/indenting uses run-in brackets, opening brackets are broken and allow run-in statements. The closing bracket is attached to the last line in the block. Switches are indented to allow a run-in to the opening switch block. This style frequently is used with an indent of 2 spaces.",

        "Lisp style formatting / indenting uses attached brackets, opening brackets are attached at the end of the statement. The closing bracket is attached to the last line in the block."
    };

    static String[] alignOptions = { "type", "middle", "name" };

    /* Formatting options */

    static JCheckBox padBlocks;
    static JCheckBox padOper;
    static JCheckBox padParen;
    static JCheckBox padHeader;
    static JCheckBox addBrackets;
    static JCheckBox addOneLineBrackets;
    static JComboBox alignPointer;

    public static void populatePreferences(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        c.gridwidth = 2;
        JTextArea info = new JTextArea();
        info.setText("This plugin uses Artistic Style by Jim Pattee and Tal Davidson. http://astyle.sourceforge.net\n");
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setOpaque(false);
        info.setEditable(false);
        p.add(info, c);
        c.gridwidth = 1;

        JLabel label = new JLabel("Formatting style: ");
        c.gridx = 0;
        c.gridy++;
        p.add(label, c);

        astyleStyle = new JComboBox(styles);
        String sel = Base.preferences.get("astyle.style");
        if (sel == null) {
            sel = "gnu";
        }
        int idx = Arrays.asList(codes).indexOf(sel);
        if (idx == -1) {
            idx = 6;
        }
        astyleStyle.setSelectedIndex(idx);

        c.gridx = 1;
        p.add(astyleStyle, c);

        final JTextArea desc = new JTextArea();
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        p.add(desc, c);
        desc.setOpaque(false);

        String description = descs[idx];
        desc.setText("\n" + description + "\n");

        astyleStyle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = astyleStyle.getSelectedIndex();
                if (idx > -1) {
                    String description = descs[idx];
                    desc.setText("\n" + description + "\n");
                }
            }
        });

        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;

        padBlocks = new JCheckBox("Pad blocks");
        padBlocks.setSelected(Base.preferences.getBoolean("astyle.pad.blocks"));
        c.gridx = 0;
        p.add(padBlocks, c);

        padOper = new JCheckBox("Pad Operators");
        padOper.setSelected(Base.preferences.getBoolean("astyle.pad.operators"));
        c.gridx = 1;
        p.add(padOper, c);

        c.gridy++;

        padParen = new JCheckBox("Pad Parenthesis");
        padParen.setSelected(Base.preferences.getBoolean("astyle.pad.parenthesis"));
        c.gridx = 0;
        p.add(padParen, c);

        padHeader = new JCheckBox("Pad Header");
        padHeader.setSelected(Base.preferences.getBoolean("astyle.pad.header"));
        c.gridx = 1;
        p.add(padHeader, c);

        c.gridy++;


        addBrackets = new JCheckBox("Add Brackets");
        addBrackets.setSelected(Base.preferences.getBoolean("astyle.add.brackets"));
        c.gridx = 0;
        p.add(addBrackets, c);

        addOneLineBrackets = new JCheckBox("Add One-Line Brackets");
        addOneLineBrackets.setSelected(Base.preferences.getBoolean("astyle.add.onelinebrackets"));
        c.gridx = 1;
        p.add(addOneLineBrackets, c);

        c.gridy++;




        label = new JLabel("Align pointer to:");
        c.gridx = 0;
        c.gridwidth = 1;
        p.add(label, c);

        alignPointer = new JComboBox(alignOptions);
        String pal = Base.preferences.get("astyle.align.pointer", "name");
        idx = Arrays.asList(alignOptions).indexOf(pal);
        alignPointer.setSelectedIndex(idx);
        c.gridx = 1;
        p.add(alignPointer, c);
    }

    public static String getPreferencesTitle() {
        return "Artistic Style";
    }

    public static void savePreferences() {
        int idx = astyleStyle.getSelectedIndex();

        if (idx == -1) return;
        if (idx >= codes.length) return;
        String code = codes[idx];
        
        Base.preferences.set("astyle.style", code);

        Base.preferences.setBoolean("astyle.pad.blocks", padBlocks.isSelected());
        Base.preferences.setBoolean("astyle.pad.operators", padOper.isSelected());
        Base.preferences.setBoolean("astyle.pad.parenthesis", padParen.isSelected());
        Base.preferences.setBoolean("astyle.pad.header", padHeader.isSelected());
        Base.preferences.setBoolean("astyle.add.brackets", addBrackets.isSelected());
        Base.preferences.setBoolean("astyle.add.onelinebrackets", addOneLineBrackets.isSelected());
        Base.preferences.set("astyle.align.pointer", alignOptions[alignPointer.getSelectedIndex()]);
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
}
