package org.uecide.varcmd;

import org.uecide.*;

public class vc_sketch implements VariableCommand {
    public String main(Sketch sketch, String args) {
        if (args.equals("root")) {
            return sketch.getFolder().getAbsolutePath();
        }
        if (args.equals("name")) {
            return sketch.getName();
        }
        return "[ERR]";
    }
}
