package org.uecide.builtin;

import org.uecide.*;

/* Set key to a value
 *
 * Usage:
 *     __builtin_set::key.name::value
 */

public class set extends BuiltinCommand {
    public set(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length != 2) {
            ctx.error("Usage: __builtin_set::variable::value");
            return false;
        }

        ctx.set(arg[0], arg[1]);
        return true;
    }

    public void kill() {
    }
}
