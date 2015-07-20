package org.uecide.builtin;

import org.uecide.*;

public class foreach implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {
        if (arg.length != 3) {
            for (String a : arg) {
                System.err.print("foreach: " + a);
            }
            ctx.error("Usage: __builtin_foreach::item,item,item...::variable::script");
            return false;
        }

        String[] items = arg[0].split(",");
        String variable = arg[1];
        String target = arg[2];

        for (String item : items) {
            item = item.trim();
            ctx.set(variable, item);
            if(!(Boolean)ctx.executeKey(target)) {
                return false;
            }
        }
        return true;
    }
}
