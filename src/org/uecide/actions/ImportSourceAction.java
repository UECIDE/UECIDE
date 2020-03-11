package org.uecide.actions;

import org.uecide.UECIDE;
import org.uecide.Context;
import org.uecide.Utils;
import java.io.File;
import java.io.IOException;

public class ImportSourceAction extends Action {

    public ImportSourceAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "ImportSource <file>"
        };
    }

    public String getCommand() { return "importsource"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }

        File source = null;

        if (args[0] instanceof File) {
            source = (File)args[0];
        } else if (args[0] instanceof String) {
            source = new File((String)args[0]);
        } else {
            throw new BadArgumentActionException();
        }

        if (!source.exists()) {
            throw new ActionException("File not found");
        }
        

        File dest = new File(ctx.getSketch().getFolder(), source.getName());
        if (dest.exists()) {
            throw new ActionException("File already exists");
        }

        try {
            String data = Utils.getFileAsString(source);
            ctx.getSketch().createNewFile(dest.getName(), data);
        } catch (IOException ex) {
            throw new ActionException(ex.getMessage());
        }
        return true;
    }
}
