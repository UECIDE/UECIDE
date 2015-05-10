package org.uecide.builtin;

import org.uecide.*;

public class unlock_port implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        Serial.unlockPort(arg[0]);
        return true;
    }
}
