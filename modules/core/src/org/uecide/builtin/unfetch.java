
package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

/* Replace the contents of the active editor with the content of a variable
 *
 * Usage:
 *     __builtin_unfetch::key.name
 */

public class unfetch extends BuiltinCommand {
    public unfetch(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        if (arg.length == 0) { // We need a variable
            return false;
        }

        SketchFile sf = ctx.getActiveSketchFile();
        if (sf == null) {
            return false;
        }

        sf.setFileData(ctx.get(arg[0]));
        return true;
    }

    public void kill() {
    }
}
