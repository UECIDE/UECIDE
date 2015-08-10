package org.uecide.builtin;

import org.uecide.*;

public class unlock_port implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        Serial.unlockPort(arg[0]);
        return true;
    }
}
