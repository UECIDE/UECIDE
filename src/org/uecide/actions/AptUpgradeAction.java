package org.uecide.actions;

import org.uecide.Package;
import org.uecide.APT;
import org.uecide.Context;
import org.uecide.Debug;
import java.util.TreeMap;

public class AptUpgradeAction extends Action {

    public AptUpgradeAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "AptUpgrade"
        };
    }

    public String getCommand() { return "aptupgrade"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        APT apt;
        try {
            apt = APT.factory(ctx);

            if (args.length == 1) {
                Package p;
                if (args[0] instanceof String) {
                    p = apt.getPackage((String)args[0]);
                    if (p == null) {
                        throw new ActionException("Package not found");
                    }
                } else if (args[0] instanceof Package) {
                    p = (Package)args[0];
                } else {
                    throw new BadArgumentActionException();
                }

                apt.upgradePackage(p);
                return true;
            } else {
                Package[] pl = apt.getUpgradeList();
                for (Package p : pl) {
                    apt.upgradePackage(p);
                }
            }
        } catch (Exception ex) {
            Debug.exception(ex);
            throw new ActionException(ex.toString());
        }
        return true;
    }
}
