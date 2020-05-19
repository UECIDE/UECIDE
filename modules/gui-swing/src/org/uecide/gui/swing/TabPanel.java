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
    AutoTab defaultPanel;

    public TabPanel(String n, AutoTab p) {
        super();
        name = n;
        defaultPanel = p;
        setLayout(new BorderLayout());
        tabLabel = new JLabel(name);
        ((JLabel)tabLabel).setOpaque(false);
    }

    public Component getTab() {
        return tabLabel;
    }

    public void reset() {
        defaultPanel.add(this);
    }

    public void refreshPanel() {
    }
}
