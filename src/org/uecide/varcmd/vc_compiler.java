package org.uecide.varcmd;

import org.uecide.*;

public class vc_compiler implements VariableCommand {
    public String main(Sketch sketch, String args) {
        if(args.equals("root")) {
            return sketch.getCompiler().getFolder().getAbsolutePath();
        }

        return sketch.getCompiler().get(args);
    }
}
