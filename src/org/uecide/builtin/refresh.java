package org.uecide.builtin;

import org.uecide.Base;
import org.uecide.Context;
import java.io.IOException;

class refresh extends BuiltinCommand {
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        try {
            Base.cleanAndScanAllSettings();
            return true;
        } catch (IOException ex) {
            throw new BuiltinCommandException(ex.getMessage());
        }
    }

    public void kill() {
    }
}
