package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class OpenSketchAction extends Action {

    public OpenSketchAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }

        if (args[0] != null) {
            if (!(args[0] instanceof File)) {
                throw new BadArgumentActionException();
            }
        }
        
        File f = (File)args[0];

        if (f != null) {
            if (!f.exists()) {
                throw new FileNotFoundActionException();
            }
        }

        try {
            Sketch openedSketch = new Sketch(f, ctx);
            ctx.setSketch(openedSketch);
            return true;
        } catch (Exception ex) {
            throw new ActionException(ex.getMessage());
        }
    }
}
