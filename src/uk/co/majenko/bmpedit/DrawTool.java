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

public class DrawTool implements Tool {

    static final int KEY_MASK = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK;

    int paintMode = 0;
    ToolsPanel toolsPanel;

    JSlider toolSize;
    JPanel toolSizePanel;

    ToolButton button;

    public DrawTool(ToolsPanel tb) {
        toolsPanel = tb;
        button = new ToolButton("/uk/co/majenko/bmpedit/icons/draw.png", this);
        button.setSelectedColor(new Color(0, 200, 0));
    }

    public ToolButton getButton() {
        return button;
    }

    public JPanel getOptionsPanel() {
        toolSizePanel = new JPanel();
        toolSizePanel.setLayout(new BorderLayout());
        toolSizePanel.setBackground(Color.WHITE);
        toolSizePanel.setSize(new Dimension(32, 128));
        toolSizePanel.setMinimumSize(new Dimension(32, 128));
        toolSizePanel.setMaximumSize(new Dimension(32, 128));
        toolSizePanel.setPreferredSize(new Dimension(32, 128));

        toolSize = new JSlider(1, 32, 1);
        toolSize.setOrientation(JSlider.VERTICAL);
        toolSize.setMajorTickSpacing(1);
        toolSize.setMinorTickSpacing(1);
        toolSize.setPaintTicks(true);
        toolSize.setPaintLabels(false);
        toolSize.setSnapToTicks(true);
        toolSize.setBorder(new EmptyBorder(0, 0, 0, 0));
        toolSizePanel.add(toolSize, BorderLayout.CENTER);

        JTextField sizeValue = new JTextField(4);
        sizeValue.setText("1");

        toolSize.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                sizeValue.setText("" + toolSize.getValue());
            }
        });

        sizeValue.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    toolSize.setValue(Integer.parseInt(sizeValue.getText()));
                } catch (Exception ignored) {
                }
                sizeValue.setText("" + toolSize.getValue());
            }
        });

        toolSizePanel.add(sizeValue, BorderLayout.SOUTH);
        return toolSizePanel;
    }

    public void press(ZoomableBitmap bm, PixelClickEvent e) {

        BufferedImage image = bm.getImage();

        int mods = e.getModifiersEx() & KEY_MASK;

        switch (mods) {
            case 0: { // Normal press
                if (e.getButton() == 1) paintMode = 1;
                if (e.getButton() == 3) paintMode = 2;
                paintPixel(image, e.getPixel());
                bm.repaint();
            } break;

            case InputEvent.CTRL_DOWN_MASK: {
                int x = e.getPixel().x;
                int y = e.getPixel().y;

                int rgb = image.getRGB(x, y);
                if (e.getButton() == 1) {
                    toolsPanel.setForegroundColor(new Color(rgb));
                } else if (e.getButton() == 3) {
                    toolsPanel.setBackgroundColor(new Color(rgb));
                }
            } break;
        }
    }

    public void release(ZoomableBitmap bm, PixelClickEvent e) {
        paintMode = 0;
    }

    public void drag(ZoomableBitmap bm, PixelClickEvent e) {
        if (paintMode != 0) {
            BufferedImage image = bm.getImage();
            drawLine(image, e.getPreviousPixel(), e.getPixel());
            bm.repaint();
        }
    }

    void paintPixel(BufferedImage image, Point p) {
        Graphics2D g = (Graphics2D)image.getGraphics();

        int size = toolSize.getValue();

        switch (paintMode) {
            case 1:
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.setColor(toolsPanel.getForegroundColor());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                if (size == 1) {
                    g.fillRect(p.x, p.y, 1, 1);
                } else {
                    g.fillOval(p.x - (size / 2), p.y - (size / 2), size, size);
                }
                break;
            case 2:
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.setColor(toolsPanel.getBackgroundColor());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                if (size == 1) {
                    g.fillRect(p.x, p.y, 1, 1);
                } else {
                    g.fillOval(p.x - (size / 2), p.y - (size / 2), size, size);
                }
                break;
        }
    }

    void drawLine(BufferedImage image, Point from, Point to) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        int size = toolSize.getValue();
        switch (paintMode) {
            case 1:
                g.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(toolsPanel.getForegroundColor());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g.drawLine(from.x, from.y, to.x, to.y);
                break;
            case 2:
                g.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(toolsPanel.getBackgroundColor());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g.drawLine(from.x, from.y, to.x, to.y);
                break;
        }
    }

    public void select(ZoomableBitmap bm) {
        button.setSelected(true);
        bm.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        toolSizePanel.setVisible(true);
    }

    public void deselect(ZoomableBitmap bm) {
        button.setSelected(false);
        toolSizePanel.setVisible(false);
    }
}
