package org.uecide.builtin;

import org.uecide.*;

public class bullet3 extends BuiltinCommand {
    public bullet3(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        ctx.bullet3(sb.toString());
        return true;
    }

    public void kill() {
    }
}
