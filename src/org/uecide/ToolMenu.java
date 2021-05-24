package org.uecide;

import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToolMenu extends JMenuItem implements ActionListener {

    Tool tool;
    String type;
    String script;
    Context ctx = null;

    public ToolMenu(Tool t, PropertyFile pf) {
        super(pf.get("name"));
        tool = t;
        type = pf.get("type");
        if (type.equals("script")) {
            script = pf.get("script");
        }
        addActionListener(this);
    }

    public void setContext(Context c) {
        ctx = c;
    }

    public void actionPerformed(ActionEvent evt) {
        if (type.equals("script")) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    tool.execute(ctx, script);
                }
            });
            t.start();
        }
    }
   
}
