package org.uecide.actions;

import org.uecide.Package;
import org.uecide.APT;
import org.uecide.Context;
import java.util.TreeMap;

public class AptInstallAction extends Action {

    public AptInstallAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "AptInstall <package>"
        };
    }

    public String getCommand() { return "aptinstall"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }

        Package pkg;
        APT apt;

        try {
            apt = APT.factory(ctx);
        } catch (Exception ex) {
            throw new ActionException(ex.getMessage());
        }

        if (args[0] instanceof String) {
            pkg = apt.getPackage((String)args[0]);
            if (pkg == null) {
                throw new ActionException("Package not found");
            }
        } else if (args[0] instanceof Package) {
            pkg = (Package)args[0];
        } else {
            throw new BadArgumentActionException();
        }

        ctx.message("Installing " + pkg.getName() + " ... ");
        try {
            apt.installPackage(pkg);
        } catch (Exception ex) {
            throw new ActionException(ex.getMessage());
        }
        ctx.message("Done");
        return true;
    }
}
