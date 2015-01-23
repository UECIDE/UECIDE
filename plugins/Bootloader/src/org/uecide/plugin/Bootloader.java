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



public class Bootloader extends Plugin {

    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    public Bootloader(Editor e) { editor = e; }
    public Bootloader(EditorBase e) { editorTab = e; }

    public void run() {
        Sketch sketch = editor.getSketch();

        Board board = sketch.getBoard();
        Core core = sketch.getCore();
        String programmer = sketch.getProgrammer();

        PropertyFile props = sketch.mergeAllProperties();
        String blProgs = props.get("bootloader.upload");

        System.err.println(blProgs);

    }

    public void doProgram(String prog) {
        editor.clearConsole();
        Sketch sketch = editor.getSketch();
        PropertyFile props = sketch.mergeAllProperties();
        editor.message("Programming bootloader using " + prog);
        String bl = sketch.parseString(props.get("bootloader.file"));
        editor.message("Bootloader: " + bl);
        if (bl == null) {
            String url = sketch.parseString(props.get("bootloader.url"));
            if (url != null) {
                editor.error("The bootloader can be downloaded from " + url);
                return;
            }
        }

        sketch.programFile(prog, bl);
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_TOOLS | Plugin.MENU_MID)) {
            Sketch sketch = editor.getSketch();
            JMenuItem item = new JMenu("Program Bootloader");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    run();
                }
            });
            menu.add(item);

            PropertyFile props = sketch.mergeAllProperties();
            String blProgs = props.get("bootloader.upload");
            if (blProgs == null) {
                JMenuItem sub = new JMenuItem("No bootloader programmer defined!");
                item.add(sub);
                return;
            }
            String[] progs = blProgs.split("::");
            for (String prog : progs) {
                JMenuItem sub = new JMenuItem(sketch.parseString(props.get("upload." + prog + ".name")));
                sub.setActionCommand(prog);
                sub.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        doProgram(e.getActionCommand());
                    }
                });
                item.add(sub);
            }
        }
    }
    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

}
