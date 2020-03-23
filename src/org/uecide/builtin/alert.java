package org.uecide.builtin;

import org.uecide.Context;

public class alert extends BuiltinCommand {
    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        if (arg.length != 1) {
            throw new BuiltinCommandException("Syntax error");
        }

        String message = arg[0];

        ctx.getGui().alert(message);
        return true;
    }

    public void kill() {
    }
}
