package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.Programmer;

import java.util.TreeSet;
import java.util.TreeMap;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProgrammersMenu extends JMenu implements MenuListener {

    Context ctx;

    public ProgrammersMenu(Context c) {
        super("Programmers (None Selected)");
        ctx = c;
        addMenuListener(this);

        updateProgrammer();
    }

    public void updateProgrammer() {
        Programmer programmer = ctx.getProgrammer();
        if (programmer == null) {
            setText("Programmer (None Selected)");
        } else {
            setText("Programmer (" + programmer.getDescription() + ")");
            try {
                setIcon(new CleverIcon(16, programmer.getIcon()));
            } catch (Exception ex) {
            }
        }
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();
        for (Programmer programmer : Base.programmers.values()) {
            if (programmer.worksWith(ctx.getBoard())) {
                if (!programmer.isHidden()) {
                    ProgrammersMenuItem menu = new ProgrammersMenuItem(ctx, programmer);
                    add(menu);
                }
            }
        }
    }
    
}
