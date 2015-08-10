package org.uecide.builtin;

import org.uecide.*;

public class dot implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        ctx.messageStream(".");
        return true;
    }
}
