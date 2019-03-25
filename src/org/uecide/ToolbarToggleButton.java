package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
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

    public ToolbarToggleButton(String name, String tooltip, int size) throws IOException {
        this(name, tooltip, size, null, null);
    }

    public ToolbarToggleButton(String name, String tooltip) throws IOException {
        this(name, tooltip, 24, null, null);
    }

    public ToolbarToggleButton(String name, String tooltip, ActionListener al) throws IOException {
        this(name, tooltip, 24, al, null);
    }

    public ToolbarToggleButton(String name, String tooltip, int size, ActionListener al) throws IOException {
        this(name, tooltip, 24, al, null);
    }

    public ToolbarToggleButton(String name, String tooltip, int size, ActionListener al, String altname) throws IOException {
        super();
        buttonIcon = IconManager.getIcon(size, name);
        if (altname != null) {
            alternateIcon = IconManager.getIcon(size, altname);
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
