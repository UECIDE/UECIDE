package org.uecide.actions;

import org.uecide.*;
import java.util.TreeMap;

public class ActionsAction extends Action {

    public ActionsAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "Actions"
        };
    }

    public String getCommand() { return "actions"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        TreeMap<String, Class<? extends Action>> actions = Action.getActions();
        for (Class<? extends Action> cl : actions.values()) {
            try {
                Action a = Action.constructAction(cl, ctx);
                String[] usage = a.getUsage();
                for (String s : usage) {
                    ctx.message(s);
                }
            } catch (Exception ex) {
                Debug.exception(ex);
            }
        }
        return true;
    }
}
