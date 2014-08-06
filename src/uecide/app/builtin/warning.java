package uecide.app.builtin;

import uecide.app.*;

public class warning {
    public static boolean main(Editor editor, String[] arg) {
        StringBuilder sb = new StringBuilder();
        for (String s : arg) {
            sb.append(s);
            sb.append(" ");
        }
        if (editor != null) {
            editor.warning(sb.toString());
        } else {
            System.out.println(sb.toString());
        }
        return true;
    }
}
