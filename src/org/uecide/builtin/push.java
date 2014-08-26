package org.uecide.builtin;

import org.uecide.*;

public class push implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        if (arg.length != 2) {
            sketch.error("Usage: __builtin_push::variable::value");
            return false;
        }

        String val = sketch.settings.get(arg[0]);
        if (val == null) {
            val = "";
        }

        if (!val.equals("")) {
            val += "::";
        }

        val += arg[1];

        sketch.settings.put(arg[0], val);
        return true;
    }
}
