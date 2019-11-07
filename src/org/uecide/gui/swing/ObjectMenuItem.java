package org.uecide.gui.swing;

import javax.swing.JMenuItem;

public class ObjectMenuItem extends JMenuItem {

    Object object;

    public ObjectMenuItem(String t, Object o) {
        super(t);
        object = o;
    }

    public ObjectMenuItem(String t) {
        super(t);
        object = null;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object o) {
        object = o;
    }
}
       
