package org.uecide.builtin;

import org.uecide.*;

import java.io.File;

/* Create a temporary file and store the path in a key
 *
 * Usage:
 *     __builtin_tmpfile::key.name::prefix::suffix
 */

public class tmpfile extends BuiltinCommand {
    public tmpfile(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length != 3) {
            throw new BuiltinCommandException("Syntax Error");
        }

        try {
            File f = File.createTempFile(arg[1], arg[2]);
            f.deleteOnExit();
            ctx.set(arg[0], f.getAbsolutePath());
            return true;
        } catch (Exception ex) {
            Debug.exception(ex);
        }
        return false;
    }

    public void kill() {
    }
}
