package uecide.app.builtin;

import uecide.app.*;

public class echo {
    public static boolean main(Editor editor, String[] arg) {
        StringBuilder sb = new StringBuilder();
        for (String s : arg) {
            sb.append(s);
            sb.append(" ");
        }
        if (editor != null) {
            editor.message(sb.toString());
        } else {
            System.out.println(sb.toString());
        }
        return true;
    }
}
