package org.uecide.builtin;

import org.uecide.*;
import org.uecide.gui.Gui;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

/* Request authentication. Assigns to a key
 * 
 * Usage: 
 *     __builtin_getauth::prompt::key.name
 */

public class getauth extends BuiltinCommand {
    public getauth(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        String prompt = arg[0];
        String dest = arg[1];

        String value = "";
        String exist = Preferences.get(dest);

        Gui gui = ctx.getGui();

        value = gui.askPassword(prompt, exist);
        if (value == null) return false;
        ctx.set(dest, value);
        Preferences.set(dest, value);
        return true;
    }

    public void kill() {
    }
}
