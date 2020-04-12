package org.uecide.builtin;

import org.uecide.Context;

import java.io.File;

/* Test if a file exists or not. Jumps to one of two keys
 *
 * Usage:
 *     __builtin_exists::file::key.true::key.false
 */

public class exists extends BuiltinCommand {
    public exists(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length != 3) {
            throw new BuiltinCommandException("Syntax error");
        }

        String path = arg[0];
        String yes = arg[1];
        String no = arg[2];

        File f = new File(path);
        if (f.exists()) {
            return ctx.action("runKey", yes);
        } 

        return ctx.action("runKey", no);
    }

    public void kill() {
    }
}
