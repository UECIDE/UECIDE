package org.uecide.builtin;

import org.uecide.*;

public class bullet implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        ctx.bullet(sb.toString());
        return true;
    }
}
