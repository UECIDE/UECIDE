package org.uecide.builtin;

import org.uecide.*;

public class start_buffer implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
System.err.println("Args: " + arg.length);
        ctx.startBuffer(arg.length == 1);
        return true;
    }
}
