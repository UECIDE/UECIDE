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

public class FillTool implements Tool {

    static final int KEY_MASK = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK;

    int paintMode = 0;
    ToolsPanel toolsPanel;

    JSlider toolSize;
    JPanel toolSizePanel;

    ToolButton button;

    public FillTool(ToolsPanel tb) {
        toolsPanel = tb;
        button = new ToolButton("/uk/co/majenko/bmpedit/icons/fill.png", this);
        button.setSelectedColor(new Color(0, 200, 0));
        button.setToolTipText("Fill");
    }

    public ToolButton getButton() {
        return button;
    }

    public JPanel getOptionsPanel() {
        return null;
    }

    public void press(ZoomableBitmap bm, PixelClickEvent e) {

        BufferedImage image = bm.getImage();

        int mods = e.getModifiersEx() & KEY_MASK;

        switch (mods) {
            case 0: 
                if (e.getButton() == 1) {
                    bm.fill(e.getPixel().x, e.getPixel().y, toolsPanel.getForegroundColor());
                } else if (e.getButton() == 3) {
                    bm.fill(e.getPixel().x, e.getPixel().y, toolsPanel.getBackgroundColor());
                }
                break;
        }
    }

    public void release(ZoomableBitmap bm, PixelClickEvent e) {
    }

    public void drag(ZoomableBitmap bm, PixelClickEvent e) {
    }

    public void select(ZoomableBitmap bm) {
        button.setSelected(true);
        bm.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void deselect(ZoomableBitmap bm) {
        button.setSelected(false);
    }
}
