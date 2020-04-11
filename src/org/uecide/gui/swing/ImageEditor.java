package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.Debug;

import java.io.File;

import uk.co.majenko.bmpedit.BMPEdit;

public class ImageEditor extends TabPanel implements ContextEventListener {
    Context ctx;
    File file;
    BMPEdit editor;
    
    public ImageEditor(Context ctx, AutoTab parent, File file) {
        super(file.getName(), parent);
        this.ctx = ctx;
        this.file = file;
        editor = new BMPEdit();
        try {
            editor.loadImage(file);
        } catch (Exception ex) {
            Debug.exception(ex);
        }
        add(editor);
        this.ctx.listenForEvent("saveSketch", this);
    }

    public void contextEventTriggered(ContextEvent evt) {
        if (evt.getEvent().equals("saveSketch")) {
            try {
                editor.saveImage(file);
            } catch (Exception ex) {
                Debug.exception(ex);
            }
        }
    }
}
