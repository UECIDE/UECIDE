package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

// Pass the current editor's text through the stdin of a command and replace it with the stdout
public class filter extends BuiltinCommand {
    public filter(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        if (arg.length == 0) { // We need a command
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
