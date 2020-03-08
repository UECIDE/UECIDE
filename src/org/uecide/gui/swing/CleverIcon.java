package org.uecide.gui.swing;

import org.uecide.*;

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
    ArrayList<AnimationListener> animationListeners = new ArrayList<AnimationListener>();

    public CleverIcon(int s, String... n) throws IOException {
        super();
        size = s;
        name = n;
        updateIcon();
    }

    public CleverIcon(int s, File f) throws IOException {
        super();
        size = s;
        if (f == null) {
            name = new String[1];
            name[0] = "default";
            updateIcon();
            return;
        } 

        if (!f.exists()) {
            name = new String[1];
            name[0] = "default";
            updateIcon();
            return;
        }
        updateIcon(f);
    }

    public void updateIcon(File f) throws IOException {
        BufferedImage image;
        image = ImageIO.read(f);
        scaledImage = new BufferedImage[1];
        Image scaled = Utils.getScaledImage(image, size, size); 
        scaledImage[0] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaledImage[0].createGraphics();
        g.drawImage(scaled, 0, 0, size, size, 0, 0, size, size, null);

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
         
    public void updateIcon() throws IOException, MalformedURLException {
        ArrayList<URL> frameList = new ArrayList<URL>();

        for (int i = 0; i < name.length; i++) {
            URL[] u = IconManager.getIconPaths(name[i]);
            for (URL url : u) {
                frameList.add(url);
            }
        }

        frames = frameList.size();

        scaledImage = new BufferedImage[frames];

        for (int i = 0; i < frames; i++) {
            BufferedImage image;
            URL u = frameList.get(i);
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
        for (AnimationListener l : animationListeners) {
            l.animationUpdated(this);
        }
    }

    public ImageIcon disabled() {
        return disabledIcon;
    }

    public void addAnimationListener(AnimationListener l) {
        animationListeners.add(l);
    }
}
