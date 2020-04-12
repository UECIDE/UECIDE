package org.uecide.actions;

import org.uecide.Context;
import org.uecide.UECIDE;

public class RefreshGuiAction extends Action {

    public RefreshGuiAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "RefreshGui"
        };
    }

    public String getCommand() { return "refreshgui"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        for (Context c : UECIDE.sessions) {
            c.getGui().refresh();
        }
        return true;
    }
}
