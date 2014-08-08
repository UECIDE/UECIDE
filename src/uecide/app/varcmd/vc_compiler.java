package uecide.app.varcmd;

import uecide.app.*;

public class vc_compiler {
    public static String main(Sketch sketch, String args) {
        if (args.equals("root")) {
            return sketch.getCompiler().getFolder().getAbsolutePath();
        }
        return sketch.getCompiler().get(args);
    }
}
