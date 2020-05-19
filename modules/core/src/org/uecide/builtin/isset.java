package org.uecide.builtin;

import org.uecide.Context;
import org.uecide.PropertyFile;

/* Test if a key is set or not. Jumps to one of two keys
 *
 * Usage:
 *     __builtin_isset::key.name::key.true::key.false
 */

public class isset extends BuiltinCommand {
    public isset(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length != 3) {
            throw new BuiltinCommandException("Syntax error");
        }

        String key = arg[0];
        String yes = arg[1];
        String no = arg[2];

        PropertyFile pf = ctx.getMerged();
        String res = pf.get(key);

        if (res == null) {
            return ctx.action("runKey", no);
        } 

        return ctx.action("runKey", yes);
    }

    public void kill() {
    }
}
