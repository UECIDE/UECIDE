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
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;

import jssc.*;

import say.swing.*;


public class UploadLastHex extends Plugin 
{
    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    public UploadLastHex(Editor e) { editor = e; }
    public UploadLastHex(EditorBase e) { editorTab = e; }

    Thread uploadThread = null;

    public void run() {
        uploadThread = new Thread(new Runnable() {
            public void run() {
                editor.getSketch().upload();
            }
        });
        uploadThread.start();
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
        if (flags == Plugin.TOOLBAR_EDITOR) {
            try {
                toolbar.add(new ToolbarButton("apps.reuploadhex", "Re-upload last HEX file", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
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

    public static void savePreferences() {
    }

    public void releasePort(String portName) {
    }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_SKETCH | Plugin.MENU_MID)) {
            JMenuItem item = new JMenuItem("Re-Upload Last Compiled");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    run();
                }
            });
            menu.add(item);
        }
    }

    public static String getPreferencesTitle() {
        return "Re-upload HEX";
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

    public void addPanelsToTabs(JTabbedPane pane,int flags) { }

    public void populateMenu(JPopupMenu menu, int flags) { }

}

