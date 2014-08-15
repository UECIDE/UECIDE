package uecide.app.builtin;

import uecide.app.*;

public abstract interface BuiltinCommand {
        public abstract boolean main(Sketch sktch, String[] arg);
}
