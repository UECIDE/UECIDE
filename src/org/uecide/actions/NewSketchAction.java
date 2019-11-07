package org.uecide.actions;

import org.uecide.Base;
import org.uecide.Context;
import java.io.File;

public class NewSketchAction extends Action {

    public NewSketchAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 0) {
            throw new SyntaxErrorActionException();
        }

        Base.createContext((File)null);
        return true;
    }
}
