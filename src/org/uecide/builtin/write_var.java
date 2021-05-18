package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

/* Write the contents of a variable to a file
 *
 * Usage:
 *     __builtin_write_var::file::key.name
 */

public class write_var extends BuiltinCommand {
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        try {
            File f = new File(arg[0]);
            
            PrintWriter pw = new PrintWriter(f);

            for (int i = 1; i < arg.length; i++) {
                pw.print(ctx.get(arg[i]));
            }
            pw.close();

            return true;
        } catch (Exception e) {
            ctx.error(e);
        }
        return false;
    }

    public void kill() {
    }
}

