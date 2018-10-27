package org.uecide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.plaf.basic.*;

public class FixedSplitPane extends JSplitPane implements ComponentListener, MouseListener {

    public static final int TOP = 0;
    public static final int LEFT = 0;
    public static final int BOTTOM = 1;
    public static final int RIGHT = 1;

    int splitSize = -1; // The left or top split size (or -1 for auto)

    int anchor = TOP;

    Component panelOne;
    Component panelTwo;

    int orientation;

    String propertyKey = "";

    public FixedSplitPane(int o, Component one, Component two, String prop, int anch) {
        super(o, one, two);
        orientation = o;
        panelOne = one;
        panelTwo = two;
        panelOne.setMinimumSize(new Dimension(0, 0));
        panelTwo.setMinimumSize(new Dimension(0, 0));
        propertyKey = prop;
        anchor = anch;
        splitSize = Preferences.getInteger(propertyKey);
        ((BasicSplitPaneUI)getUI()).getDivider().addMouseListener(this);
        addComponentListener(this);
        setContinuousLayout(true);
    }

    public void setSplitSize(int size) {
        if (!Preferences.getBoolean(propertyKey + "_lock")) {
            splitSize = size;
        }
        recalculateSplit();
    }

    public void hideOne() {
        panelOne.setVisible(false);
    }

    public void hideTwo() {
        panelTwo.setVisible(false);
    }

    public void showOne() {
        panelOne.setVisible(true);
    }

    public void showTwo() {
        panelTwo.setVisible(true);
    }

    public void recalculateSplit() {
        if (orientation == JSplitPane.VERTICAL_SPLIT) {
            if (anchor == TOP) {
                setDividerLocation(splitSize);
            } else {
                Dimension size = getSize();
                setDividerLocation(size.height - splitSize);
            }
        } else {
            if (anchor == LEFT) {
                setDividerLocation(splitSize);
            } else {
                Dimension size = getSize();
                setDividerLocation(size.width - splitSize);
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        if (Preferences.getBoolean(propertyKey + "_lock")) {
            recalculateSplit();
        } else {
            if (orientation == JSplitPane.VERTICAL_SPLIT) {
                if (anchor == TOP) {
                    splitSize = getDividerLocation();
                } else {
                    Dimension size = getSize();
                    splitSize = size.height - getDividerLocation();
                }
            } else {
                if (anchor == LEFT) {
                    splitSize = getDividerLocation();
                } else {
                    Dimension size = getSize();
                    splitSize = size.width - getDividerLocation();
                }
            }
            Preferences.setInteger(propertyKey, splitSize);
        }
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        recalculateSplit();
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }
}
