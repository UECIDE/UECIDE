package org.uecide.gui.swing;

import org.uecide.Context;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ActionToolbarButton extends ToolbarButton implements ActionListener {
    Context ctx;
    String action;
    Object[] params;

    public ActionToolbarButton(Context c, String name, String icon, String act, Object... par) {
        super(name, icon, null);
        ctx = c;
        action = act;
        params = par;
        addActionListener(this);
        try {
            setIcon(IconManager.getIconFromContext(ctx, icon, 24));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent evt) {
        ctx.action(action, params);
    }
}
