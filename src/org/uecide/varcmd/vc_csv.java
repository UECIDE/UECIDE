package org.uecide.varcmd;

import org.uecide.Context;
import org.uecide.Utils;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

public class vc_csv extends VariableCommand {
    public String main(Context ctx, String args) throws VariableCommandException {

        String[] arg = args.split(",");
        if (arg.length != 3) {
            throw new VariableCommandException("Syntax Error");
        }

        File f = new File(arg[0]);
        if (!f.exists()) {
            throw new VariableCommandException("File not found: " + f.getAbsolutePath());
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }

                String[] entries = line.split(",");
                if (entries[0].trim().equals(arg[1])) {
                    br.close();
                    return entries[Utils.s2i(arg[2])].trim();
                }
            }
            br.close();
        } catch (Exception e) {
            throw new VariableCommandException(e.getMessage());
        }

        throw new VariableCommandException("Entry not found in CSV");
    }
}
