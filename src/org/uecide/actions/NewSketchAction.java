package org.uecide.actions;

import org.uecide.UECIDE;
import org.uecide.Context;
import java.io.File;

public class NewSketchAction extends Action {

    public NewSketchAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "NewSketch"
        };
    }

    public String getCommand() { return "newsketch"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 0) {
            throw new SyntaxErrorActionException();
        }

        UECIDE.createContext((File)null);
        return true;
    }
}
