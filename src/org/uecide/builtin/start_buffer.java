package org.uecide.builtin;

import org.uecide.*;

public class start_buffer extends BuiltinCommand {
    public start_buffer(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        ctx.warning("Use of __builtin_start_buffer is broken");
//        ctx.startBuffer(arg.length == 1);
        return true;
    }

    public void kill() {
    }
}
