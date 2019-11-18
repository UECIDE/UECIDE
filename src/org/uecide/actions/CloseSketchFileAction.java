package org.uecide.actions;

import org.uecide.*;
import java.io.File;
import java.io.IOException;

public class CloseSketchFileAction extends Action {

    public CloseSketchFileAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }

        if (args[0] instanceof SketchFile) {
            SketchFile f = (SketchFile)args[0];
            if (f.isModified()) {
                int v = ctx.getGui().askYesNoCancel(f.getFile().getName() + " has been modified. Save before closing?");
                if (v == 0) { // Yes
                    try {
                        f.saveDataToDisk();
                    } catch (IOException exc) {
                        throw new ActionException(exc.getMessage());
                    }
                } else if (v == 2) { // Cancel
                    return false;
                }
            }
            ctx.getGui().closeSketchFileEditor(f);
            return true;
        }

        if (args[0] instanceof String) {
            String s = (String)args[0];
            SketchFile f = ctx.getSketch().getFileByName(s);
            if (f == null) {
                throw new ActionException("Sketch file not found: " + s);
            }
            if (f.isModified()) {
                int v = ctx.getGui().askYesNoCancel(f.getFile().getName() + " has been modified. Save before closing?");
                if (v == 0) { // Yes
                    try {
                        f.saveDataToDisk();
                    } catch (IOException exc) {
                        throw new ActionException(exc.getMessage());
                    }
                } else if (v == 2) { // Cancel
                    return false;
                }
            }
            ctx.getGui().closeSketchFileEditor(f);
            return true;
        }

        throw new BadArgumentActionException();
    }
}
