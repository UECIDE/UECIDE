package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class BuildAction extends Action {

    public BuildAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {
            return ctx.getSketch().build();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ActionException(ex.getMessage());
        }
    }
}
