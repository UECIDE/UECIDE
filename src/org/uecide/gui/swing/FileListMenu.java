package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;
import org.uecide.SketchFile;

import java.util.TreeMap;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FileListMenu extends JMenu implements MenuListener, ActionListener {

    Context ctx;
    int type;

    public FileListMenu(Context c, String n, int t) {
        super(n);
        ctx = c;
        type = t;
        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();

        TreeMap<String, SketchFile> sketchFiles = ctx.getSketch().getSketchFiles();

        for (SketchFile f : sketchFiles.values()) {
            if (f.getGroup() == type) {
                ObjectMenuItem item = new ObjectMenuItem(f.toString(), f);
                item.addActionListener(this);
                add(item);
            }
        }
    }

    public void actionPerformed(ActionEvent evt) {
        ObjectMenuItem item = (ObjectMenuItem)evt.getSource();
        ctx.action("openSketchFile", (SketchFile)item.getObject());
    }
    
}
