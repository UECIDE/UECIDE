package org.uecide.actions;

import org.uecide.Context;

public class RunKeyAction extends Action {

    public RunKeyAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "RunKey <key>"
        };
    }

    public String getCommand() { return "runkey"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }
        if (!(args[0] instanceof String)) {
            throw new BadArgumentActionException();
        }
        return (Boolean)ctx.executeKey((String)args[0]);
    }
}
