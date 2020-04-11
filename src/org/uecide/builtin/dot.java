package org.uecide.builtin;

import org.uecide.*;

public class dot extends BuiltinCommand {
    public dot(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        ctx.messageStream(".");
        return true;
    }

    public void kill() {
    }
}
