package org.uecide;

import org.uecide.PropertyFile;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.File;
//import javax.swing.ImageIcon;

public class ToolIcon extends ToolbarButton implements ActionListener {
    public static final int TOOLBAR = 0;
    public static final int EDITOR = 1;

    public static final int SCRIPT = 0;

    public Tool tool;
    public String key;
    public String name;
    public int type;
    public String script;
    public String icon;
    public Context ctx = null;
    public CleverIcon imgIcon = null;
    
    public ToolIcon(Tool t, String k, PropertyFile data, int s) throws IOException {
        super(data.get("icon"), data.get("name"), s);
        addActionListener(this);
        tool = t;
        key = k;

        name = data.get("name");
        icon = data.get("icon");

        try {
            imgIcon = new CleverIcon(s, new File(t.getFolder(), icon));
            setIcon(imgIcon);
        } catch (Exception ex) {
            Base.error(ex.getMessage());
        }

        switch (data.get("type")) {
            case "script": 
                type = SCRIPT; 
                script = data.get("script");
                break;
        }
    }

    public void setContext(Context c) {
        ctx = c;
    }

    public void actionPerformed(ActionEvent evt) {
        if (type == SCRIPT) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    tool.execute(ctx, script);
                }
            });
            t.start();
        }
    }
}
