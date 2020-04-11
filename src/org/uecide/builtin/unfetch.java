
package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

// Fetch all the text of the active editor into a variable
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
