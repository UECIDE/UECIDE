package org.uecide.builtin;

import org.uecide.*;

/* Run a script or key
 *
 * Usage:
 *     __builtin_exec::key.name
 */

public class exec extends BuiltinCommand {
    public exec(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if(arg.length != 1) {
            throw new BuiltinCommandException("Syntax Error");
        }

        String key = arg[0];

        return ctx.action("runKey", key);

    }

    public void kill() {
    }
}
