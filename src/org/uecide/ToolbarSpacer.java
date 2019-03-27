package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class ToolbarSpacer extends JPanel {

    int gap = 16;
    int height = 24;
    
    public ToolbarSpacer() {
        this(16, 24);
        setEnabled(false);
//        setRolloverEnabled(false);
//        setBorderPainted(false);
//        setFocusable(false);
        setOpaque(false);
    }

    public ToolbarSpacer(int s) {
        this(s, 24);
    }

    public ToolbarSpacer(int s, int h) {
        super();
        gap = s;
        height = h;
    }

    public boolean isBorderPainted() { return false; }
    public boolean isFocusPainted() { return false; }
    @Override
    public boolean isFocusable() { return false; }
    public boolean isRolloverEnabled() { return false; }
    @Override
    public Insets getInsets() { return new Insets(height + 4, gap, 0, 0); }
    @Override
    public Dimension getSize() {
        return new Dimension(gap, height);
    }
    @Override
    public Dimension getPreferredSize() { return getSize(); }
    @Override
    public Dimension getMinimumSize() { return getSize(); }
    @Override
    public Dimension getMaximumSize() { return getSize(); }
}
