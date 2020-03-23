package org.uecide.builtin;

import org.uecide.Context;

public class askyesno extends BuiltinCommand {
    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        if (arg.length != 3) {
            throw new BuiltinCommandException("Syntax error");
        }

        String question = arg[0];
        String yesKey = arg[1];
        String noKey = arg[2];

        boolean answer = ctx.getGui().askYesNo(question);
        if (answer) {
            ctx.action("runKey", yesKey);
        } else {
            ctx.action("runKey", noKey);
        }
        return true;
    }

    public void kill() {
    }
}
