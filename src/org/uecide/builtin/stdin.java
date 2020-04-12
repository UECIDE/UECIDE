package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

/* Execute a command and capture the output to a file
 *
 * Usage:
 *     __builtin_stdin::file::command[::arg...]
 */

public class stdin extends BuiltinCommand {
    public stdin(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        if (arg.length < 2) { // We need at least a file and a command
            return false;
        }


        File out = new File(arg[0]);
        String command = "";
        for (int i = 1; i < arg.length; i++) {
            if (!command.equals("")) {
                command += "::";
            }
            command += arg[i];
        }

        try {
            ctx.setOutputStream(new FileOutputStream(out));
        } catch (IOException ex) {
            Debug.exception(ex);
            throw new BuiltinCommandException(ex.getMessage());
        }
        if (!(Boolean)ctx.executeCommand(command, null)) {
            ctx.clearOutputStream();
            return false;
        }
        ctx.clearOutputStream();
        return true;
    }

    public void kill() {
    }
}
