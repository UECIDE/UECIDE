package org.uecide.varcmd;

import org.uecide.Context;
import java.io.File;

// A simple "YES" or "NO" to the question "Does this file exist?
//
// ${exists:/path/to/file} => YES / NO

public class vc_exists extends VariableCommand {
    public String main(Context sketch, String args) throws VariableCommandException {
        File f = new File(args);
        if (f.exists()) return "YES";
        return "NO";
    }
}
