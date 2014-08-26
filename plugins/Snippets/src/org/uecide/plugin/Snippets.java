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


public class Snippets extends Plugin {

    public JMenu snippetMenu = null;

    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }
    public Snippets(Editor e) { editor = e; populateSnippetMenu(); }
    public Snippets(EditorBase e) { editorTab = e; }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_EDIT | Plugin.MENU_BOTTOM)) {
            if (snippetMenu == null) {
                populateSnippetMenu();
            }
            menu.add(snippetMenu);
        }
    }

    public void populateSnippetMenu() {
        if (snippetMenu == null) {
            snippetMenu = new JMenu("Snippets");
        } else {
            snippetMenu.removeAll();
        }
        JMenuItem item;

        item = new JMenuItem("Add snippet");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                createNewSnippet();
            }
        });
        snippetMenu.add(item);
        snippetMenu.addSeparator();

        PropertyFile snippets = Base.preferences.getChildren("snippets");
        for (String k : snippets.childKeys()) {
            String name = snippets.get(k + ".name");
            item = new JMenuItem(name);
            item.setActionCommand(k);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    insertSnippet(ev.getActionCommand());
                }
            });
            snippetMenu.add(item);
        }
    }

    public void createNewSnippet() {
        int tn = editor.getActiveTab();
        if (tn == -1) {
            return;
        }
        editorTab = editor.getTab(tn);
        String text = editorTab.getSelectedText();
        if (text == null || text.equals("")) {
            return;
        }

       String name = (String)JOptionPane.showInputDialog(
                    editor,
                    "Please name this snippet:",
                    "Create Snippet",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "My Snippet"); 

        if (name == null || name.equals("")) {
            return;
        }

        String key = name;
        key = key.replaceAll(" ", "");
        key = key.replaceAll("\\.", "");
        key = key.replaceAll("-", "");
        key = key.replaceAll("#", "");
        key = key.replaceAll("=", "");
        key = key.toLowerCase();

        Base.preferences.set("snippets." + key + ".name", name);
        Base.preferences.set("snippets." + key + ".text", text);
        Base.preferences.saveDelay();
        populateSnippetMenu();
    }

    public void insertSnippet(String key) {
        int tn = editor.getActiveTab();
        if (tn == -1) {
            return;
        }
        editorTab = editor.getTab(tn);

        String text = Base.preferences.get("snippets." + key + ".text");
        if (text == null || text.equals("")) {
            return;
        }

        editorTab.insertAtCursor(text);

    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public static void populatePreferences(JPanel p) {
    }

    public static String getPreferencesTitle() {
        return null;
    }

    public static void savePreferences() {
    }

    public ImageIcon getFileIconOverlay(File f) {
        return null;
    }
}
