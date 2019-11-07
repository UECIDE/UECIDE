package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.Context;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;

import java.io.File;


public class RecentFileMenu extends JMenu implements MenuListener {

    Context ctx;

    public RecentFileMenu(Context c) {
        super("Recent Sketches");
        ctx = c;
        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        ArrayList<File> MRU = Base.MRUList;

        removeAll();
        for (File f : MRU) {
            if (!f.exists()) continue;
            if (!f.isDirectory()) continue;
            ObjectMenuItem item = new ObjectMenuItem(f.getName(), f);
            item.setToolTipText(f.getAbsolutePath());
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ObjectMenuItem i = (ObjectMenuItem)e.getSource();
                    ctx.action("opensketch", (File)i.getObject());
                }
            });
            add(item);
        }
    }
    
}    
