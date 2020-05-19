package org.uecide.builtin;

import org.uecide.*;

/* Display a second-level bullet point
 *
 * Usage:
 *     __builting_bullet2::message
 */

public class bullet2 extends BuiltinCommand {
    public bullet2(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        ctx.bullet2(sb.toString());
        return true;
    }

    public void kill() {
    }
}
