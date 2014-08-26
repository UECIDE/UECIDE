package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class cp implements BuiltinCommand {
    Sketch sketch;

    public boolean main(Sketch sktch, String[] arg) {
        sketch = sktch;

        if (arg.length < 2) {
            sketch.error("Usage: __builtin_cp::[file::[file::[file...]]]::[dest]");
            return false;
        }

        if (arg.length > 2) {
            return copy_many_to_one(arg);
        } else {
            return copy_one_to_one(arg);
        }
    }

    public boolean copy_one_to_one(String[] arg) {
        File from = new File(arg[0]);
        File to = new File(arg[1]);
        if (to.exists() && to.isDirectory()) {
            to = new File(to, from.getName());
        }

        Base.copyFile(from, to);
        return true;
    }

    public boolean copy_many_to_one(String[] arg) {
        File to = new File(arg[arg.length - 1]);
        if (!to.exists() || !to.isDirectory()) {
            return false;
        }
        for (int i = 0; i < arg.length - 2; i++) {
            String[] files = {
                arg[i],
                to.getAbsolutePath()
            };
            if (copy_one_to_one(files) == false) {
                return false;
            }
        }
        return true;
    }
}
