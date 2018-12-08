package org.uecide;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class Utils {
    public static Image getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();

        return resizedImg;
    }

    public static boolean s2b(String s) {
        if (s == null) return false;
        if (s.equals("true")) return true;
        if (s.equals("t")) return true;
        if (s.equals("yes")) return true;
        if (s.equals("y")) return true;
        return false;
    }

    public static int s2i(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
        }
        return 0;
    }

    public static float s2f(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
        }
        return 0.0f;
    }

    public static void browse(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void open(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(new File(url));
            } catch (Exception ex) {
            }
        }
    }
}
