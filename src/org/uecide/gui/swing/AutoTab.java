package org.uecide.gui.swing;

import javax.swing.JTabbedPane;
import java.awt.Component;

public class AutoTab extends JTabbedPane {
    public AutoTab() {
        super();
    }

    
    public Component add(Component c) {
        super.add(c);
        if (c instanceof TabPanel) {
            TabPanel p = (TabPanel)c;
            int i = indexOfComponent(c);
            setTabComponentAt(i, p.getTab());
        }
        return c;
    }
}
