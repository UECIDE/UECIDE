package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.Compiler;

import java.util.TreeSet;
import java.util.TreeMap;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CompilersMenu extends JMenu implements MenuListener {

    Context ctx;

    public CompilersMenu(Context c) {
        super("Compiler (None Selected)");
        ctx = c;
        addMenuListener(this);

        updateCompiler();
    }

    public void updateCompiler() {
        Compiler compiler = ctx.getCompiler();
        if (compiler == null) {
            setText("Compiler (None Selected)");
        } else {
            setText("Compiler (" + compiler.getDescription() + ")");
            setIcon(compiler.getIcon(16));
        }
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();
        for (Compiler compiler : Base.compilers.values()) {
            if (compiler.worksWith(ctx.getBoard())) {
                CompilersMenuItem menu = new CompilersMenuItem(ctx, compiler);
                add(menu);
            }
        }
    }
    
}
