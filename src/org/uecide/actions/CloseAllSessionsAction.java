package org.uecide.actions;

import org.uecide.*;
import java.io.File;
import java.util.ArrayList;

public class CloseAllSessionsAction extends Action {

    public CloseAllSessionsAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "CloseAllSessions"
        };
    }

    public String getCommand() { return "closeallsessions"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        ArrayList<Context> sessions = new ArrayList<Context>(Base.sessions);
        
        for (Context session : sessions) {
            session.action("CloseSession");
        }
        return true;
    }
}
