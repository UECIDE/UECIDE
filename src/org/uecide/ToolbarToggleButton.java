package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

public class ToolbarToggleButton extends ToolbarButton implements ActionListener {

    ImageIcon alternateIcon = null;
    ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener>();
    boolean selected = false;

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
            System.err.println("Getting alternate icon " + altname + " at size " + (size * 75 / 100) + " (" + size + ")");
            alternateIcon = IconManager.getIcon(size * 75 / 100, altname);
        }

        super.addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        selected = !selected;
        if (alternateIcon != null) {
            if (!selected) {
                setIcon(buttonIcon);
            } else {
                setIcon(alternateIcon);
            }
        }
        setBorderPainted(selected);
        for (ActionListener al : actionListeners) {
            al.actionPerformed(evt);
        }
    }

    @Override
    public void addActionListener(ActionListener l) {
        actionListeners.add(l);
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean s) {
        selected = s;
        if (alternateIcon != null) {
            if (!s) {
                setIcon(buttonIcon);
            } else {
                setIcon(alternateIcon);
            }
        }
        repaint();
    }
}
