package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.Tool;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

public class ToolMenuItem extends JMenuItem implements ActionListener {

    Context ctx;
    Tool tool;
    String key;

    public ToolMenuItem(Context c, Tool t, String k) {
        super(t.get("tool." + k + ".name"));
        ctx = c;
        tool = t;
        key = k;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                tool.execute(ctx, "tool." + key + ".script");
            }
        });
        t.start();
    }
}
