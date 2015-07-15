package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class write implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        try {
            File f = new File(arg[0]);
            
            PrintWriter pw = new PrintWriter(f);

            for (int i = 1; i < arg.length; i++) {
                pw.println(arg[i]);
            }
            pw.close();

            return true;
        } catch (Exception e) {
            ctx.error(e);
        }
        return false;
    }
}
