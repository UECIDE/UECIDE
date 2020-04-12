package org.uecide.builtin;

import org.uecide.*;

/* Display a top-level bullet point
 *
 * Usage:
 *     __builtin_bullet::message
 */

public class bullet extends BuiltinCommand {
    public bullet(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        ctx.bullet(sb.toString());
        return true;
    }

    public void kill() {
    }
}
