package org.uecide.actions;

import org.uecide.*;
import java.io.File;
import java.util.TreeSet;

public class CloseSessionAction extends Action {

    public CloseSessionAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "CloseSession"
        };
    }

    public String getCommand() { return "closesession"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        // If we're closing a system context then let's just quit. 
        if (ctx.isSystemContext()) {
            System.exit(0);
        }

        if (ctx.getSketch().isModified()) {
            TreeSet<SketchFile> modifiedFiles = ctx.getSketch().getModifiedFiles();
            String message = "Do you want to save *" + ctx.getSketch().getName() + "* before closing?\n\n";

            message += "The following files have been modified:\n";

            for (SketchFile f : modifiedFiles) {
                message += "* " + f.getFile().getName() + "\n";
            }

            int resp = ctx.getGui().askYesNoCancel(message);
            if (resp == 0) { // yes
                if (ctx.action("saveSketch")) {
                    ctx.getGui().close();
                }
            } else if (resp == 1) { // no
                ctx.getGui().close();
            } else if (resp == 2) { // cancel
                return false;
            }
        } else {
            ctx.getGui().close();
        }
        return true;
    }
}
