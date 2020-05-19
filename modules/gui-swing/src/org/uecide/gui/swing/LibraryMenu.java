package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Library;
import org.uecide.Sketch;
import org.uecide.SketchFile;

import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class LibraryMenu extends JMenuItem implements ActionListener {
    Library library;
    Context ctx;

    public LibraryMenu(Context c, Library l) {
        super(l.getName());
        ctx = c;
        library = l;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        Sketch s = ctx.getSketch();
        SketchFile sf = s.getMainFile();
        ctx.triggerEvent("fileDataRead", sf);
        ctx.triggerEvent("saveCursorLocation", sf);
        String data = sf.getFileData();
        sf.setFileData(library.getInclude() + data);
        ctx.triggerEvent("restoreCursorLocation", sf);
        ctx.triggerEvent("includeAdded", sf);
    }
}
    
