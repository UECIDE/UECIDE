package uk.co.majenko.bmpedit;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class SelectTool implements Tool {

    static final int KEY_MASK = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK;

    int paintMode = 0;
    ToolsPanel toolsPanel;

    JSlider toolSize;
    JPanel toolSizePanel;

    ToolButton button;

    public SelectTool(ToolsPanel tb) {
        toolsPanel = tb;
        button = new ToolButton("/uk/co/majenko/bmpedit/icons/select.png", this);
        button.setSelectedColor(new Color(0, 200, 0));
        button.setToolTipText("Rectangle Selection");
    }

    public ToolButton getButton() {
        return button;
    }

    public JPanel getOptionsPanel() {
        return null;
    }

    public void press(ZoomableBitmap bm, PixelClickEvent e) {

        int mods = e.getModifiersEx() & KEY_MASK;

        switch (mods) {
            case 0: {
                int x = e.getPixel().x;
                int y = e.getPixel().y;

                if ((e.getButton() == 1) || (paintMode == 1)) {
                    bm.setRubberbandTopLeft(x, y);
                    paintMode = 1;
                } else if ((e.getButton() == 3) || (paintMode == 2)) {
                    bm.setRubberbandBottomRight(x, y);
                    paintMode = 2;
                }
            } break;
        }
    }

    public void release(ZoomableBitmap bm, PixelClickEvent e) {
        paintMode = 0;
    }

    public void drag(ZoomableBitmap bm, PixelClickEvent e) {
        if (paintMode > 0) {
            press(bm, e);
        }
    }

    public void select(ZoomableBitmap bm) {
        button.setSelected(true);
        bm.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        bm.showRubberband();
    }

    public void deselect(ZoomableBitmap bm) {
        button.setSelected(false);
        bm.hideRubberband();
    }
}
