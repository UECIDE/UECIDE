package uecide.app.builtin;

import uecide.app.*;

public class error {
    public static boolean main(Editor editor, String[] arg) {
        StringBuilder sb = new StringBuilder();
        for (String s : arg) {
            sb.append(s);
            sb.append(" ");
        }
        if (editor != null) {
            editor.error(sb.toString());
        } else {
            System.err.println(sb.toString());
        }
        return true;
    }
}
