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

public class BMPEdit extends JPanel implements PixelClickListener, MouseWheelListener {

    BufferedImage image;

    JScrollPane mainPanel;

    ZoomableBitmap imagePanel;

    ToolsPanel toolsPanel;
    EditBar editBar;

    int paintMode = 0;

    ArrayList<BufferedImage> history = new ArrayList<BufferedImage>();

    boolean modified = false;

    static final int KEY_MASK = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK;

    public BMPEdit() {
        super();
        image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);

        imagePanel = new ZoomableBitmap(image);

        mainPanel = new JScrollPane();
        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        outerPanel.add(imagePanel, c);
        mainPanel.setViewportView(outerPanel);

        imagePanel.addPixelClickListener(this);

        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);

        MouseWheelListener[] l = mainPanel.getMouseWheelListeners();
        for (MouseWheelListener list : l) {
            mainPanel.removeMouseWheelListener(list);
        }
        mainPanel.addMouseWheelListener(this);

        try {
            toolsPanel = new ToolsPanel(this);

            add(toolsPanel, BorderLayout.EAST);
        } catch (Exception e) {
            e.printStackTrace();
        }

        editBar = new EditBar(this);
        add(editBar, BorderLayout.NORTH);

    }

    public void loadImage(File f) throws IOException {
        image = ImageIO.read(f);
        imagePanel.updateImage(image);
    }

    public void pixelClicked(PixelClickEvent e) {
    }

    public void pixelPressed(PixelClickEvent e) {

        switch (toolsPanel.getSelectedTool()) {
            case ToolsPanel.DRAW:
                processDraw(e);
                break;
            case ToolsPanel.ERASE:
                processErase(e);
                break;
            case ToolsPanel.PICK:
                processPick(e);
                break;
            case ToolsPanel.SELECT:
                processSelect(e);
                break;
        }
    }

    public void processDraw(PixelClickEvent e) {
        int mods = e.getModifiersEx() & KEY_MASK;

        switch (mods) {
            case 0: { // Normal press
                if (toolsPanel.getSelectedTool() == ToolsPanel.DRAW) {
                    if (e.getButton() == 1) paintMode = 1;
                    if (e.getButton() == 3) paintMode = 2;

                    storeHistory();

                    paintPixel(e.getPixel());
                }
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

    public void processErase(PixelClickEvent e) {
        int mods = e.getModifiersEx() & KEY_MASK;

        switch (mods) {
            case 0: { // Normal press
                if (toolsPanel.getSelectedTool() == ToolsPanel.ERASE) {
                    if (e.getButton() == 1) paintMode = 3;

                    storeHistory();

                    paintPixel(e.getPixel());
                }
            } break;
        }
    }

    public void processSelect(PixelClickEvent e) {
        int x = e.getPixel().x;
        int y = e.getPixel().y;

        if ((e.getButton() == 1) || (paintMode == 1)) {
            imagePanel.setRubberbandTopLeft(x, y);
            paintMode = 1;
        } else if ((e.getButton() == 3) || (paintMode == 2)) {
            imagePanel.setRubberbandBottomRight(x, y);
            paintMode = 2;
        }
    }

    public void processPick(PixelClickEvent e) {
        int x = e.getPixel().x;
        int y = e.getPixel().y;

        int rgb = image.getRGB(x, y);
        if (e.getButton() == 1) {
            toolsPanel.setForegroundColor(new Color(rgb));
        } else if (e.getButton() == 3) {
            toolsPanel.setBackgroundColor(new Color(rgb));
        }
        toolsPanel.setSelectedTool(ToolsPanel.DRAW);
    }

    public void paintPixel(Point p) {
        Graphics2D g = (Graphics2D)image.getGraphics();

        int size = toolsPanel.getToolSize();

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
                imagePanel.repaint();
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
                imagePanel.repaint();
                break;
            case 3:
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.setColor(new Color(0, 0, 0, 0));
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f));
                if (size == 1) {
                    g.fillRect(p.x, p.y, 1, 1);
                } else {
                    g.fillOval(p.x - (size / 2), p.y - (size / 2), size, size);
                }
                imagePanel.repaint();
                break;
        }
    }

    public void drawLine(Point from, Point to) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        int size = toolsPanel.getToolSize();
        switch (paintMode) {
            case 1:
                g.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(toolsPanel.getForegroundColor());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g.drawLine(from.x, from.y, to.x, to.y);
                imagePanel.repaint();
                break;
            case 2:
                g.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(toolsPanel.getBackgroundColor());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g.drawLine(from.x, from.y, to.x, to.y);
                imagePanel.repaint();
                break;
            case 3:
                g.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(0, 0, 0, 0));
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f));
                g.drawLine(from.x, from.y, to.x, to.y);
                imagePanel.repaint();
                break;
        }
    }

    public void pixelReleased(PixelClickEvent e) {
        paintMode = 0;
    }

    public void pixelEntered(PixelClickEvent e) {
        switch (toolsPanel.getSelectedTool()) {
            case ToolsPanel.DRAW:
                if (paintMode != 0) {
                    drawLine(e.getPreviousPixel(), e.getPixel());
                }
                break;
            case ToolsPanel.SELECT:
                if (paintMode != 0) {
                    processSelect(e);
                }
                break;
        }
    }
    
    public void pixelExited(PixelClickEvent e) {
    }

    public void storeHistory() {
        setModified(true);
        BufferedImage i = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        i.setData(image.getData());
        history.add(i);
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    public void undo() {
        if (history.size() == 0) return;
        BufferedImage i = history.remove(history.size()-1);
        image = i;
        imagePanel.updateImage(image);
    }


    public void mouseWheelMoved(MouseWheelEvent event) {
        int mods = event.getModifiersEx() & KEY_MASK;

        if (mods == 0) { // Vertical scroll
            int rot = event.getWheelRotation();
            int clicks = event.getScrollAmount();
            JScrollBar bar = mainPanel.getVerticalScrollBar();
            int v = bar.getValue();
            v += (rot * clicks);
            bar.setValue(v);
        } else if (mods == InputEvent.SHIFT_DOWN_MASK) {
            int rot = event.getWheelRotation();
            int clicks = event.getScrollAmount();
            JScrollBar bar = mainPanel.getHorizontalScrollBar();
            int v = bar.getValue();
            v += (rot * clicks);
            bar.setValue(v);
        } else if (mods == InputEvent.CTRL_DOWN_MASK) {
            int rot = event.getWheelRotation();
            double zoom = imagePanel.getZoomFactor();
            if (rot < 0) {
                zoom = zoom * 1.5d;
            } else {
                zoom = zoom / 1.5d;
            }
            if (zoom < 1d) zoom = 1d;
            if (zoom > 64d) zoom = 64d;
            imagePanel.setZoomFactor(zoom);
            mainPanel.revalidate();
        }
    }

    public boolean saveImage(File to) throws IOException {
        String lcname = to.getName().toLowerCase();
        if (lcname.endsWith(".gif")) {
            ImageIO.write(image, "gif", to);
            setModified(false);
            return true;
        } else if (lcname.endsWith(".jpg")) {
            ImageIO.write(image, "jpeg", to);
            setModified(false);
            return true;
        } else if (lcname.endsWith(".png")) {
            ImageIO.write(image, "png", to);
            setModified(false);
            return true;
        }
        return false;
    }

    public boolean isModified() { return modified; }

    public void setModified(boolean m) { modified = m; }

    public void scale() {
        JPanel scalePanel = new JPanel();
        scalePanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;

        scalePanel.add(new JLabel("Width:"), c);
        c.gridy = 1;
        scalePanel.add(new JLabel("Height:"), c);
        c.gridy = 2;
        scalePanel.add(new JLabel("Scaling:"), c);

        JTextField widthField = new JTextField(10);
        widthField.setText("" + image.getWidth());
        c.gridx = 1;
        c.gridy = 0;
        scalePanel.add(widthField, c);

        JTextField heightField = new JTextField(10);
        heightField.setText("" + image.getHeight());
        c.gridx = 1;
        c.gridy = 1;
        scalePanel.add(heightField, c);

        String[] options = { "Bilinear", "Bicubic", "Nearest" };
        JComboBox<String> scaling = new JComboBox<String>(options);
        c.gridx = 1;
        c.gridy = 2;
        scalePanel.add(scaling, c);


        int r = JOptionPane.showConfirmDialog(this, scalePanel, "Scale Image", JOptionPane.OK_CANCEL_OPTION);

        if (r == JOptionPane.OK_OPTION) {
            try {
                storeHistory();
                int w = Integer.parseInt(widthField.getText());
                int h = Integer.parseInt(heightField.getText());

                BufferedImage i = new BufferedImage(w, h, image.getType());

                Graphics2D g = i.createGraphics();
                if (((String)scaling.getSelectedItem()).equals("Bilinear")) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                } else if (((String)scaling.getSelectedItem()).equals("Bicubic")) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                } else if (((String)scaling.getSelectedItem()).equals("Nearest")) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                } 
                g.drawImage(image, 0, 0, w, h, 0, 0, image.getWidth(), image.getHeight(), null);
                g.dispose();
                image = i;
                imagePanel.updateImage(image);
            } catch (Exception e) {
            }
        }

    }

    public void crop() {
        Rectangle r = imagePanel.getRubberband();
        cropImage(r);
    }

    public void cropImage(Rectangle r) {
        BufferedImage newImage = new BufferedImage(r.width, r.height, image.getType());
        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, 
            0, 0, /* by */ r.width, r.height,
            r.x, r.y, /* by */ r.x + r.width, r.y + r.height,
            null
        );
        g.dispose();
        storeHistory();
        image = newImage;
        imagePanel.setRubberbandTopLeft(0, 0);
        imagePanel.setRubberbandBottomRight(r.width, r.height);
        imagePanel.updateImage(image);
    }

    public ZoomableBitmap getImagePanel() {
        return imagePanel;
    }

}
