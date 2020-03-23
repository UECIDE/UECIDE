package org.uecide.builtin;

import org.uecide.Context;

public class action extends BuiltinCommand {
    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
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
