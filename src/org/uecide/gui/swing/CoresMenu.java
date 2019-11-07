package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.Core;

import java.util.TreeSet;
import java.util.TreeMap;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CoresMenu extends JMenu implements MenuListener {

    Context ctx;

    public CoresMenu(Context c) {
        super("Core (None Selected)");
        ctx = c;
        addMenuListener(this);

        updateCore();
    }

    public void updateCore() {
        Core core = ctx.getCore();
        if (core == null) {
            setText("Core (None Selected)");
        } else {
            setText("Core (" + core.getDescription() + ")");
            setIcon(core.getIcon(16));
        }
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();
        for (Core core : Base.cores.values()) {
            if (core.worksWith(ctx.getBoard())) {
                CoresMenuItem menu = new CoresMenuItem(ctx, core);
                add(menu);
            }
        }
    }
    
}
