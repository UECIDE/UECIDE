package org.uecide.gui.swing;

import org.uecide.Context;

import javax.swing.JRadioButtonMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.TreeMap;

public class OptionEntryMenu extends JRadioButtonMenuItem implements ActionListener {

    Context ctx;
    String set;
    String opt;

    public OptionEntryMenu(Context context, String optionSet, String option) {
        super(context.getSketch().getOptionNames(optionSet).get(option));
        ctx = context;
        set = optionSet;
        opt = option;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        setSelected(true);
        ctx.action("SetOption", set, opt);
    }
}


