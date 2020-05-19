package org.uecide.gui.swing;

import java.awt.event.MouseEvent;

public interface TabMouseListener {
    public abstract void mouseTabEntered(TabPanel tab, MouseEvent e);
    public abstract void mouseTabExited(TabPanel tab, MouseEvent e);
    public abstract void mouseTabPressed(TabPanel tab, MouseEvent e);
    public abstract void mouseTabReleased(TabPanel tab, MouseEvent e);
    public abstract void mouseTabClicked(TabPanel tab, MouseEvent e);
}
