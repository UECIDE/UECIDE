package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

// CSV File Entry Reader.
// 
// Reads an entry from a CSV file (arg 0) where the entry in the first column matches arg 1
// and reads from the entry numbered in arg 2
//
// Usage: ${csv:file,match,column}
// Example: ${csv:partitions.csv,app0,3}

public class vc_csv extends VariableCommand {
    public String main(Context sketch, String args) throws VariableCommandException {
        String[] arg = args.split(",");
        if (arg.length != 3) {
            throw new VariableCommandException("Syntax error");
        }

        File f = new File(arg[0]);
        if (!f.exists()) {
            throw new VariableCommandException("File not found");
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    // Skip comments
                    continue;
                }
                String[] entries = line.split(",");
                if (entries[0].trim().equals(arg[1])) {
                    return entries[Utils.s2i(arg[2])].trim();
                }
            }
            br.close();
            return "NOTFOUND";
        } catch (Exception ex) {
            throw new VariableCommandException(ex.getMessage());
        }
    }
}
