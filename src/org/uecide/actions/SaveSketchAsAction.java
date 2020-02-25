package org.uecide.actions;

import org.uecide.Base;
import org.uecide.Context;

import java.io.File;
import java.io.IOException;

public class SaveSketchAsAction extends Action {

    public SaveSketchAsAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "SaveSketchAs <filename>"
        };
    }

    public String getCommand() { return "savesketchas"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        try {
            if (args.length == 1) { // A filename has been provided
                if (args[0] instanceof File) { // It was provided as a file
                    // Save sketch as
                    return ctx.getSketch().saveAs((File)args[0]);
                } else if (args[0] instanceof String) { // It was provided as a simple filename
                    // Save sketch as
                    return ctx.getSketch().saveAs(new File(Base.getSketchbookFolder(), (String)args[0]));
                } else {
                    throw new BadArgumentActionException();
                }
            }

            // No filename - if it's untitled then ask for a filename.

            File f = ctx.gui.askSketchFilename("Save sketch as...", Base.getSketchbookFolder());
            if (f == null) {    
                return false;
            }
            return ctx.getSketch().saveAs(f);
        } catch (IOException ex) {
            throw new ActionException(ex.getMessage());
        }
    }
}
