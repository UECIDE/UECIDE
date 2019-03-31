package org.uecide;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.imageio.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class CleverIcon extends ImageIcon {
    int size;
    int frame = 0;
    int frames = 0;
    String[] name;
    BufferedImage[] scaledImage;
    ImageIcon disabledIcon = null;

    public CleverIcon(int s, String... n) throws IOException {
        super();
        size = s;
        frames = n.length;
        name = new String[frames];
        scaledImage = new BufferedImage[frames];
        for (int i = 0; i < frames; i++) {
            name[i] = n[i];
        }
        updateIcon();
    }

    public void updateIcon() throws IOException, MalformedURLException {
        for (int i = 0; i < frames; i++) {
            BufferedImage image;
            URL u = IconManager.getIconPath(name[i]);
            if (u == null) {
                u = FileManager.getURLFromPath("res://org/uecide/icons/unknown.png");
            }
            try {
                image = ImageIO.read(u);
            } catch (Exception ex) {
                image = ImageIO.read(FileManager.getURLFromPath("res://org/uecide/icons/unknown.png"));
            }

            Image scaled = Utils.getScaledImage(image, size, size);
            scaledImage[i] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaledImage[i].createGraphics();
            g.drawImage(scaled, 0, 0, size, size, 0, 0, size, size, null);
        }

        BufferedImage disabledImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Color disabledColor = UIManager.getColor("Button.disabledColor");
        if (disabledColor == null) {
            disabledColor = new Color(0x80, 0x80, 0x80);
        }
        int col = disabledColor.getRGB();
        col &= 0x00FFFFFF;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int rgb = scaledImage[0].getRGB(x, y);
                rgb &= 0xFF000000;
                rgb |= col;
                disabledImage.setRGB(x, y, rgb);
            }
        }


        disabledIcon = new ImageIcon(disabledImage);
        

        setImage(scaledImage[0]);
    }

    public void animate() {
        if (frames <= 1) return;
        frame++;
        if (frame >= frames) {
            frame = 0;
        }
        setImage(scaledImage[frame]);
    }

    public ImageIcon disabled() {
        return disabledIcon;
    }
}
