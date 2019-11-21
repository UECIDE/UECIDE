package org.uecide.actions;

import org.uecide.Context;
import org.uecide.Preferences;

import java.awt.Color;
import java.awt.Font;

/* abort <action>
 *
 * Aborts an action that was started with Context.actionThread()
 */

public class SetPrefAction extends Action {

    public SetPrefAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "SetPref <key> <value>"
        };
    }

    public boolean actionPerformed(Object[] args) throws ActionException {

        if (args.length != 2) throw new SyntaxErrorActionException();
        if (!(args[0] instanceof String)) throw new BadArgumentActionException();

        String key = (String)args[0];
   
        if (args[1] instanceof String) {
            String value = (String)args[1];
            Preferences.set(key, value);
            return true;
        }

        if (args[1] instanceof Integer) {
            Integer value = (Integer)args[1];
            Preferences.setInteger(key, value);
            return true;
        }

        if (args[1] instanceof Color) {
            Color value = (Color)args[1];
            Preferences.setColor(key, value);
            return true;
        }

        if (args[1] instanceof Font) {
            Font value = (Font)args[1];
            Preferences.setFont(key, value);
            return true;
        }

        if (args[1] instanceof Boolean) {
            Boolean value = (Boolean)args[1];
            Preferences.setBoolean(key, value);
            return true;
        }

        throw new BadArgumentActionException();
    }
}
