package org.uecide.builtin;

import org.uecide.Context;
import org.uecide.Sketch;

import java.io.File;
import java.io.IOException;

public class save extends BuiltinCommand {
    public boolean main(Context ctx, String[] args) throws BuiltinCommandException {

        Sketch s = ctx.getSketch();

        if (args.length == 0) {
            try {
                return s.save();
            } catch (IOException ex) {
                throw new BuiltinCommandException(ex.toString());
            }
        } else {
            try {
                return s.saveAs(new File(args[0]));
            } catch (IOException ex) {
                throw new BuiltinCommandException(ex.toString());
            }
        }
    }

    public void kill() {}
}
