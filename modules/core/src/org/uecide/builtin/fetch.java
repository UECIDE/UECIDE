package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

/* Fetch the text from the active editor into a variable
 * 
 * Usage:
 *     __builtin_fetch::key.name
 */

public class fetch extends BuiltinCommand {
    public fetch(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        if (arg.length == 0) { // We need a variable
            return false;
        }

        SketchFile sf = ctx.getActiveSketchFile();
        if (sf == null) {
            return false;
        }

        String txt = sf.getFileData();
        ctx.set(arg[0], txt);
        return true;
    }

    public void kill() {
    }
}
