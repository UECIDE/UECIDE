package org.uecide.varcmd;

import org.uecide.*;

public class vc_ucase extends VariableCommand {
    public String main(Context sketch, String args) throws VariableCommandException {
        return args.toUpperCase();
    }
}
