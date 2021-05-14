package org.uecide.varcmd;

import org.uecide.Context;
import java.io.File;

public class vc_exists extends VariableCommand {
    public String main(Context ctx, String args) throws VariableCommandException {
        File f = new File(args);
        if (f.exists()) {
            return "YES";
        }   
        return "NO";
    }
}
