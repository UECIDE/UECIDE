package org.uecide;

import javax.swing.*;

public class JMenuItemWithObject extends JMenuItem {
    Object object;

    public JMenuItemWithObject(String text, Object o) {
        super(text);
        object = o;
    }

    public Object getObject() {
        return object;
    }
}
