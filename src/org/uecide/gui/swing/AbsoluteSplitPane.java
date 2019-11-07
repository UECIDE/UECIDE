package org.uecide.gui.swing;

import javax.swing.JSplitPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.SplitPaneUI;

public class AbsoluteSplitPane extends JSplitPane implements ComponentListener, MouseListener {

    int left = -1; // and top
    int right = -1; // and bottom
    int orient;

    public AbsoluteSplitPane(int orientation, Component a, Component b) {
        super(orientation, a, b);
        orient = orientation;
        addComponentListener(this);
        ((BasicSplitPaneUI)getUI()).getDivider().addMouseListener(this);
    }

    void updateDividerLocation() {
        if (right == -1) {
            setDividerLocation(left);
        } else {
            Dimension d = getSize();
            if (orient == JSplitPane.VERTICAL_SPLIT) {
                setDividerLocation(d.height - right);
            } else {
                setDividerLocation(d.width - right);
            }
        }
    }

    public void setLeftSize(int s) {
        left = s;
        right = -1;
        updateDividerLocation();
    }

    public void setRightSize(int s) {
        left = -1;
        right = s;
        updateDividerLocation();
    }

    public void setTopSize(int s) {
        left = s;
        right = -1;
        updateDividerLocation();
    }

    public void setBottomSize(int s) {
        left = -1;
        right = s;
        updateDividerLocation();
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        updateDividerLocation();
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        int pos = getDividerLocation();

        if (right == -1) {
            left = pos;
        } else {
            Dimension d = getSize();
            if (orient == JSplitPane.VERTICAL_SPLIT) {
                right = d.height - pos;
            } else {
                right = d.width - pos;
            }
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }
}
