package org.uecide.builtin;

import org.uecide.*;
import java.io.*;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;

/* Delete a file
 *
 * Usage:
 *     __builtin_rm::file
 */

public class rm extends BuiltinCommand {
    public rm(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        if (arg.length != 1) {
            throw new BuiltinCommandException("Syntax Error");
        }

        File f = new File(arg[0]);
        return f.delete();
    }

    public void kill() {
    }
}
