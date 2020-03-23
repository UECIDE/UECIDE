package org.uecide.builtin;

import org.uecide.Context;

public class input extends BuiltinCommand {
    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        if (arg.length != 2) {
            throw new BuiltinCommandException("Syntax error");
        }

        String target = arg[0];
        String question = arg[1];

        String answer = ctx.getGui().askString(question, "");
        if ((answer == null) || answer.equals("")) {
            return false;
        }
        ctx.set(target, answer);
        return true;
    }

    public void kill() {
    }
}
