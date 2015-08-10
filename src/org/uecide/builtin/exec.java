package org.uecide.builtin;

import org.uecide.*;

public class exec implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        if(arg.length != 1) {
            ctx.error("Usage: __builtin_exec::<script key>");
            return false;
        }

        String key = arg[0];

        return (Boolean)ctx.executeKey(key);

    }
}
