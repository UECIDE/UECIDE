package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class OpenSketchFileAction extends Action {

    public OpenSketchFileAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "OpenSketchFile <filename> [line]"
        };
    }

    public String getCommand() { return "opensketchfile"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length == 0) {
            throw new SyntaxErrorActionException();
        }

        if (args.length > 2) {
            throw new SyntaxErrorActionException();
        }

        if (args[0] instanceof SketchFile) {
            SketchFile f = (SketchFile)args[0];
            ctx.gui.openSketchFileEditor(f);

            if ((args.length == 2) && (args[1] != null)) {
                if (args[1] instanceof Integer) {
                    Integer lineno = (Integer)args[1];
                    ctx.gui.navigateToLine(f, lineno);
                }
            }

            return true;
        }

        if (args[0] instanceof String) {
            String s = (String)args[0];
            SketchFile f = ctx.getSketch().getFileByName(s);
            if (f == null) {
System.err.println("This bit");
                throw new ActionException("Sketch file not found: " + s);
            }
            ctx.gui.openSketchFileEditor(f);

            if ((args.length == 2) && (args[1] != null)) {
                if (args[1] instanceof Integer) {
                    Integer lineno = (Integer)args[1];
                    ctx.gui.navigateToLine(f, lineno);
                }
            }

            return true;
        }

        throw new BadArgumentActionException();
    }
}
