package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;

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

    public ToolbarButton(String name, String tooltip, int size) throws IOException {
        this(name, tooltip, size, null);
    }

    public ToolbarButton(String name, String tooltip) throws IOException {
        this(name, tooltip, 24, null);
    }

    public ToolbarButton(String name, String tooltip, ActionListener al) throws IOException {
        this(name, tooltip, 24, al);
    }

    public ToolbarButton(String name, String tooltip, int size, ActionListener al) throws IOException {
        super();
        buttonIcon = IconManager.getIcon(size, name);
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
