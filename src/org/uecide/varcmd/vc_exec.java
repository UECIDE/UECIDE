package org.uecide.varcmd;

import org.uecide.*;

public class vc_exec implements VariableCommand {
    public String main(Sketch sketch, String args) {
        if(sketch.executeKey(args)) {
            return "true";
        }

        return "false";
    }
}
