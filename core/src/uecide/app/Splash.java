package uecide.app;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import uecide.plugin.*;


import javax.swing.*;
import javax.imageio.*;

import uecide.app.debug.Board;
import uecide.app.debug.Core;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * The base class for the main uecide.application.
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading
 * files and images, etc) that comes from that.
 */
public class Splash extends Window {
    BufferedImage image;

    String message = "";

    int percent = 0;
    int w;
    int h;

    public Splash() {
        super((Window) null);
        image = Base.getLibBufferedImage("theme/about.png");
        w = image.getWidth();
        h = image.getHeight();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screen.width-w)/2, (screen.height-h)/2, w, h);
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        int x = 0;
        int y = 0;
        int mx = 0;
        int my = 0;
        try {
            x = Integer.parseInt(Base.theme.get("about.version.x"));
            y = Integer.parseInt(Base.theme.get("about.version.y"));

            mx = Integer.parseInt(Base.theme.get("splash.message.x"));
            my = Integer.parseInt(Base.theme.get("splash.message.y"));
        } catch (Exception e) {
        }

        BufferedImage temp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = temp.createGraphics();
        g2.drawImage(image, 0, 0, null);

        if (x < 0) {
            x = w + x;
        }

        if (y < 0) {
            y = h + y;
        }

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(Base.theme.getFont("about.version.font"));
        g2.setColor(Base.theme.getColor("about.version.color"));
        g2.drawString("v" + Base.VERSION_NAME, x, y);
        g2.drawString(message, mx, my);

        g2.setColor(new Color(200,0,00));
        g2.fillRect(0, h-4, (int) (((float)percent / 100.0) * (float)w) , h - 1); 
        g.drawImage(temp, 0, 0, null);
    }

    public void setMessage(String m) {
        message = m;
        repaint();
    }

    public void setMessage(String m, int p) {
        setMessage(m);
        setPercent(p);
    }

    public void setPercent(int p) {
        percent = p;
        repaint();
    }
}
