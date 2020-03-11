package org.uecide.gui.swing;

import org.uecide.UECIDE;
import org.uecide.Context;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.HashMap;

import java.io.File;


public class FrequentFileMenu extends JMenu implements MenuListener {

    Context ctx;

    public FrequentFileMenu(Context c) {
        super("Frequent Sketches");
        ctx = c;
        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        HashMap<File,Integer> MCU = UECIDE.MCUList;

        removeAll();
        for (File f : MCU.keySet()) {
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
