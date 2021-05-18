package org.uecide.builtin;

import org.uecide.Context;

import org.uecide.Base;
import org.uecide.Editor;
import javax.swing.JOptionPane;

/* Ask a "Yes / No" question. Jumps to one of two keys.
 * 
 * Usage:
 *     __builtin_askyesno::question::key.yes::key.no
 */

public class askyesno extends BuiltinCommand {
    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        if (arg.length != 3) {
            throw new BuiltinCommandException("Syntax error");
        }

        String question = arg[0];
        String yesKey = arg[1];
        String noKey = arg[2];

        Editor ed = ctx.getEditor();

        if (ed != null) {
            if (ed.twoOptionBox(JOptionPane.QUESTION_MESSAGE, "Excuse me, but...", question,
                                 Base.i18n.string("misc.yes"), Base.i18n.string("misc.no")
            ) == 0) {
                ctx.executeKey(yesKey);
            } else {
                ctx.executeKey(noKey);
            }
        }
        return true;
    }

    public void kill() {
    }
}

