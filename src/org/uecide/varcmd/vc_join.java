package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

public class vc_join implements VariableCommand {
    public String main(Sketch sketch, String args) {
        int comma = args.indexOf(",");
        if (comma == -1) {
            return "[syntax error]";
        }
    
        String array = args.substring(0, comma);
        String joiner = args.substring(comma + 1);
        return array.replaceAll("::", joiner);
    }
}
