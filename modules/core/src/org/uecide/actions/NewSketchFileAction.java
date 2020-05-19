package org.uecide.actions;

import org.uecide.UECIDE;
import org.uecide.Context;
import org.uecide.Debug;
import java.io.File;
import java.io.IOException;

public class NewSketchFileAction extends Action {

    public NewSketchFileAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "NewSketchFile <name.ext>"
        };
    }

    public String getCommand() { return "newsketchfile"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }

        String name = null;

        if (args[0] instanceof File) {
            File f = (File)args[0];
            name = f.getName();
        } else if (args[0] instanceof String) {
            name = (String)args[0];
        } else {
            throw new BadArgumentActionException();
        }

        File f = new File(ctx.getSketch().getFolder(), name);
        if (f.exists()) {
            throw new ActionException("File already exists");
        }

        try {
            ctx.getSketch().createNewFile(name);
        } catch (IOException ex) {
            Debug.exception(ex);
            throw new ActionException(ex.getMessage());
        }
        return true;
    }
}
