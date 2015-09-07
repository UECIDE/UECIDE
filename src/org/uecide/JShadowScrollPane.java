package org.uecide;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

public class JShadowScrollPane extends JScrollPane {
    BufferedImage img = null;
    int topShadow = 0;
    int bottomShadow = 0;

    public JShadowScrollPane(int ts, int bs) {
        super();
        topShadow = ts;
        bottomShadow = bs;

        if (topShadow < 0) topShadow = 0;
        if (topShadow > 9) topShadow = 9;
        if (bottomShadow < 0) bottomShadow = 0;
        if (bottomShadow > 9) bottomShadow = 9;

        JViewport vp = getViewport();
        vp.setScrollMode(JViewport.BLIT_SCROLL_MODE);
        vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        vp.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    } 

    @Override
    public void paint(Graphics g) {
        JViewport vp = getViewport();
        if (img == null || img.getWidth() != getWidth() || img.getHeight() != getHeight()) {
            img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g2 = img.createGraphics();
        super.paint(g2);

        Rectangle bounds = vp.getVisibleRect();

        for (int i = 0; i < topShadow; i++) {
            g2.setPaint(new Color(0f, 0f, 0f, 0.1f + ((float)i / 10f)));
            g2.drawLine(0, topShadow - i - 1, bounds.width, topShadow - i - 1);
        }
        for (int i = 0; i < bottomShadow; i++) {
            g2.setPaint(new Color(0f, 0f, 0f, 0.1f + ((float)i / 10f)));
            g2.drawLine(0, bounds.height - bottomShadow + i + 1, bounds.width, bounds.height - bottomShadow + i + 1);
        }
        g2.dispose();
        g.drawImage(img, 0, 0, null);
    }

    public void setShadow(int ts, int bs) {
        topShadow = ts;
        bottomShadow = bs;
        if (topShadow < 0) topShadow = 0;
        if (topShadow > 9) topShadow = 9;
        if (bottomShadow < 0) bottomShadow = 0;
        if (bottomShadow > 9) bottomShadow = 9;
        repaint();
    }
}

