package org.uecide.builtin;

import org.uecide.Context;

import org.uecide.Base;
import org.uecide.Editor;
import javax.swing.JOptionPane;

/* Ask a "Yes / No" question. Jumps to one of two keys.
 * 
 * Usage:
 *     __builtin_askyesnocancel::question::key.yes::key.no::key.cancel
 */

public class askyesnocancel extends BuiltinCommand {
    // __builtin_askyesno::Do you want to do this?::script.yes::script.no
    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {
        if (arg.length != 4) {
            throw new BuiltinCommandException("Syntax error");
        }

        String question = arg[0];
        String yesKey = arg[1];
        String noKey = arg[2];
        String cancelKey = arg[3];

        Editor ed = ctx.getEditor();

        if (ed != null) {
            int res = ed.threeOptionBox(JOptionPane.QUESTION_MESSAGE, "Excuse me, but...", question,
                                 Base.i18n.string("misc.yes"), Base.i18n.string("misc.no"), Base.i18n.string("misc.cancel"));
            switch (res) {
                case 0: ctx.executeKey(yesKey); break;
                case 1: ctx.executeKey(noKey); break;
                case 2: ctx.executeKey(cancelKey); break;
            }
        }
        return true;
    }

    public void kill() {
    }
}

