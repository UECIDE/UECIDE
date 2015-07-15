package org.uecide.builtin;

import org.uecide.*;
import java.io.*;
import java.net.*;

public class loadjar implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        try {
            if (arg.length != 1) {
                return false;
            }
            File f = new File(arg[0]);

            if (!f.exists()) {
                ctx.error("Error: " + f.getAbsolutePath() + " could not be found.");
                return false;
            }

            URL u = f.toURI().toURL();
            Base.addURL(u);
            return true;
        } catch (Exception e) {
            Base.error(e);
            return false;
        }
    }
}
