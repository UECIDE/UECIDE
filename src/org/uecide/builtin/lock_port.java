package org.uecide.builtin;

import org.uecide.*;

public class lock_port implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        if (!Serial.waitLock(arg[0])) {
            sketch.error("Timeout waiting for serial port lock");
            return false;
        }
        Serial.lockPort(arg[0]);
        return true;
    }
}
