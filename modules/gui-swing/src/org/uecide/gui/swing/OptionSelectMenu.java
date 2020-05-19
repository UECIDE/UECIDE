package org.uecide.gui.swing;

import org.uecide.Context;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import javax.swing.JMenu;
import javax.swing.ButtonGroup;

import java.util.TreeMap;

public class OptionSelectMenu extends JMenu implements MenuListener {
    Context ctx;
    String opt;

    public OptionSelectMenu(Context context, String option) {
        super(context.getSketch().getOptionGroups().get(option));
        ctx = context;
        opt = option;
        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();
        ButtonGroup group = new ButtonGroup();
        TreeMap<String, String> optvals = ctx.getSketch().getOptionNames(opt);
        for (String key : optvals.keySet()) {
            OptionEntryMenu menu = new OptionEntryMenu(ctx, opt, key);
            group.add(menu);
            menu.setSelected(ctx.getSketch().getOption(opt).equals(key));
            add(menu);
        }
    }
}
