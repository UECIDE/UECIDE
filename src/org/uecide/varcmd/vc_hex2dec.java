package org.uecide.varcmd;

import org.uecide.Context;

public class vc_hex2dec extends VariableCommand {
    public String main(Context sketch, String args) throws VariableCommandException {
        String out = "0";
        try {
            if (!args.startsWith("0x")) {
                args = "0x" + args;
            }
            int val = Integer.decode(args);
            out = "" + val;
        } catch (Exception ex) {
            throw new VariableCommandException(ex.getMessage());
        }
        return out;
    }
}

