package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class ToolbarButton extends JButton {

    ImageIcon buttonIcon;

    public ToolbarButton(ImageIcon ico) {
        this(ico, null);
    }

    public ToolbarButton(ImageIcon ico, ActionListener al) {
        super();
        buttonIcon = ico;
        setIcon(ico);
    //    setToolTipText(null);
        if (al != null) {
            addActionListener(al);
        }
    }

    public ToolbarButton(String cat, String name, String tooltip, int size) {
        this(cat, name, tooltip, size, null);
    }

    public ToolbarButton(String cat, String name, String tooltip) {
        this(cat, name, tooltip, 24, null);
    }

    public ToolbarButton(String cat, String name, String tooltip, ActionListener al) {
        this(cat, name, tooltip, 24, al);
    }

    public ToolbarButton(String cat, String name, String tooltip, int size, ActionListener al) {
        super();
        buttonIcon = Base.getIcon(cat, name, size);
        setIcon(buttonIcon);
        setToolTipText(tooltip);
        if (al != null) {
            addActionListener(al);
        }
    }

    public boolean isBorderPainted() { return false; }
    public boolean isFocusPainted() { return false; }
    public Insets getInsets() { return new Insets(2, 2, 2, 2); }
}
