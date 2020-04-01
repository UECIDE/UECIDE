package org.uecide.gui.swing;

import org.uecide.Context;

import java.io.File;

import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class BinaryFileConversionMenuItem extends JMenuItem implements ActionListener {
    Context ctx;
    File fileToConvert;

    public BinaryFileConversionMenuItem(Context c, File f) {
        super("File Conversion Options");
        ctx = c;
        fileToConvert = f;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        new BinaryFileConversionOptions(ctx, fileToConvert);
        ctx.triggerEvent("binaryFileConversionChanged", fileToConvert);
    }
}
