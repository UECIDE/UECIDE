package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class PurgeAction extends Action {

    public PurgeAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "Purge"
        };
    }

    public String getCommand() { return "purge"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {
            ctx.getSketch().purgeCache();
            ctx.getSketch().purgeBuildFiles();
            return true;
        } catch (Exception ex) {
            Debug.exception(ex);
            throw new ActionException(ex.getMessage());
        }
    }
}
