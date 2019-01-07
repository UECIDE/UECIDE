package org.uecide;

import javax.swing.*;

public class JSTab extends JPanel {
    JSPlugin plugin;

    JSTab(JSPlugin p) {
        plugin = p;
    }

    public JSPlugin getPlugin() {
        return plugin;
    }

}
