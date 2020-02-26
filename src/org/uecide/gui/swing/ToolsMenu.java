package org.uecide.gui.swing;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.PropertyFile;
import org.uecide.Tool;

public class ToolsMenu extends JMenu implements MenuListener {
    
    Context ctx;

    public ToolsMenu(Context c) {
        super("Tools");
        ctx = c;
        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();

        JMenuItem pluginManagerMenu = new JMenuItem("Plugin Manager (TODO)");
        add(pluginManagerMenu);

        addSeparator();

        for (Tool t : Base.tools.values()) {
            if (t.worksWith(ctx.getCore())) {

                PropertyFile pf = t.getProperties();
                
                PropertyFile tlist = pf.getChildren("tool");
                String[] toolNames = tlist.childKeys();

                for (String toolName : toolNames) {
                    ToolMenuItem m = new ToolMenuItem(ctx, t, toolName);
                    add(m);
                }
            }
        }
    }
}
