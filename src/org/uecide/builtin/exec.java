package org.uecide.builtin;

import org.uecide.*;

public class exec extends BuiltinCommand {
    public exec(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if(arg.length != 1) {
            throw new BuiltinCommandException("Syntax Error");
        }

        String key = arg[0];

        return (Boolean)ctx.executeKey(key);

    }

    public void kill() {
    }
}
