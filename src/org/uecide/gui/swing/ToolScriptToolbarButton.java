package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Debug;
import org.uecide.Tool;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToolScriptToolbarButton extends ToolbarButton implements ActionListener {
    Context ctx;
    Tool tool;
    String script;
    Object[] params;

    public ToolScriptToolbarButton(Context c, Tool t, String name, String icon, String scr) {
        super(name, icon, null);
        ctx = c;
        tool = t;
        script = scr;
        try {
            setIcon(IconManager.getIconFromTool(ctx, tool, icon, 24));
        } catch (Exception ex) {
            Debug.exception(ex);
            ex.printStackTrace();
        }
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                tool.execute(ctx, "tool." + script + ".script");
            }
        });
        t.start();
    }
}
