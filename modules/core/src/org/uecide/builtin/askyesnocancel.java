package org.uecide.builtin;

import org.uecide.Context;

/* Ask a "Yes / No / Cancel" question. Jumps to one of three keys.
 *
 * Usage:
 *     __builtin_askyesnocancel::question::key.yes::key.no::key.cancel
 */

public class askyesnocancel extends BuiltinCommand {

    public askyesnocancel(Context c) { super(c); }

    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length != 4) {
            throw new BuiltinCommandException("Syntax error");
        }

        String question = arg[0];
        String yesKey = arg[1];
        String noKey = arg[2];
        String cancelKey = arg[3];

        int answer = ctx.getGui().askYesNoCancel(question);
        switch (answer) {
            case 0: ctx.action("runKey", yesKey); break;
            case 1: ctx.action("runKey", noKey); break;
            case 2: ctx.action("runKey", cancelKey); break;
        }
        return true;
    }

    public void kill() {
    }
}
