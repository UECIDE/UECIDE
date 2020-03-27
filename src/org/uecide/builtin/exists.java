package org.uecide.builtin;

import org.uecide.Context;

import java.io.File;

public class exists extends BuiltinCommand {
    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
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
