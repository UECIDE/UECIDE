package org.uecide.builtin;

import org.uecide.*;

/* Display a warning message
 *
 * Usage:
 *     __builtin_warning::message[::...]
 */

public class warning extends BuiltinCommand {
    public warning(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        ctx.warning(sb.toString());
        return true;
    }

    public void kill() {
    }
}
