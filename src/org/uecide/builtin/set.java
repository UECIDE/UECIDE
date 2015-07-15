package org.uecide.builtin;

import org.uecide.*;

public class set implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        if (arg.length != 2) {
            ctx.error("Usage: __builtin_set::variable::value");
            return false;
        }

        ctx.set(arg[0], arg[1]);
        return true;
    }
}
