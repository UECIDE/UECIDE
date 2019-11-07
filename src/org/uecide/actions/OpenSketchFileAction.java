package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class OpenSketchFileAction extends Action {

    public OpenSketchFileAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }

        if (args[0] instanceof SketchFile) {
            SketchFile f = (SketchFile)args[0];
            ctx.gui.openSketchFileEditor(f);
            return true;
        }

        if (args[0] instanceof String) {
            String s = (String)args[0];
            SketchFile f = ctx.getSketch().getFileByName(s);
            if (f == null) {
                throw new ActionException("Sketch file not found: " + s);
            }
            ctx.gui.openSketchFileEditor(f);
            return true;
        }

        throw new BadArgumentActionException();
    }
}
