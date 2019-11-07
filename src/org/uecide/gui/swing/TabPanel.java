package org.uecide.gui.swing;

import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.BorderLayout;

public class TabPanel extends JPanel {

    String name;

    public TabPanel(String n) {
        super();
        name = n;
        setLayout(new BorderLayout());
    }

    public Component getTab() {
        return new JLabel(name);
    }
}
