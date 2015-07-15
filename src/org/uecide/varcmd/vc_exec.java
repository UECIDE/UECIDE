package org.uecide.varcmd;

import org.uecide.*;

public class vc_exec implements VariableCommand {
    public String main(Context sketch, String args) {
        if(sketch.executeKey(args)) {
            return "true";
        }

        return "false";
    }
}
