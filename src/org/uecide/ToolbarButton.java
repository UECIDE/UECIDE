package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;

public class ToolbarButton extends JButton {

    ImageIcon buttonIcon;

    int size;

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

        size = ico.getIconWidth() * 125 / 100;

        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    public ToolbarButton(String name, String tooltip, int s) throws IOException {
        this(name, tooltip, s, null);
    }

    public ToolbarButton(String name, String tooltip) throws IOException {
        this(name, tooltip, 24, null);
    }

    public ToolbarButton(String name, String tooltip, ActionListener al) throws IOException {
        this(name, tooltip, 24, al);
    }

    public ToolbarButton(String name, String tooltip, int s, ActionListener al) throws IOException {
        super();
        size = s;
        buttonIcon = IconManager.getIcon(size * 75 / 100, name);
        setIcon(buttonIcon);
        setToolTipText(tooltip);
        if (al != null) {
            addActionListener(al);
        }
    }

    @Override
    public boolean isBorderPainted() { return false; }
    @Override
    public boolean isFocusPainted() { return false; }
    @Override
    public Insets getInsets() { return new Insets(2, 2, 2, 2); }

    @Override
    public Dimension getSize() {
        return new Dimension(size * 15 / 10, size);
    }

    @Override
    public Dimension getPreferredSize() { return getSize(); }
    @Override
    public Dimension getMinimumSize() { return getSize(); }
    @Override
    public Dimension getMaximumSize() { return getSize(); }

}
