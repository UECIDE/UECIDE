package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class OpenSketchAction extends Action {

    public OpenSketchAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {

        File file = null;

        if (args.length == 0) { 
            file = ctx.getGui().askOpenSketch("Open Sketch", Base.getSketchbookFolder());
            if (file == null) {
                System.err.println("It cancelled?");
                return false;
            }
        }

        if (args.length == 1) {
            if (args[0] == null) {
                file = null;
            } else if (args[0] instanceof File) {
                file = (File)args[0];
            } else if (args[0] instanceof String) {
                file = new File((String)args[0]);
            } else {
                throw new BadArgumentActionException();
            }
        }

        if (args.length > 1) {
            throw new SyntaxErrorActionException();
        }

        if ((file != null) && !(file.exists())) {
            throw new FileNotFoundActionException();
        }
            
        try {

            // If this is a new context with no sketch (called from base's createContext)
            // then make a sketch and assign it to the context.
            if (ctx.getSketch() == null)  {
                Sketch s = new Sketch(file, ctx);
                ctx.setSketch(s);
                return true;
            }

            // Make a new context - this action then gets called again with the first "if" above.
            Base.createContext(file);

            // If we have an unmodified blank sketch (freshly opened), then just close it.
            // Note: this has to happen after the new context, or there's a good chance
            // UECIDE will quit as the last session closed.
            if (ctx.getSketch().isUntitled() && !(ctx.getSketch().isModified())) {
                ctx.action("closeSession");
            }

            return true;
        } catch (Exception ex) {
            throw new ActionException(ex.getMessage());
        }
    }
}
