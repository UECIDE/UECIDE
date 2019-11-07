package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.CommunicationPort;

import java.io.File;

import javax.swing.JRadioButtonMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PortsMenuItem extends JRadioButtonMenuItem implements ActionListener {

    Context ctx;
    CommunicationPort port;

    public PortsMenuItem(Context c, CommunicationPort p) {
        super(p.getName());
        ctx = c;
        port = p;
        if (port == ctx.getDevice()) {
            setSelected(true);
        } else {
            setSelected(false);
        }
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        ctx.action("setDevice", port);
    }
}
