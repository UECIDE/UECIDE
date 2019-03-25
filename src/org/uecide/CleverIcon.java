package org.uecide;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.imageio.*;
import java.net.*;
import java.io.*;

public class CleverIcon extends ImageIcon {
    int size;
    String name;
    BufferedImage scaledImage;
    BufferedImage image;

    public CleverIcon(int s, String n) throws IOException {
        super();
        size = s;
        name = n;
        updateIcon();
    }

    public void updateIcon() throws IOException, MalformedURLException {
        URL u = IconManager.getIconPath(name);
        if (u == null) {
            u = FileManager.getURLFromPath("res://org/uecide/icons/unknown.png");
        }
        try {
            image = ImageIO.read(u);
        } catch (Exception ex) {
            image = ImageIO.read(FileManager.getURLFromPath("res://org/uecide/icons/unknown.png"));
        }

        Image i = Utils.getScaledImage(image, size, size);
        scaledImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaledImage.createGraphics();
        g.drawImage(i, 0, 0, size-1, size-1, 0, 0, size-1, size-1, null);

        setImage(scaledImage);
    }
}
