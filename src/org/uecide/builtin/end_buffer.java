package org.uecide.builtin;

import org.uecide.*;

public class end_buffer extends BuiltinCommand {
    public end_buffer(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
//        String out = ctx.endBuffer();
//        if (arg.length == 1) {
//            ctx.set(arg[0], out);
//        }
        return true;
    }

    public void kill() {
    }
}
