package org.uecide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ActiveMenuItem extends JMenuItem {

    public ActiveMenuItem(String name, int shortcut, int mods, ActionListener action) {
        this(name, shortcut, mods, action, null);
    }

    public ActiveMenuItem(String name, int shortcut, int mods, ActionListener action, String command) {
        super(name);
        if (action != null) {
            addActionListener(action);
        }
        setActionCommand(command);
        if (shortcut != 0) {
            int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            setAccelerator(KeyStroke.getKeyStroke(shortcut, modifiers | mods));
        }
    }

}
