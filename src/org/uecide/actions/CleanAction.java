package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class CleanAction extends Action {

    public CleanAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "Clean"
        };
    }

    public String getCommand() { return "clean"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {
            ctx.getSketch().purgeBuildFiles();
            return true;
        } catch (Exception ex) {
            throw new ActionException(ex.getMessage());
        }
    }
}
