package org.uecide.actions;

import org.uecide.Context;
import org.uecide.LibraryManager;
import org.uecide.Utils;

import java.io.File;

public class AddLibraryAction extends Action {

    public AddLibraryAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "AddLibrary <path>"
        };
    }

    public String getCommand() { return "addlibrary"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        File source = null;
        Integer priority = 100;
        String name = null;

        if (args.length != 3) {
            throw new SyntaxErrorActionException();
        }

        if (args[0] instanceof File) {
            source = (File)args[0];
        } else if (args[0] instanceof String) {
            source = new File((String)args[0]);
        } else {
            throw new BadArgumentActionException();
        }

        if (args[1] instanceof Integer) {
            priority = (Integer)args[1];
        } else if (args[1] instanceof String) {
            priority = Utils.s2i((String)args[1]);
        } else {
            throw new BadArgumentActionException();
        }

        if (args[2] instanceof String) {
            name = (String)args[2];
        } else {
            throw new BadArgumentActionException();
        }
            
        return LibraryManager.addLibrary(source, priority, name);
    }
}
