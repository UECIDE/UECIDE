package org.uecide.builtin;

import org.uecide.*;

/* Append a value to an "array" string (a string of terms separated by ::)
 *
 * Usage:
 *     __builtin_push::key.name::value
 */

public class push extends BuiltinCommand {
    public push(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length != 2) {
            throw new BuiltinCommandException("Syntax Error");
        }

        String val = null;
        val = ctx.get(arg[0]);
        if (val == null) {
            val = "";
        }

        if (!val.equals("")) {
            val += "::";
        }

        val += arg[1];

        ctx.set(arg[0], val);
        return true;
    }

    public void kill() {
    }
}
