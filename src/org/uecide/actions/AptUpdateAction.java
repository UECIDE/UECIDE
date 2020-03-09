package org.uecide.actions;

import org.uecide.Package;
import org.uecide.APT;
import org.uecide.Context;
import java.util.TreeMap;

public class AptUpdateAction extends Action {

    public AptUpdateAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "AptUpdate"
        };
    }

    public String getCommand() { return "aptupdate"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        APT apt;
        try {
            apt = APT.factory(ctx);
            apt.update();
        } catch (Exception ex) {
            throw new ActionException(ex.getMessage());
        }
        return true;
    }
}
