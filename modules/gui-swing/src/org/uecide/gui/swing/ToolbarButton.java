package org.uecide.gui.swing;

import org.uecide.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;

public class ToolbarButton extends JButton implements AnimationListener {

    CleverIcon buttonIcon;

    int size;

    public ToolbarButton(String tooltip) {
        super();
        setToolTipText(tooltip);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        size = Preferences.getInteger("theme.toolbar.iconsize", 24);
    }

    public ToolbarButton(String tooltip, String icon, ActionListener action) {
        super();
        setToolTipText(tooltip);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        size = Preferences.getInteger("theme.toolbar.iconsize", 24);

        try {
            int buttonSize = Preferences.getInteger("theme.toolbar.iconsize", 24);
            buttonIcon = IconManager.getIcon(buttonSize * 75 / 100, icon);
            buttonIcon.addAnimationListener(this);
            setIcon(buttonIcon);
        } catch (IOException ex) {
            Debug.exception(ex);
        }

        super.addActionListener(action);

        
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

    public void animationUpdated(CleverIcon i) {
        repaint();
    }

}
