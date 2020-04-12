package org.uecide.builtin;

import org.uecide.*;
import java.io.*;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;

/* Copy file(s)
 *
 * Usage:
 *     __builtin_cp::file[::file...]::destination
 */


public class cp extends BuiltinCommand {
    public cp(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        if (arg.length < 2) {
            throw new BuiltinCommandException("Syntax Error");
        }

        if (arg.length > 2) {
            return copy_many_to_one(arg);
        } else {
            return copy_one_to_one(arg);
        }
    }

    public boolean copy_one_to_one(String[] arg) throws BuiltinCommandException {
        File from = new File(arg[0]);
        File to = new File(arg[1]);
        if (to.exists() && to.isDirectory()) {
            to = new File(to, from.getName());
        }

        try {
            Files.copy(from.toPath(), to.toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            Debug.exception(e);
            throw new BuiltinCommandException(e.getMessage());
        }
        return true;
    }

    public boolean copy_many_to_one(String[] arg) throws BuiltinCommandException {
        File to = new File(arg[arg.length - 1]);
        if (!to.exists() || !to.isDirectory()) {
            return false;
        }
        for (int i = 0; i < arg.length - 2; i++) {
            String[] files = {
                arg[i],
                to.getAbsolutePath()
            };
            if (copy_one_to_one(files) == false) {
                return false;
            }
        }
        return true;
    }

    public void kill() {
    }
}
