package org.uecide.builtin;

import org.uecide.*;

public class set implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        if (arg.length != 2) {
            sketch.error("Usage: __builtin_set::variable::value");
            return false;
        }

        sketch.settings.put(arg[0], arg[1]);
        return true;
    }
}
