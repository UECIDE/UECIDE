package org.uecide.builtin;

import org.uecide.Base;
import org.uecide.Context;

import java.io.File;

public class deltree extends BuiltinCommand {
    public boolean main(Context ctx, String[] args) throws BuiltinCommandException {
        for (String d : args) {
            File dir = new File(d);
            if (dir.exists()) {
                if (dir.isDirectory()) {
                    Base.removeDir(dir);
                } else {
                    dir.delete();
                }
            }
        }
        return true;
    }

    public void kill() {
    }
}
