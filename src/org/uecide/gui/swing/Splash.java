/*
 * Copyright (c) 2015, Majenko Technologies
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide.gui.swing;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.font.*;
import javax.swing.*;
import javax.imageio.*;

import org.uecide.*;

public class Splash extends JDialog { //Window {
    BufferedImage image;

    String message = "";
    String betaMessage = "";

    int percent = 0;
    int w;
    int h;

    public Splash() {
        super(null, JDialog.ModalityType.MODELESS);
        try {
            image = ImageIO.read(Splash.class.getResource("/org/uecide/icons/about.png"));
            w = image.getWidth();
            h = image.getHeight();

            setUndecorated(true);
            setResizable(false);
    
            setTitle("Loading UECIDE");

            setSize(new Dimension(w, h));
            setPreferredSize(new Dimension(w, h));
            setMaximumSize(new Dimension(w, h));
            setMinimumSize(new Dimension(w, h));
            setLocationRelativeTo(null);
            setVisible(true);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(w, h);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(w, h);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(w, h);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.drawImage(image, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(new Color(0xbe, 0xbe, 0xbe));
        drawCenteredString(g2, 285, 135, "v" + Base.systemVersion, new Color(0xbe, 0xbe, 0xbe), new Color(0, 0, 0, 96));

        int barHeight = 20;

        g2.setColor(new Color(50, 10, 0));
        g2.fillRect(0, h - barHeight, w , h - 1);
        g2.setColor(new Color(150, 40, 10));
        g2.fillRect(0, h - barHeight, (int)(((float)percent / 100.0) * (float)w) , h - 1);
        g2.setColor(Color.white);
        drawCenteredString(g2, w / 2, h - (barHeight / 2), message, new Color(0xbe, 0xbe, 0xbe), new Color(0, 0, 0, 64));
    }

    public void drawCenteredString(Graphics2D g, int x, int y, String message, Color fg, Color shad) {
        Rectangle r = getStringBounds(g, message);
        
        int px = x - (r.width / 2);
        int py = y - (r.height / 2) + r.height; // Text goes *up* from here, everything else goes down.

        g.setColor(shad);
        g.drawString(message, px + 3, py + 3);
        g.setColor(fg);
        g.drawString(message, px, py);
    }

    public void setMessage(String m) {
        message = m;
        repaint();
    }

    public void setMessage(String m, int p) {
        setMessage(m);
        setPercent(p);
    }

    public void setBetaMessage(String m) {
        betaMessage = m;
        repaint();
    }

    public void setPercent(int p) {
        percent = p;
        repaint();
    }

    public void enableCloseOnClick() {
        final Splash me = this;
        this.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                me.dispose();
            }
            public void mouseExited(MouseEvent e) {
            }
            public void mousePressed(MouseEvent e) {
            }
            public void mouseReleased(MouseEvent e) {
            }
            public void mouseEntered(MouseEvent e) {
            }
        });
    }

    public void enableScrollContributors(int x, int y) {
    }

    private Rectangle getStringBounds(Graphics2D g2, String str) {
        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
        return gv.getPixelBounds(null, 0, 0);
    }
}
