package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class append_var extends BuiltinCommand {
    public append_var(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        try {
            File f = new File(arg[0]);

            FileWriter fw = new FileWriter(f, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            for (int i = 1; i < arg.length; i++) {
                pw.println(ctx.get(arg[i]));
            }
            pw.close();
            bw.close();
            fw.close();

            return true;
        } catch (Exception e) {
            Debug.exception(e);
            throw new BuiltinCommandException(e.getMessage());
        }
    }

    public void kill() {
    }
}
