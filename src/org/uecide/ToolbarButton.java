package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;

import java.awt.font.FontRenderContext;

public class ToolbarButton extends JButton implements MouseListener {

    Icon buttonIcon;

    int size;
    boolean hover = false;
    String text = "";
    Font font;

    public ToolbarButton(ImageIcon ico) {
        this(ico, null);
    }

    public ToolbarButton(ImageIcon ico, ActionListener al) {
        this(ico, ico.getIconWidth() * 133 / 100, al);
    }
    public ToolbarButton(ImageIcon ico, int s, ActionListener al) {
        super();
        buttonIcon = ico;
        setIcon(ico);
        setDisabledIcon(ico);
        if (al != null) {
            super.addActionListener(al);
        }

        size = s;

        setBorderPainted(false);
        setContentAreaFilled(false);
        super.addMouseListener(this);
        System.err.println("This is used...?!");
    }

    public ToolbarButton(String name, String tooltip, int s) throws IOException {
        this(name, tooltip, s, null);
    }

    public ToolbarButton(String name, String tooltip) throws IOException {
        this(name, tooltip, Preferences.getInteger("theme.iconsize"), null);
    }

    public ToolbarButton(String name, String tooltip, ActionListener al) throws IOException {
        this(name, tooltip, Preferences.getInteger("theme.iconsize"), al);
    }

    @Override
    public void setIcon(Icon i) {
        buttonIcon = i;
    }

    public ToolbarButton(String name, String tooltip, int s, ActionListener al) throws IOException {
        super();
        size = s;
        CleverIcon icon = IconManager.getIcon(size * 66 / 100, name);
        buttonIcon = icon;
        setIcon(icon);
        setDisabledIcon(icon.disabled());
        text = tooltip;
        if (al != null) {
            super.addActionListener(al);
        }
        super.addMouseListener(this);
        InputStream is = ToolbarButton.class.getResourceAsStream("/fonts/NotoSans-Light.ttf");
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(10.0f);
        } catch (FontFormatException ex) {
            Base.error(ex);
        }
    }

    @Override
    public boolean isFocusPainted() { return false; }
    @Override
    public Insets getInsets() { return new Insets(2, 2, 2, 2); }

    @Override
    public Dimension getSize() {
        Integer scale = UIManager.getInt("Button.scale");
        if (scale == null) {
            scale = new Integer(125);
        }
        if (scale < 1) {
            scale = new Integer(125);
        }
        Integer scaled = size * scale / 100;
        Dimension s = new Dimension(hover ? (scaled + getTextWidth()) : scaled, scaled);
        return s;
    }

    @Override
    public Dimension getPreferredSize() { return getSize(); }
    @Override
    public Dimension getMinimumSize() { return getSize(); }
    @Override
    public Dimension getMaximumSize() { return getSize(); }

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) { hover = true; SwingUtilities.updateComponentTreeUI(this); }
    public void mouseExited(MouseEvent e) { hover = false; SwingUtilities.updateComponentTreeUI(this); }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}

    void paintColouredIcon(Graphics2D g, Icon i, int x, int y, Color c) {

        int red = c.getRed();
        int green = c.getGreen();
        int blue = c.getBlue();

        int w = i.getIconWidth();
        int h = i.getIconHeight();

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics ig = img.createGraphics();
        i.paintIcon(null, ig, 0, 0);
        ig.dispose();

        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int rgba = img.getRGB(px, py);
                Color col = new Color(rgba, true);
                int alpha = col.getAlpha();
                Color destCol = new Color(red, green, blue, alpha);
                g.setColor(destCol);
                g.drawLine(x + px, y + py, x + px, y + py);
            }
        }

    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;

        RenderingHints rh = new RenderingHints(
             RenderingHints.KEY_TEXT_ANTIALIASING,
             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHints(rh);

        Dimension dim = getSize();

        if (!hover) {
            int iy = dim.height / 2 - buttonIcon.getIconHeight() / 2;
            int ix = iy;

            paintColouredIcon(g2d, buttonIcon, ix, iy, UIManager.getColor("Button.foreground"));
            return;
        }

        int iy = dim.height / 2 - buttonIcon.getIconHeight() / 2;
        int ix = iy;
        paintColouredIcon(g2d, buttonIcon, ix, iy, UIManager.getColor("Button.foreground"));
        g2d.setColor(UIManager.getColor("Button.foreground"));
        g2d.setFont(font);

        if (text.length() < 10) {
            g2d.drawString(text, size, dim.height / 2 + 5);
        } else {
            int l = text.length();
            int mid = l/2;
            int i = 0;
            String line1 = "";
            String line2 = "";
            while (((mid - i) > 0) && ((mid + i) < l)) {
                if (text.charAt(mid + i) == ' ') {
                    line1 = text.substring(0, mid + i);
                    line2 = text.substring(mid + i + 1);
                    break;
                }
                if (text.charAt(mid - i) == ' ') {
                    line1 = text.substring(0, mid - i);
                    line2 = text.substring(mid - i + 1);
                    break;
                }
                i++;
            }
            g2d.drawString(line1, size + 2, dim.height / 2 );
            g2d.drawString(line2, size + 2, dim.height / 2 + 10);
            
        }
    }

    int getTextWidth() {
        FontMetrics fm = getFontMetrics(font);
        if (text.length() < 10) {
            return fm.stringWidth(text);
        } else {
            int l = text.length();
            int mid = l/2;
            int i = 0;
            String line1 = "";
            String line2 = "";
            while (((mid - i) > 0) && ((mid + i) < l)) {
                if (text.charAt(mid + i) == ' ') {
                    line1 = text.substring(0, mid + i);
                    line2 = text.substring(mid + i + 1);
                    break;
                }
                if (text.charAt(mid - i) == ' ') {
                    line1 = text.substring(0, mid - i);
                    line2 = text.substring(mid - i + 1);
                    break;
                }
                i++;
            }

            int l1 = fm.stringWidth(line1);
            int l2 = fm.stringWidth(line2);
            if (l1 > l2) return l1;
            return l2;
        }
    }

}
