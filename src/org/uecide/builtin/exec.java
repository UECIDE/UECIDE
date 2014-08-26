package org.uecide.builtin;

import org.uecide.*;

public class exec implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        if(arg.length != 1) {
            sketch.error("Usage: __builtin_exec::<script key>");
            return false;
        }

        String key = arg[0];

        return sketch.executeKey(key);

    }
}
