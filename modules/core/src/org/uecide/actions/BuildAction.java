package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class BuildAction extends Action {

    public BuildAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "Build"
        };
    }

    public String getCommand() { return "build"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {
            return ctx.getSketch().build();
        } catch (Exception ex) {
            Debug.exception(ex);
            throw new ActionException(ex.getMessage());
        }
    }
}
