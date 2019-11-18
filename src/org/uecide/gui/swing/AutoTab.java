package org.uecide.gui.swing;

import javax.swing.JTabbedPane;
import javax.swing.JFrame;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class AutoTab extends JTabbedPane implements MouseListener {
    ArrayList<TabChangeListener> listeners = new ArrayList<TabChangeListener>();
    ArrayList<TabMouseListener> mouseListeners = new ArrayList<TabMouseListener>();
    boolean separateWindow = false;
    JFrame parentWindow = null;
    public AutoTab() {
        super(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    TabPanel getPanelByTab(Component t) {
        for (int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if (c instanceof TabPanel) {
                Component l = ((TabPanel)c).getTab();
                if (t == l) return (TabPanel)c;
            }
        }
        return null;
    }

    public void setSeparateWindow(boolean w) {
        separateWindow = w;
    }

    public void setParentWindow(JFrame w) {
        parentWindow = w;
    }

    public boolean isSeparateWindow() {
        return separateWindow;
    }

    public JFrame getParentWindow() {
        return parentWindow;
    }

    @Override
    public Component add(Component c) {
        super.add(c);
        if (c instanceof TabPanel) {
            TabPanel p = (TabPanel)c;
            int i = indexOfComponent(c);
            setTabComponentAt(i, p.getTab());
            p.getTab().addMouseListener(this);
        }
        if (listeners != null) {
            for (TabChangeListener listener : listeners) {
                listener.tabAdded(new TabChangeEvent(this, c));
            }
        }
        return c;
    }

    @Override
    public void remove(Component c) {
        if (listeners != null) {
            for (TabChangeListener listener : listeners) {
                listener.tabRemoved(new TabChangeEvent(this, c));
            }
        }
        if (c instanceof TabPanel) {
            TabPanel p = (TabPanel)c;
            p.getTab().removeMouseListener(this);
        }
        super.remove(c);
    }

    public void addTabChangeListener(TabChangeListener l) {
        if (listeners == null) {
            listeners = new ArrayList<TabChangeListener>();
        }
        listeners.add(l);
    }

    public void removeTabChangeListener(TabChangeListener l) {
        if (listeners == null) {
            listeners = new ArrayList<TabChangeListener>();
        }
        listeners.remove(l);
    }

    public void addTabMouseListener(TabMouseListener l) {
        if (mouseListeners == null) {
            mouseListeners = new ArrayList<TabMouseListener>();
        }
        mouseListeners.add(l);
    }

    public void removeTabMouseListener(TabMouseListener l) {
        if (mouseListeners == null) {
            mouseListeners = new ArrayList<TabMouseListener>();
        }
        mouseListeners.remove(l);
    }
        

    public void mouseEntered(MouseEvent evt) {
        TabPanel p = getPanelByTab((Component)evt.getSource());
        if (p != null) {
            for (TabMouseListener listener : mouseListeners) {
                listener.mouseTabEntered(p, evt);
            }
        }
    }

    public void mouseExited(MouseEvent evt) {
        TabPanel p = getPanelByTab((Component)evt.getSource());
        if (p != null) {
            for (TabMouseListener listener : mouseListeners) {
                listener.mouseTabExited(p, evt);
            }
        }
    }
    public void mousePressed(MouseEvent evt) {
        TabPanel p = getPanelByTab((Component)evt.getSource());
        if (p != null) {
            for (TabMouseListener listener : mouseListeners) {
                listener.mouseTabPressed(p, evt);
            }
        }
    }
    public void mouseReleased(MouseEvent evt) {
        TabPanel p = getPanelByTab((Component)evt.getSource());
        if (p != null) {
            for (TabMouseListener listener : mouseListeners) {
                listener.mouseTabReleased(p, evt);
            }
        }
    }
    public void mouseClicked(MouseEvent evt) {
        TabPanel p = getPanelByTab((Component)evt.getSource());
        if (p != null) {
            for (TabMouseListener listener : mouseListeners) {
                listener.mouseTabClicked(p, evt);
            }
        }
    }
}
