/*
 * Copyright (c) 2014, Majenko Technologies
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
        try {
            URL loc = Splash.class.getResource("/uecide/app/icons/about.png");
            image = ImageIO.read(loc);
            w = image.getWidth();
            h = image.getHeight();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            setBounds((screen.width-w)/2, (screen.height-h)/2, w, h);
            setVisible(true);
        } catch (Exception e) {
            Base.error(e);
        }
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
        g2.drawString("v" + Base.systemVersion, x, y);
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
}
