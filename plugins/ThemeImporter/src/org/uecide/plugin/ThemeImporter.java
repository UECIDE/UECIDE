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
import javax.swing.filechooser.*;
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


public class ThemeImporter extends Plugin {

    static final int BLOCK_MAXLEN = 1024;

    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }
    public ThemeImporter(Editor e) { editor = e; }
    public ThemeImporter(EditorBase e) { editorTab = e; }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_MID | Plugin.MENU_TOOLS)) {
            
            JMenu sub = new JMenu("Import Editor Theme");

            JMenuItem item = new JMenuItem("SublimeText / TextMate");
            
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    askImportSublimeTheme();
                }
            });

            sub.add(item);
            menu.add(sub);
        }
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public static void populatePreferences(JPanel p) {
    }

    public static String getPreferencesTitle() {
        return "Theme Importer";
    }

    public static void savePreferences() {
    }

    public ImageIcon getFileIconOverlay(File f) {
        return null;
    }

    public void setSetting(PropertyFile dest, String to, String from, PropertyFile alt) {
        PropertyFile p = findPListSlice(alt, from);
        if (p != null) {
            if (p.get("foreground") != null) {
                dest.setColor(to + ".fgcolor", p.getColor("foreground"));
            } else {
                dest.setColor(to + ".fgcolor", alt.getColor("0.settings.settings.foreground"));
            }
        }
    }

    public void askImportSublimeTheme() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select SublimeText Theme");
        fc.setApproveButtonText("Import");
        fc.setFileFilter(new FileNameExtensionFilter("SublimeText / TextMate Themes", "tmTheme"));
        int rc = fc.showOpenDialog(editor);
        if (rc == JFileChooser.APPROVE_OPTION) {
            String name = fc.getSelectedFile().getName();
            name = name.substring(0, name.lastIndexOf("."));
            PropertyFile th = loadPList(fc.getSelectedFile());
            File tf = new File(Base.getUserThemesFolder(), name+".theme");
            PropertyFile theme = new PropertyFile(tf);

            theme.set("name",                               th.get("name"));
            theme.setColor("editor.fgcolor",                th.getColor("0.settings.settings.foreground"));
            theme.setColor("editor.bgcolor",                th.getColor("0.settings.settings.background"));

            theme.setColor("editor.gutter.bgcolor",         th.getColor("0.settings.settings.background"));
            theme.setColor("editor.gutter.fgcolor",         th.getColor("0.settings.settings.foreground"));
            theme.setColor("editor.gutter.bordercolor",     th.getColor("0.settings.settings.foreground"));

            theme.setColor("editor.line.bgcolor",           th.getColor("0.settings.settings.lineHighlight"));

            theme.setColor("editor.fold.fgcolor",           th.getColor("0.settings.settings.foreground"));
            theme.setColor("editor.fold.bgcolor",           th.getColor("0.settings.settings.background"));

            theme.setColor("editor.caret.fgcolor",          th.getColor("0.settings.settings.caret"));

            theme.setColor("editor.markall.bgcolor",        th.getColor("0.settings.settings.findHighlight"));
            theme.setColor("editor.markall.fgcolor",        th.getColor("0.settings.settings.findHighlightForeground"));
            theme.setColor("editor.searchall.bgcolor",      th.getColor("0.settings.settings.findHighlight"));
            theme.setColor("editor.searchall.fgcolor",      th.getColor("0.settings.settings.findHighlightForeground"));

            theme.setColor("editor.brackets.fgcolor",       th.getColor("0.settings.settings.bracketsForeground"));

    
            theme.setColor("editor.select.bgcolor",         th.getColor("0.settings.settings.activeGuide"));

            setSetting(theme, "editor.error", "invalid", th);
            setSetting(theme, "editor.comment", "comment", th);
            setSetting(theme, "editor.literal", "string", th);
            setSetting(theme, "editor.literal.decimal", "number", th);
            setSetting(theme, "editor.literal.float", "number", th);
            setSetting(theme, "editor.literal.hex", "number", th);
            setSetting(theme, "editor.datatype", "storage.type", th);
            setSetting(theme, "editor.function", "entity.name.function", th);
            setSetting(theme, "editor.variable", "variable", th);
            setSetting(theme, "editor.reserved", "keyword", th);
            setSetting(theme, "editor.preprocessor", "support.function", th);
            setSetting(theme, "editor.identifier", "constant.language", th);
            setSetting(theme, "editor.operator", "entity.other.inherited-class", th);

            theme.set("console.color",                      "#000000");
            theme.set("console.output.color",               "#00cc00");
            theme.set("console.warning.color",              "#cccc00");
            theme.set("console.error.color",                "#ff0000");
            theme.set("editor.compile.error.bgcolor",       "#880000");
            theme.set("editor.compile.warning.bgcolor",     "#888800");

            theme.set("editor.caret.style.insert",          "line");
            theme.set("editor.caret.style.replace",         "block");
            theme.setBoolean("editor.bracket.pair",         true);
            theme.setBoolean("editor.line.enabled",         true);
            theme.setBoolean("editor.line.fade",            true);
            theme.setBoolean("editor.markall.border",       false);
            theme.setBoolean("editor.select.rounded",       false);

            theme.save();
            Base.rescanThemes();
            if (JOptionPane.showConfirmDialog(editor, "Theme installed. Would you like to\nactivate it now?", "Theme Installed", JOptionPane.YES_NO_OPTION) == 0) {
                Base.preferences.set("theme.selected", name);
                Base.preferences.save();
                Editor.refreshAllEditors();
            }

        }
    }

    public PropertyFile loadPList(File f) {
        PropertyFile theme = new PropertyFile();

        String content = loadFileToString(f);
        String[] lines = content.split("\n");

        Stack<String> keyStack = new Stack<String>();
        Stack<Boolean> arrayStack = new Stack<Boolean>();
        Stack<Integer> sliceStack = new Stack<Integer>();

        String currentKey = "";
        String thisKey = "";
        String thisValue = "";

        Pattern kPat = Pattern.compile("<key>(.*)</key>");
        Pattern sPat = Pattern.compile("<string>(.*)</string>");

        boolean inArray = false;
        int arraySlice = 0;

        for (String line : lines) {
            line = line.trim();

            if (line.equals("<dict>")) {
                arrayStack.push(inArray);
                sliceStack.push(arraySlice);
                keyStack.push(currentKey);

                if (inArray) {
                    currentKey = currentKey + "." + arraySlice + "." + thisKey;
                } else {
                    currentKey = currentKey + "." + thisKey;
                }
                inArray = false;
                arraySlice = 0;
                continue;
            }

            if (line.equals("</dict>")) {
                inArray = arrayStack.pop();
                arraySlice = sliceStack.pop();
                currentKey = keyStack.pop();

                thisKey = "";
                if (inArray) {
                    arraySlice++;
                }
                continue;
            }

            if  (line.equals("<array>")) {
                inArray = true;
                arraySlice = 0;
                continue;
            }

            if  (line.equals("</array>")) {
                inArray = false;
                continue;
            }

            Matcher m = kPat.matcher(line);
            if (m.find()) {
                thisKey = m.group(1);
                continue;
            }

            m = sPat.matcher(line);
            if (m.find()) {
                thisValue = m.group(1);
                String fullKey;
                if (inArray) {
                    fullKey = currentKey + "." + arraySlice + "." + thisKey;
                } else {
                    fullKey = currentKey + "." + thisKey;
                } 
                fullKey = fullKey.replaceAll("\\.\\.", ".");
                while (fullKey.startsWith(".")) {
                    fullKey = fullKey.substring(1);
                }
                theme.set(fullKey, thisValue);
            }
        }

        return theme;
    }

    public String loadFileToString(File f) {
        InputStream    fis;
        BufferedReader br;
        String         line;
        StringBuilder sb = new StringBuilder();

        try {

            fis = new FileInputStream(f);
            br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

            while((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }

            br.close();
        } catch(Exception e) {
            editor.error(e);
        }

        return sb.toString();
    }

    public PropertyFile findPListSlice(PropertyFile in, String scope) {

        int i = 0;
        while (in.keyExists(Integer.toString(i))) {
            String sc = in.get(i + ".scope");
            if (sc != null) {
                String[] sb = sc.split(",");
                for (String scopebit : sb) {
                    scopebit = scopebit.trim();
                    if (scopebit.equals(scope)) {
                        PropertyFile out = in.getChildren(i + ".settings");
                        return out;
                    }
                }
            }
            i++;
        }

        return null;

    }

}
