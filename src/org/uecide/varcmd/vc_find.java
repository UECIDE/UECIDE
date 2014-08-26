package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

public class vc_find implements VariableCommand {
    public String main(Sketch sketch, String args) {
        int commaPos = args.indexOf(',');

        if(commaPos > 0) {
            String data = args.substring(0, commaPos);
            String fname = args.substring(commaPos + 1);
            String[] each = data.split("::");

            String outString = "";

            for(String chunk : each) {
                File f = new File(chunk);

                if(f.exists() && f.isDirectory()) {
                    File ff = new File(f, fname);

                    if(ff.exists()) {
                        return ff.getAbsolutePath();
                    }
                }
            }

            return "Not found";
        } else {
            return "Syntax Error in find";
        }
    }
}
