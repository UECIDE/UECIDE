package org.uecide.gui.swing;

import org.uecide.Context;

import java.io.File;

import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class DeleteFileMenuItem extends JMenuItem implements ActionListener {
    Context ctx;
    File fileToDelete;

    public DeleteFileMenuItem(Context c, File f) {
        super("Delete File");
        ctx = c;
        fileToDelete = f;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        if (fileToDelete.delete()) {
            ctx.triggerEvent("fileDeleted");
        }
    }
}
