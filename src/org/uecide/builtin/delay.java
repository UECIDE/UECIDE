package org.uecide.builtin;

import org.uecide.*;

public class delay implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        try {
            Thread.sleep(Integer.parseInt(arg[0]));
        } catch (Exception e) {
        }
        return true;
    }
}
