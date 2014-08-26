package org.uecide.builtin;

import org.uecide.*;

public class echo implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        sketch.message(sb.toString());
        return true;
    }
}
