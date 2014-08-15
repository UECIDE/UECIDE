package uecide.app.builtin;

import uecide.app.*;

public class cout implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        StringBuilder sb = new StringBuilder();

        for(String s : arg) {
            sb.append(s);
        }

        sketch.rawMessage(sb.toString());
        return true;
    }
}
