package org.uecide.gui.swing;

import org.uecide.Context;

import org.uecide.gui.Gui;

import java.io.File;

import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class RenameFileMenuItem extends JMenuItem implements ActionListener {
    Context ctx;
    File fileToRename;

    public RenameFileMenuItem(Context c, File f) {
        super("Rename File");
        ctx = c;
        fileToRename = f;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        Gui gui = ctx.getGui();
        String newname = gui.askString("Enter new filename", fileToRename.getName());
        if (newname == null) return;
        if (newname.equals(fileToRename.getName())) return;

        File newfile = new File(fileToRename.getParentFile(), newname);
        if (newfile.exists()) {
            ctx.error("Error: Destination file already exists");
            return;
        }
        if (fileToRename.renameTo(newfile)) {
            ctx.triggerEvent("fileRenamed");
        }
    }
}
