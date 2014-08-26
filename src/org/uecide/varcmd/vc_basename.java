package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

public class vc_basename implements VariableCommand {
    public String main(Sketch sketch, String args) {
        String[] bits = args.split(",");
        String filename = bits[0];
        String extension = "";
        if (bits.length > 1) {
            extension = bits[1];
        }
        File f = new File(filename);
        filename = f.getName();
        if (extension.equals("")) {
            return filename;
        }
        if (!filename.endsWith(extension)) {
            return filename;
        }

        filename = filename.substring(0, filename.lastIndexOf(extension));
        return filename;
    }
}
