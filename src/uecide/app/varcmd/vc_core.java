package uecide.app.varcmd;

import uecide.app.*;

public class vc_core implements VariableCommand {
    public String main(Sketch sketch, String args) {
        if(args.equals("root")) {
            return sketch.getCore().getFolder().getAbsolutePath();
        }

        return sketch.getCore().get(args);
    }
}
