package org.uecide.builtin;

import org.uecide.Context;

/* Run an action.
 * 
 * Usage:
 *     __builtin_action::actionName::arg::arg::arg...
 */

public class action extends BuiltinCommand {
    
    public action(Context c) { super(c); }

    public boolean main( String[] arg) throws BuiltinCommandException {
        String name = arg[0];
        if (arg.length == 1) {
            return ctx.action(name);
        } else {
            String args[] = new String[arg.length - 1];
            for (int i = 0; i < arg.length - 1; i++) {
                args[i] = arg[i+1];
            }
            return ctx.action(name, (Object[]) args);
        }
    }

    public void kill() {
    }
}
