package org.uecide.actions;

import org.uecide.Package;
import org.uecide.APT;
import org.uecide.Context;
import org.uecide.Debug;
import org.uecide.Version;
import java.util.TreeMap;

public class AptSearchAction extends Action {

    public AptSearchAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "AptSearch <package>"
        };
    }

    public String getCommand() { return "aptsearch"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }

        APT apt = null;

        try {
            apt = APT.factory(ctx);
        } catch (Exception ex) {
            Debug.exception(ex);
            throw new ActionException(ex.getMessage());
        }

        if (!(args[0] instanceof String)) {
            throw new BadArgumentActionException();
        }

        String searchTerm = ((String)args[0]).toLowerCase();

        Package[] pkgs = apt.getPackages();
        String format = "%-50s %10s %10s %s";
        ctx.message(String.format(format, "Package Name", "Installed", "Available", ""));
        for (Package p : pkgs) {
            String name = p.getName();
            Package instPack = apt.getInstalledPackage(p.getName());
            Version avail = p.getVersion();
            Version inst = null;
            String msg = "";
            if (instPack != null) {
                inst = instPack.getVersion();
                if (avail.compareTo(inst) > 0) {
                    msg = "Update";
                }
            }
            String comp = p.getName() + " " + p.getDescription();
            if (comp.toLowerCase().contains(searchTerm)) {
                ctx.message(String.format(format, name, inst == null ? "" : inst.toString(), avail.toString(), msg));
                ctx.message("  " + p.getDescriptionLineOne());
            }
        }
        
        return true;
    }
}
