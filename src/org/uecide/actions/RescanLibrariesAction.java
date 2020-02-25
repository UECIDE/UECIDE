package org.uecide.actions;

import org.uecide.Context;
import org.uecide.LibraryManager;

public class RescanLibrariesAction extends Action {

    public RescanLibrariesAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "RescanLibraries"
        };
    }

    public String getCommand() { return "rescanlibraries"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        LibraryManager.rescanAllLibraries();
        return true;
    }
}
