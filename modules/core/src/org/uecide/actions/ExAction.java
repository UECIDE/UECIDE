package org.uecide.actions;

import org.uecide.Context;

public class ExAction extends Action {

    public ExAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "Ex <command>"
        };
    }

    public String getCommand() { return "ex"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        String command = null;
        for (Object s : args) {
            if (!(s instanceof String)) {
                throw new BadArgumentActionException();
            }
            if (command == null) {
                command = (String)s;
            } else {
                command = command + "::" + (String)s;
            }
        }
        return (Boolean)ctx.executeCommand(command, null);
    }
}
