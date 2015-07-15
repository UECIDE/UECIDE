package org.uecide.varcmd;

import org.uecide.*;

public class vc_select implements VariableCommand {
    public String main(Context context, String args) {
        PropertyFile props = context.getMerged();
        String[] keys = props.childKeysOf(args);
        String out = "";
        for (String k : keys) {
            if (!out.equals("")) {
                out += "::";
            }
            out += args + "." + k;
        }

        System.err.println("Select: " + out);

        return out;
    }
}
