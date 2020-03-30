package org.uecide.gui.swing;

import org.uecide.UECIDE;
import org.uecide.Context;
import org.uecide.Compiler;
import org.uecide.Debug;

import java.util.TreeSet;
import java.util.TreeMap;

import java.io.File;

import javax.swing.JRadioButtonMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CompilersMenuItem extends JRadioButtonMenuItem implements ActionListener {

    Context ctx;
    Compiler compiler;

    public CompilersMenuItem(Context c, Compiler b) {
        super(b.getDescription());
        ctx = c;
        compiler = b;
        if (compiler == ctx.getCompiler()) {
            setSelected(true);
        } else {
            setSelected(false);
        }
        try {
            setIcon(new CleverIcon(16, compiler.getIcon()));
        } catch (Exception ex) {
            Debug.exception(ex);
        }
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        ctx.action("setCompiler", compiler);
    }
}
