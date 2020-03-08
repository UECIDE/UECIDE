package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Sketch;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.TreeMap;

public class OptionsMenu extends JMenu implements MenuListener, ActionListener {
    Context ctx;

    public OptionsMenu(Context context) {
        super("Options");
        ctx = context;
        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();

        TreeMap<String, String> opts = ctx.getSketch().getOptionGroups();

        for (String opt : opts.keySet()) {
            JMenu option = new OptionSelectMenu(ctx, opt);
            add(option);
        }

        addSeparator();

        JMenuItem insert = new JMenuItem("Insert settings into sketch");

        insert.addActionListener(this);

        add(insert);
    }

    public void actionPerformed(ActionEvent evt) {
        ctx.action("UpdateSketchOptions");
    }
}
