package org.uecide.varcmd;

import org.uecide.*;

public class vc_select implements VariableCommand {
    public String main(Sketch sketch, String args) {
        PropertyFile props = sketch.mergeAllProperties();
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
