package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class append implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        try {
            File f = new File(arg[0]);

            FileWriter fw = new FileWriter(f, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            for (int i = 1; i < arg.length; i++) {
                pw.println(arg[i]);
            }
            pw.close();
            bw.close();
            fw.close();

            return true;
        } catch (Exception e) {
            Base.error(e);
        }
        return false;
    }
}
