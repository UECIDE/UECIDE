package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.Core;

import java.util.TreeSet;
import java.util.TreeMap;

import java.io.File;

import javax.swing.JRadioButtonMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CoresMenuItem extends JRadioButtonMenuItem implements ActionListener {

    Context ctx;
    Core core;

    public CoresMenuItem(Context c, Core b) {
        super(b.getDescription());
        ctx = c;
        core = b;
        if (core == ctx.getCore()) {
            setSelected(true);
        } else {
            setSelected(false);
        }
        try {
            setIcon(new CleverIcon(16, core.getIcon()));
        } catch (Exception ex) {
        }
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        ctx.action("setCore", core);
    }
}
