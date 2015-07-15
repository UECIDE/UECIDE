package org.uecide.varcmd;

import org.uecide.*;

public class vc_char implements VariableCommand {
    public String main(Context sketch, String args) {
        if (args.startsWith("0x")) {
            try {
                int ch = Integer.parseInt(args.substring(2, 16));
                return Character.toString((char)ch);
            } catch (Exception e) {
                return " ";
            }
        }
        return args;
    }
}
