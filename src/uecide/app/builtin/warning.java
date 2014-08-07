package uecide.app.builtin;

import uecide.app.*;

public class warning {
    public static boolean main(Sketch sketch, String[] arg) {
        StringBuilder sb = new StringBuilder();
        for (String s : arg) {
            sb.append(s);
            sb.append(" ");
        }
        sketch.warning(sb.toString());
        return true;
    }
}
