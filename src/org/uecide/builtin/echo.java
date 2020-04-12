package org.uecide.builtin;

import org.uecide.*;

/* Display a message on the console
 *
 * Usage:
 *     __builtin_echo::message
 */

public class echo extends BuiltinCommand {
    public echo(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        ctx.message(sb.toString());
        return true;
    }

    public void kill() {
    }
}
