package org.uecide.gui.swing;

import org.uecide.UECIDE;
import org.uecide.Context;
import org.uecide.CommunicationPort;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

public class PortsMenu extends JMenu implements MenuListener {

    Context ctx;

    public PortsMenu(Context c) {
        super("Port (None Selected)");
        ctx = c;
        addMenuListener(this);

        updatePort();
    }

    public void updatePort() {
        CommunicationPort port = ctx.getDevice();
        if (port == null) {
            setText("Port (None Selected)");
        } else {
            setText("Port (" + port.getName() + ")");
        }
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        ctx.action("updateSerialPorts");
        removeAll();
        for (CommunicationPort port : UECIDE.communicationPorts) {
            PortsMenuItem i = new PortsMenuItem(ctx, port);
            add(i);
        }
    }
    
}
