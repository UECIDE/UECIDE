package org.uecide.actions;

import org.uecide.*;

public class SetOptionAction extends Action {

    public SetOptionAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "SetOption <option> <value>"
        };
    }

    public String getCommand() { return "setoption"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        if (args.length != 2) {
            throw new SyntaxErrorActionException();
        }

        if (!(args[0] instanceof String)) {
            throw new BadArgumentActionException();
        }

        if (!(args[1] instanceof String)) {
            throw new BadArgumentActionException();
        }

        String key = (String)args[0];
        String val = (String)args[1];

        ctx.getSketch().setOption(key, val);

        ctx.triggerEvent("optionChanged", key);
        return true;
    }
}
