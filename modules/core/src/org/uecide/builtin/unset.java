package org.uecide.builtin;

import org.uecide.*;

/* Unset a key
 *
 * Usage:
 *     __builtin_unset::key.name
 */

public class unset extends BuiltinCommand {
    public unset(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        for (String key : arg) {
            ctx.unset(key);
        }
        return true;
    }

    public void kill() {
    }
}
