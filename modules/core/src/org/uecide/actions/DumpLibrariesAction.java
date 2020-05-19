package org.uecide.actions;

import org.uecide.*;
import java.util.TreeMap;

public class DumpLibrariesAction extends Action {

    public DumpLibrariesAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "DumpLibraries"
        };
    }

    public String getCommand() { return "dumplibraries"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        LibraryManager.dumpLibraryList(ctx);
        return true;
    }
}
