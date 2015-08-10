package org.uecide;

import org.uecide.*;
import org.uecide.debug.*;
import org.uecide.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;
import java.beans.*;

import org.markdown4j.Markdown4jProcessor;


public class JImageTextPane extends JTextPane {
    BufferedImage backgroundImage = null;
    float alpha = 0.2f;

    JImageTextPane() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int w = getWidth();
        int h = getHeight();
        Color color1 = Color.WHITE;
        Color color2 = new Color(215, 225, 255);
        GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, w, h);
        if (backgroundImage != null) {
            int iw = backgroundImage.getWidth();
            int ih = backgroundImage.getHeight();
            float aspect = (float)ih / (float)iw;

            AlphaComposite alcom = AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, alpha);
            g2d.setComposite(alcom);
            g2d.drawImage(backgroundImage, 0, 0, w, (int)((float)w * aspect), this);
            alcom = AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 1.0f);
            g2d.setComposite(alcom);
        }
        super.paintComponent(g);
    }

    void setBackgroundImage(BufferedImage img) {
        backgroundImage = img;
    }

    void setAlphaBlending(float f) {
        alpha = f;
    }

}
