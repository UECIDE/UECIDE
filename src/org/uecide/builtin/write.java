package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class write extends BuiltinCommand {
    public write(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        try {
            File f = new File(arg[0]);
            
            PrintWriter pw = new PrintWriter(f);

            for (int i = 1; i < arg.length; i++) {
                pw.println(arg[i]);
            }
            pw.close();

            return true;
        } catch (Exception e) {
            Debug.exception(e);
            ctx.error(e);
        }
        return false;
    }

    public void kill() {
    }
}
