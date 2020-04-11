package org.uecide.builtin;

import org.uecide.*;

public class delay extends BuiltinCommand {
    public delay(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        try {
            Thread.sleep(Integer.parseInt(arg[0]));
        } catch (Exception e) {
            Debug.exception(e);
        }
        return true;
    }

    public void kill() {
    }
}
