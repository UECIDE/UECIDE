package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;

import java.util.ArrayList;

import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class CreateFileMenu extends JMenuItem implements ActionListener {

    Context ctx;
    String extension;

    public CreateFileMenu(Context c, String n, String e) {
        super(n);
        ctx = c;
        extension = e;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        String name = ctx.getGui().askString("Enter filename (without extension)", "");

        if (name != null) {
            System.err.println("NewSketchFile(" +  name + "." + extension + ")");
            ctx.action("NewSketchFile", name + "." + extension);
        }
    }
}
