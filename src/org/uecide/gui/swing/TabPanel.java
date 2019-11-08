package org.uecide.gui.swing;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.awt.BorderLayout;

public class TabPanel extends JPanel {

    String name;
    Component tabLabel;

    public TabPanel(String n) {
        super();
        name = n;
        setLayout(new BorderLayout());
        tabLabel = new JLabel(name);
    }

    public Component getTab() {
        return tabLabel;
    }
}
