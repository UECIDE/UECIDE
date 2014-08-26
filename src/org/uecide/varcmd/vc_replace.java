package org.uecide.varcmd;

import org.uecide.*;

public class vc_replace implements VariableCommand {
    public String main(Sketch sketch, String args) {
        String[] bits = args.split(",");

        if(bits.length != 3) {
            return "Syntax error in replace - bad arg count";
        } else {
            return bits[0].replaceAll(bits[1], bits[2]);
        }
    }
}
