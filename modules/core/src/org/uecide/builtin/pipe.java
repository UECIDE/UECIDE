package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

/* Pipe a variable through a command capturing the output into another variable.
 *
 * Usage:
 *     __builtin_pipe::key.input::key.output::commmand::[arg...]
 */

public class pipe extends BuiltinCommand {
    public pipe(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        if (arg.length < 3) { // We need a command and two variables
            return false;
        }


        String input = arg[0];
        String output = arg[1];
        String command = "";
        for (int i = 2; i < arg.length; i++) {
            if (!command.equals("")) {
                command += "::";
            }
            command += arg[i];
        }

        ctx.setOutputStream(new VariableOutputStream(ctx, output));
        ctx.setInputStream(new VariableInputStream(ctx, input));

        if (!(Boolean)ctx.executeCommand(command, null)) {
            ctx.clearOutputStream();
            ctx.clearInputStream();
            return false;
        }
        ctx.clearOutputStream();
        ctx.clearInputStream();
        return true;
    }

    public void kill() {
    }
}
