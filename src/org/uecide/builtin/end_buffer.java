package org.uecide.builtin;

import org.uecide.*;

public class end_buffer implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        String out = ctx.endBuffer();
        if (arg.length == 1) {
            ctx.set(arg[0], out);
        }
        return true;
    }
}
