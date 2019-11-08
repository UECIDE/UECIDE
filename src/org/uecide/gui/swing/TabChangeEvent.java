package org.uecide.gui.swing;

import java.util.EventObject;
import java.awt.Component;

public class TabChangeEvent extends EventObject {
    Component component;
    public TabChangeEvent(Object o, Component c) {
        super(o);
        component = c;
    }

    public Component getComponent() {
        return component;
    }
}
