package org.uecide.builtin;

import org.uecide.Context;

public class askyesnocancel extends BuiltinCommand {
    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        if (arg.length != 4) {
            throw new BuiltinCommandException("Syntax error");
        }

        String question = arg[0];
        String yesKey = arg[1];
        String noKey = arg[2];
        String cancelKey = arg[3];

        int answer = ctx.getGui().askYesNoCancel(question);
        switch (answer) {
            case 0: ctx.action("runKey", yesKey); break;
            case 1: ctx.action("runKey", noKey); break;
            case 2: ctx.action("runKey", cancelKey); break;
        }
        return true;
    }

    public void kill() {
    }
}
