package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

/* Read a file into a variable
 *
 * Usage:
 *     __builtin_read_var::file::key.name
 */

public class read_var extends BuiltinCommand {
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        try {
            File f = new File(arg[0]);
            String data = Utils.getFileAsString(f);
            ctx.set(arg[1], data);
            return true;
        } catch (Exception ex) {
            ctx.error(ex);
        }
        return false;
    }

    public void kill() {
    }
}

