package org.uecide.varcmd;

import org.uecide.*;
import java.io.*;

public class vc_preproc implements VariableCommand {

    public String main(Context ctx, String args) {
        ctx.snapshot();
        String file = args;
        String[] bits = args.split(",");

        if (bits.length > 1) {
            file = bits[0];
            for (int i = 1; i < bits.length; i++) {
                String[] portion = bits[i].split("=");
                ctx.set(portion[0], portion[1]);
            }
        }

        File infile = new File(file);
        if (!infile.exists()) {
            ctx.rollback();
            return "FILE NOT FOUND";
        }

        String data = Base.getFileAsString(infile);
        data = ctx.parseString(data);

        String extension = null;
        String[] fbits = infile.getName().split(".");
        if (fbits.length > 1) {
            extension = "." + fbits[fbits.length-1];
        }

        File tempfile = null;

        try {
            tempfile = File.createTempFile("uecide-preproc-", extension);
            tempfile.deleteOnExit();

            PrintWriter pw = new PrintWriter(tempfile);
            pw.print(data);
            pw.close();
        } catch (Exception e) {
        }

        ctx.rollback();
        if (tempfile != null) {
            return tempfile.getAbsolutePath();
        } else {
            return "UNABLE TO CREATE TEMPFILE";
        }
    }
}
