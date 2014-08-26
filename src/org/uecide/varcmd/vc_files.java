package org.uecide.varcmd;

import org.uecide.*;
import java.io.*;

// Given a folder, list all the files with a given extension within that folder.

public class vc_files implements VariableCommand {
    public String main(Sketch sketch, String args) {
        int commaPos = args.indexOf(',');

        String path = args;
        String extension = "";
        if(commaPos > 0) {
            path = args.substring(0, commaPos);
            extension = args.substring(commaPos + 1);
        }

        File dir = new File(path);
        if (!dir.exists()) {    
            return "[not found]";
        }
        if (!dir.isDirectory()) {
            return "[not directory]";
        }

        File[] files = dir.listFiles();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (File f : files) {
            if (!f.getName().endsWith(extension)) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(f.getAbsolutePath());
        }
        return sb.toString();
    }
}
