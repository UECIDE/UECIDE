package org.uecide.varcmd;

import org.uecide.*;

public class vc_option implements VariableCommand {
    public String main(Sketch sketch, String args) {

        String[] bits = args.split("\\.");

        if (bits.length != 2) {
            return "ERR1";
        }

        String opt = bits[0];
        String key = bits[1];
        String optval = sketch.getOption(opt);

        if (optval == null) {
            return "ERR2";
        }

        String val = "options." + opt + "." + optval + "." + key;
        PropertyFile props = sketch.mergeAllProperties();
        String retval = props.get(val);
        if (retval == null) {
            return "ERR3";
        }
        return retval;
    }
}
