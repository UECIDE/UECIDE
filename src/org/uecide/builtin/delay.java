package org.uecide.builtin;

import org.uecide.*;

public class delay implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        try {
            Thread.sleep(Integer.parseInt(arg[0]));
        } catch (Exception e) {
        }
        return true;
    }
}
