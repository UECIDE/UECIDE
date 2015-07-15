package org.uecide.varcmd;

import org.uecide.*;

public class vc_option implements VariableCommand {
    public String main(Context ctx, String args) {

        String[] bits = args.split("\\.");

        if (bits.length != 2) {
            return "ERR1";
        }

        String opt = bits[0];
        String key = bits[1];
        String optval = ctx.getSketch().getOption(opt);

        if (optval == null) {
            return "ERR2";
        }

        String val = "options." + opt + "." + optval + "." + key;
        PropertyFile props = ctx.getMerged();
        String retval = props.get(val);
        if (retval == null) {
            return "ERR3";
        }
        return retval;
    }
}
