package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class ToolbarSpacer extends JButton {

    int gap = 16;
    int height = 24;
    
    public ToolbarSpacer() {
        this(16, 24);
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
    public boolean isFocusable() { return false; }
    public boolean isRolloverEnabled() { return false; }
    public Insets getInsets() { return new Insets(height + 4, gap, 0, 0); }
}
