package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

/* Append the content of a variable to a file (includes newline)
 *
 * Usage:
 *     __builtin_append::file::variable.name
 */

public class append_var extends BuiltinCommand {
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        try {
            File f = new File(arg[0]);

            FileWriter fw = new FileWriter(f, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            for (int i = 1; i < arg.length; i++) {
                pw.println(ctx.get(arg[i]));
            }
            pw.close();
            bw.close();
            fw.close();

            return true;
        } catch (Exception e) {
            throw new BuiltinCommandException(e.getMessage());
        }
    }

    public void kill() {
    }
}

