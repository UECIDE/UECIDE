package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class NewSketchAction extends Action {

    public NewSketchAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 0) {
            throw new BadArgumentActionException();
        }
        
        try {
            Sketch newSketch = new Sketch((File)null, ctx);
            ctx.setSketch(newSketch);
            return true;
        } catch (Exception ex) {
            throw new ActionException(ex.getMessage());
        }
    }
}
