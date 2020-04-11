package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class read_var extends BuiltinCommand {
    public read_var(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        try {
            File f = new File(arg[0]);
            String data = Utils.getFileAsString(f);
            ctx.set(arg[1], data);
            return true;
        } catch (Exception ex) {
            Debug.exception(ex);
        }
        return false;
    }

    public void kill() {
    }
}
