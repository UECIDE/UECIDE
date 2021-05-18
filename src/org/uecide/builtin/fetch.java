package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

import org.uecide.editors.EditorBase;

/* Fetch the text from the active editor into a variable
 * 
 * Usage:
 *     __builtin_fetch::key.name
 */

public class fetch extends BuiltinCommand {

    public boolean main(Context ctx, String[] arg) throws BuiltinCommandException {

        if (arg.length == 0) { // We need a variable
            return false;
        }

        Editor ed = ctx.getEditor();
        if (ed == null) {
            return false;
        }

        int tab = ed.getActiveTab();
        EditorBase eb = ed.getTab(tab);

        String txt = eb.getSelectedText();

        if (txt == null) {
            txt = eb.getText();
        }

        ctx.set(arg[0], txt);
        return true;
    }   

    public void kill() {
    }   
}

