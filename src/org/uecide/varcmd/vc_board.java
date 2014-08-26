package org.uecide.varcmd;

import org.uecide.*;

public class vc_board implements VariableCommand {
    public String main(Sketch sketch, String args) {
        if(args.equals("root")) {
            return sketch.getBoard().getFolder().getAbsolutePath();
        }

        return sketch.getBoard().get(args);
    }
}
