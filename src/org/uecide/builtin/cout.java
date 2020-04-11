package org.uecide.builtin;

import org.uecide.*;

public class cout extends BuiltinCommand {
    public cout(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
        }

        ctx.message(sb.toString());
        return true;
    }

    public void kill() {
    }
}
