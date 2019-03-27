package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.awt.event.*;

public class ToolbarToggleButton extends ToolbarButton {

    ImageIcon buttonIcon;
    ImageIcon alternateIcon;
    int size;

    public ToolbarToggleButton(ImageIcon ico) {
        this(ico, null);
    }

    public ToolbarToggleButton(ImageIcon ico, ActionListener al) {
        super(ico, al);
    }

    public ToolbarToggleButton(String name, String tooltip, int s) throws IOException {
        this(name, tooltip, s, null, null);
    }

    public ToolbarToggleButton(String name, String tooltip) throws IOException {
        this(name, tooltip, 24, null, null);
    }

    public ToolbarToggleButton(String name, String tooltip, ActionListener al) throws IOException {
        this(name, tooltip, 24, al, null);
    }

    public ToolbarToggleButton(String name, String tooltip, int s, ActionListener al) throws IOException {
        this(name, tooltip, s, al, null);
    }

    public ToolbarToggleButton(String name, String tooltip, int s, ActionListener al, String altname) throws IOException {
        super(name, tooltip, s, al);
        if (altname != null) {
            alternateIcon = IconManager.getIcon(size * 75 / 100, altname);
        }
    }

    public void setAlternateIcon(boolean a) {
        if (a) {
            setIcon(alternateIcon);
        } else {
            setIcon(buttonIcon);
        }
    }
}
