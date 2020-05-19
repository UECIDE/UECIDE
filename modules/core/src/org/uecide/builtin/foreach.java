package org.uecide.builtin;

import org.uecide.*;

/* For each item in a list assign it to a variable and run a key
 *
 * Usage:
 *     __builtin_foreach::item,item,item,item::key.assign::key.run
 */

public class foreach extends BuiltinCommand {
    public foreach(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length != 3) {
            throw new BuiltinCommandException("Syntax Error");
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

    public void kill() {
    }
}
