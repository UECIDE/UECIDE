package org.uecide.varcmd;

import org.uecide.*;

public class vc_sketch implements VariableCommand {
    public String main(Context ctx, String args) {
        if (args.equals("root")) {
            return ctx.getSketch().getFolder().getAbsolutePath();
        }
        if (args.equals("name")) {
            return ctx.getSketch().getName();
        }
        return "[ERR]";
    }
}
