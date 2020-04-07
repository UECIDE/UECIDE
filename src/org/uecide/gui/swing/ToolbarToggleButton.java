package org.uecide.gui.swing;

import org.uecide.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

public class ToolbarToggleButton extends ToolbarButton implements ActionListener, AnimationListener {

    CleverIcon inactiveIcon;
    CleverIcon activeIcon;

    ArrayList<ActionListener> activateActionListeners = new ArrayList<ActionListener>();
    ArrayList<ActionListener> deactivateActionListeners = new ArrayList<ActionListener>();
    
    boolean selected = false;

    public ToolbarToggleButton(String tooltip, String inact, String act, ActionListener activate, ActionListener deactivate) throws IOException {
        super(tooltip);
        try {
            int buttonSize = Preferences.getInteger("theme.toolbar.iconsize", 24);
            inactiveIcon = IconManager.getIcon(buttonSize * 75 / 100, inact);
            activeIcon = IconManager.getIcon(buttonSize * 75 / 100, act);

            inactiveIcon.addAnimationListener(this);
            activeIcon.addAnimationListener(this);

            setIcon(inactiveIcon);

            activateActionListeners.add(activate);
            deactivateActionListeners.add(deactivate);
        } catch (IOException ex) {
            Debug.exception(ex);
        }

        super.addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        selected = !selected;
        if (!selected) {
            setIcon(inactiveIcon);
            for (ActionListener al : deactivateActionListeners) {
                al.actionPerformed(evt);
            }
        } else {
            setIcon(activeIcon);
            for (ActionListener al : activateActionListeners) {
                al.actionPerformed(evt);
            }
        }
        setBorderPainted(selected);
        repaint();
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean s) {
        selected = s;
        if (!s) {
            setIcon(inactiveIcon);
        } else {
            setIcon(activeIcon);
        }
        setBorderPainted(selected);
        repaint();
    }

    public void animationUpdated(CleverIcon i) {
        repaint();
    }
}
