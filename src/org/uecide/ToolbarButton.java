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
        this(ico, ico.getIconWidth() * 133 / 100, al);
    }
    public ToolbarButton(ImageIcon ico, int s, ActionListener al) {
        super();
        buttonIcon = ico;
        setIcon(ico);
        setDisabledIcon(ico);
        if (al != null) {
            super.addActionListener(al);
        }

        size = s;

        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    public ToolbarButton(String name, String tooltip, int s) throws IOException {
        this(name, tooltip, s, null);
    }

    public ToolbarButton(String name, String tooltip) throws IOException {
        this(name, tooltip, Preferences.getInteger("theme.iconsize"), null);
    }

    public ToolbarButton(String name, String tooltip, ActionListener al) throws IOException {
        this(name, tooltip, Preferences.getInteger("theme.iconsize"), al);
    }

    public ToolbarButton(String name, String tooltip, int s, ActionListener al) throws IOException {
        super();
        size = s;
        CleverIcon icon = IconManager.getIcon(size * 75 / 100, name);
        buttonIcon = icon;
        setIcon(icon);
        setDisabledIcon(icon.disabled());
        setToolTipText(tooltip);
        if (al != null) {
            super.addActionListener(al);
        }
    }

    @Override
    public boolean isFocusPainted() { return false; }
    @Override
    public Insets getInsets() { return new Insets(2, 2, 2, 2); }

    @Override
    public Dimension getSize() {
        Integer scale = UIManager.getInt("Button.scale");
        if (scale == null) {
            scale = new Integer(125);
        }
        if (scale < 1) {
            scale = new Integer(125);
        }
        Integer scaled = size * scale / 100;
        return new Dimension(scaled, scaled);
    }

    @Override
    public Dimension getPreferredSize() { return getSize(); }
    @Override
    public Dimension getMinimumSize() { return getSize(); }
    @Override
    public Dimension getMaximumSize() { return getSize(); }

}
