package uecide.app.varcmd;

import uecide.app.*;

public class vc_board {
    public static String main(Sketch sketch, String args) {
        if(args.equals("root")) {
            return sketch.getBoard().getFolder().getAbsolutePath();
        }

        return sketch.getBoard().get(args);
    }
}
