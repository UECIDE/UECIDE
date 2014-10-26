package org.uecide.builtin;

import org.uecide.*;

public class cout implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
        }

        sketch.message(sb.toString());
        return true;
    }
}
