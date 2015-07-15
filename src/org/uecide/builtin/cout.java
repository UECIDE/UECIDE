package org.uecide.builtin;

import org.uecide.*;

public class cout implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
        }

        ctx.message(sb.toString());
        return true;
    }
}
