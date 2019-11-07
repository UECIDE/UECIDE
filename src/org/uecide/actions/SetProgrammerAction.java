package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class SetProgrammerAction extends Action {

    public SetProgrammerAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {

            if (args.length != 1) {
                throw new SyntaxErrorActionException();
            }

            if (args[0] instanceof Programmer) {
                ctx.setProgrammer((Programmer)args[0]);
                return true;
            } else if (args[0] instanceof String) {
                String s = (String)args[0];
                Programmer b = Base.programmers.get(s);
                if (b == null) {
                    throw new ActionException("Unknown Programmer");
                }
                ctx.setProgrammer(b);
                return true;
            }
            throw new BadArgumentActionException();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ActionException(ex.getMessage());
        }
    }
}
