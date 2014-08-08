package uecide.app.builtin;

import uecide.app.*;

public class echo {
    public static boolean main(Sketch sketch, String[] arg) {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
            sb.append(" ");
        }

        sketch.message(sb.toString());
        return true;
    }
}
