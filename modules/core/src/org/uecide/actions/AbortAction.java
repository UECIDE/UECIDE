package org.uecide.actions;

import org.uecide.*;

/* abort <action>
 *
 * Aborts an action that was started with Context.actionThread()
 */

public class AbortAction extends Action {

    public AbortAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "Abort <action>"
        };
    }

    public String getCommand() { return "abort"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        if (args.length != 1) {
            throw new SyntaxErrorActionException();
        }
        if (!(args[0] instanceof String)) {
            throw new BadArgumentActionException();
        }

        ctx.killRunningProcess();

        return ctx.killThread((String)args[0]);
    }
}
