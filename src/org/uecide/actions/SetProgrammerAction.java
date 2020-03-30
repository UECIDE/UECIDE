package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class SetProgrammerAction extends Action {

    public SetProgrammerAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "SetProgrammer <codename>"
        };
    }

    public String getCommand() { return "setprogrammer"; }

    static boolean inhibitUpdate = false;

    public boolean actionPerformed(Object[] args) throws ActionException {
        if (inhibitUpdate) return false; // I hate this!
        try {
            if (args.length != 1) {
                throw new SyntaxErrorActionException();
            }

            if (args[0] instanceof Programmer) {
                Programmer prog = (Programmer)args[0];
                ctx.setProgrammer(prog);
                inhibitUpdate = true;
                prog.onSelected(ctx);
                inhibitUpdate = false;
                return true;
            } else if (args[0] instanceof String) {
                String s = (String)args[0];
                Programmer b = Programmer.getProgrammer(s);
                if (b == null) {
                    throw new ActionException("Unknown Programmer");
                }
                ctx.setProgrammer(b);
                inhibitUpdate = true;
                b.onSelected(ctx);
                inhibitUpdate = false;
                return true;
            }
            throw new BadArgumentActionException();
        } catch (Exception ex) {
            Debug.exception(ex);
            ex.printStackTrace();
            throw new ActionException(ex.getMessage());
        }
    }
}
