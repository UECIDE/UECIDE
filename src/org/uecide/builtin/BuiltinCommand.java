package org.uecide.builtin;

import org.uecide.*;

public abstract interface BuiltinCommand {
        public abstract boolean main(Context ctx, String[] arg);
}
