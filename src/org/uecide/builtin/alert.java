package org.uecide.builtin;

import org.uecide.Context;

public class alert extends BuiltinCommand {
    public alert(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
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
