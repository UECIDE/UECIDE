package org.uecide.builtin;

import org.uecide.*;

public class spin extends BuiltinCommand {
    public spin(Context c) { super(c); }

    static int spinpos = 0;
    public boolean main(String[] arg) throws BuiltinCommandException {
        String symbol = "";

        switch(spinpos) {
            case 0: symbol = "|"; break;
            case 1: symbol = "/"; break;
            case 2: symbol = "-"; break;
            case 3: symbol = "\\"; break;
        }

        spinpos++;
        if (spinpos == 4) {
            spinpos = 0;
        }
    
        ctx.messageStream("\010" + symbol);
        return true;
    }

    public void kill() {
    }
}
