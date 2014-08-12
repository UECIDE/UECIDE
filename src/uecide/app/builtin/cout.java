package uecide.app.builtin;

import uecide.app.*;

public class cout {
    public static boolean main(Sketch sketch, String[] arg) {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
        }

        sketch.rawMessage(sb.toString());
        return true;
    }
}
