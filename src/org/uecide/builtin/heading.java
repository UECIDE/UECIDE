package org.uecide.builtin;

import org.uecide.*;

/* Display a top-level heading point
 *
 * Usage:
 *     __builtin_heading::message
 */

public class heading extends BuiltinCommand {
    public heading(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        ctx.heading(sb.toString());
        return true;
    }

    public void kill() {
    }
}
