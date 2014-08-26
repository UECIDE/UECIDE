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
import java.awt.datatransfer.*;



public class CopyForForum extends Plugin {

    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    public CopyForForum(Editor e) { editor = e; }
    public CopyForForum(EditorBase e) { editorTab = e; }

    public void run() {
        int et = editor.getActiveTab();
        if (et == -1) {
            return;
        }
        editorTab = editor.getTab(et);
        StringBuilder out = new StringBuilder();
        out.append("[code]\n");
        String[] data;
        if (editorTab.getSelectedText() == null) {
            data = editorTab.getText().split("\n");
        } else {
            data = editorTab.getSelectedText().split("\n");
        }
        for (String line : data) {
            line = line.replace("\t", "  ");
            out.append(line);
            out.append("\n");
        }
        out.append("[/code]\n");

        Clipboard clipboard = editor.getToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(out.toString()),null);
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_EDIT | Plugin.MENU_MID)) {
            JMenuItem item = new JMenuItem("Copy for forum");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    run();
                }
            });
            menu.add(item);
        }
    }
    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

}
