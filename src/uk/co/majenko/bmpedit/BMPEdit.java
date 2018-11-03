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

        imagePanel = new ZoomableBitmap(new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB));

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
        imagePanel.updateImage(ImageIO.read(f));
    }

    public void pixelClicked(PixelClickEvent e) {
    }

    public void pixelPressed(PixelClickEvent e) {
        storeHistory();
        toolsPanel.getSelectedTool().press(imagePanel, e);
    }

    public void pixelReleased(PixelClickEvent e) {
        toolsPanel.getSelectedTool().release(imagePanel, e);
    }

    public void pixelEntered(PixelClickEvent e) {
        toolsPanel.getSelectedTool().drag(imagePanel, e);
    }
    
    public void pixelExited(PixelClickEvent e) {
    }

    public void storeHistory() {
        setModified(true);
        BufferedImage i = new BufferedImage(imagePanel.getImage().getWidth(), imagePanel.getImage().getHeight(), imagePanel.getImage().getType());
        i.setData(imagePanel.getImage().getData());
        history.add(i);
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    public void undo() {
        if (history.size() == 0) return;
        BufferedImage i = history.remove(history.size()-1);
        imagePanel.updateImage(i);
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
            ImageIO.write(imagePanel.getImage(), "gif", to);
            setModified(false);
            return true;
        } else if (lcname.endsWith(".jpg")) {
            ImageIO.write(imagePanel.getImage(), "jpeg", to);
            setModified(false);
            return true;
        } else if (lcname.endsWith(".png")) {
            ImageIO.write(imagePanel.getImage(), "png", to);
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
        widthField.setText("" + imagePanel.getImage().getWidth());
        c.gridx = 1;
        c.gridy = 0;
        scalePanel.add(widthField, c);

        JTextField heightField = new JTextField(10);
        heightField.setText("" + imagePanel.getImage().getHeight());
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

                BufferedImage i = new BufferedImage(w, h, imagePanel.getImage().getType());

                Graphics2D g = i.createGraphics();
                if (((String)scaling.getSelectedItem()).equals("Bilinear")) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                } else if (((String)scaling.getSelectedItem()).equals("Bicubic")) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                } else if (((String)scaling.getSelectedItem()).equals("Nearest")) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                } 
                g.drawImage(imagePanel.getImage(), 0, 0, w, h, 0, 0, imagePanel.getImage().getWidth(), imagePanel.getImage().getHeight(), null);
                g.dispose();
                imagePanel.updateImage(i);
            } catch (Exception e) {
            }
        }

    }

    public void crop() {
        Rectangle r = imagePanel.getRubberband();
        cropImage(r);
    }

    public void cropImage(Rectangle r) {
        BufferedImage newImage = new BufferedImage(r.width, r.height, imagePanel.getImage().getType());
        Graphics2D g = newImage.createGraphics();
        g.drawImage(imagePanel.getImage(), 
            0, 0, /* by */ r.width, r.height,
            r.x, r.y, /* by */ r.x + r.width, r.y + r.height,
            null
        );
        g.dispose();
        storeHistory();
        imagePanel.setRubberbandTopLeft(0, 0);
        imagePanel.setRubberbandBottomRight(r.width, r.height);
        imagePanel.updateImage(newImage);
    }

    public ZoomableBitmap getImagePanel() {
        return imagePanel;
    }

    double[] userMatrix = {
        0,  0,  0,
        0,  1,  0, 
        0,  0,  0
    };

    public void conv() {
        JPanel convOptions = new JPanel();
        convOptions.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;

        convOptions.add(new JLabel("Preset: "), c);

        String[] presets = {
            "Select...",
            "Blur",
            "Edge Detect",
            "Sharpen",
            "Lighten",
            "Darken"
        };

        JComboBox<String> preset = new JComboBox<String>(presets);

        c.gridx = 1;
        c.gridwidth = 2;
        convOptions.add(preset, c);

        JTextField matrix[] = {
            new JTextField(5), new JTextField(5), new JTextField(5),
            new JTextField(5), new JTextField(5), new JTextField(5),
            new JTextField(5), new JTextField(5), new JTextField(5)
        };

        for (int i = 0; i < 9; i++) {
            matrix[i].setText(String.format("%.3f", userMatrix[i]));
        }

        for (int i = 0; i < 9; i++) {
            c.gridx = i % 3;
            c.gridy = i / 3 + 1;
            c.gridwidth = 1;

            convOptions.add(matrix[i], c);
        }

        preset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sel = (String)preset.getSelectedItem();
                if (sel.equals("Blur")) for (int i = 0; i < 9; i++) matrix[i].setText(String.format("%.3f", Convolution.BLUR[i]));
                if (sel.equals("Edge Detect")) for (int i = 0; i < 9; i++) matrix[i].setText(String.format("%.3f", Convolution.EDGE[i]));
                if (sel.equals("Sharpen")) for (int i = 0; i < 9; i++) matrix[i].setText(String.format("%.3f", Convolution.SHARPEN[i]));
                if (sel.equals("Lighten")) for (int i = 0; i < 9; i++) matrix[i].setText(String.format("%.3f", Convolution.LIGHTEN[i]));
                if (sel.equals("Darken")) for (int i = 0; i < 9; i++) matrix[i].setText(String.format("%.3f", Convolution.DARKEN[i]));
                preset.setSelectedIndex(0);
            }
        });

        int rv = JOptionPane.showConfirmDialog(this, convOptions, "Apply Convolution Matrix", JOptionPane.OK_CANCEL_OPTION);

        if (rv == JOptionPane.OK_OPTION) {
            for (int i = 0; i < 9; i++) {
                try {
                    userMatrix[i] = Double.parseDouble(matrix[i].getText());
                } catch (Exception ignored) {
                    userMatrix[i] = 0;
                }
            }
        

            storeHistory();
            Convolution convolution = new Convolution(userMatrix);

            if (imagePanel.isRubberbandShown()) {

                Rectangle r = imagePanel.getRubberband();
                
                BufferedImage newImage = new BufferedImage(r.width, r.height, imagePanel.getImage().getType());
                Graphics2D g = newImage.createGraphics();
                g.drawImage(imagePanel.getImage(),
                    0, 0, /* by */ r.width, r.height,
                    r.x, r.y, /* by */ r.x + r.width, r.y + r.height,
                    null
                );
                g.dispose();

                BufferedImage convolved = convolution.apply(newImage);

                g = imagePanel.getImage().createGraphics();
                g.drawImage(convolved, r.x, r.y, r.x + r.width, r.y + r.height,
                                       0, 0, r.width, r.height,
                                        null);
                g.dispose();
            } else {
                imagePanel.updateImage(convolution.apply(imagePanel.getImage()));
            }
        }
    }

}
