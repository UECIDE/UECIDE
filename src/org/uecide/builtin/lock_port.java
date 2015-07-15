package org.uecide.builtin;

import org.uecide.*;

public class lock_port implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        if (!Serial.waitLock(arg[0])) {
            ctx.error("Timeout waiting for serial port lock");
            return false;
        }
        Serial.lockPort(arg[0]);
        return true;
    }
}
