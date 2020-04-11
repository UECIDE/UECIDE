package org.uecide.builtin;

import org.uecide.*;

public class error extends BuiltinCommand {
    public error(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        ctx.error(sb.toString());
        return true;
    }

    public void kill() {
    }
}
