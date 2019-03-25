package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class ToolbarToggleButton extends JToggleButton {

    ImageIcon buttonIcon;
    ImageIcon alternateIcon;

    public ToolbarToggleButton(ImageIcon ico) {
        this(ico, null);
    }

    public ToolbarToggleButton(ImageIcon ico, ActionListener al) {
        super();
        buttonIcon = ico;
        setIcon(ico);
    //    setToolTipText(null);
        if (al != null) {
            addActionListener(al);
        }
    }

    public ToolbarToggleButton(String cat, String name, String tooltip, int size) {
        this(cat, name, tooltip, size, null, null, null);
    }

    public ToolbarToggleButton(String cat, String name, String tooltip) {
        this(cat, name, tooltip, 24, null, null, null);
    }

    public ToolbarToggleButton(String cat, String name, String tooltip, ActionListener al) {
        this(cat, name, tooltip, 24, al, null, null);
    }

    public ToolbarToggleButton(String cat, String name, String tooltip, int size, ActionListener al) {
        this(cat, name, tooltip, 24, al, null, null);
    }

    public ToolbarToggleButton(String cat, String name, String tooltip, int size, ActionListener al, String altcat, String altname) {
        super();
        buttonIcon = Base.getIcon(cat, name, size);
        if (altcat != null) {
            alternateIcon = Base.getIcon(altcat, altname, size);
        }
        setIcon(buttonIcon);
        setToolTipText(tooltip);
        if (al != null) {
            addActionListener(al);
        }
    }

    public boolean isBorderPainted() { return false; }
    public boolean isFocusPainted() { return false; }
    public Insets getInsets() { return new Insets(2, 2, 2, 2); }
    public void setAlternateIcon(boolean a) {
        if (a) {
            setIcon(alternateIcon);
        } else {
            setIcon(buttonIcon);
        }
    }
}
