package org.uecide.plugin;

import org.uecide.*;
import org.uecide.debug.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.datatransfer.*;
import jssc.*;


public class LCD extends JComponent
{

    byte[] screenData;
    

    Color bg = new Color(0, 0, 0);
    Color fg = new Color(255, 255, 255);

    int lcd_width = 248;
    int lcd_height = 64;
    int lcd_bytes = 1984;

    public LCD(int w, int h)
    {
        lcd_width = w;
        lcd_height = h;
        lcd_bytes = (w / 8) * h;
        screenData = new byte[lcd_bytes];
    }

    public void paintComponent(Graphics screen) 
    {
        Rectangle size = getBounds(null);

        BufferedImage offscreen = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphic = offscreen.createGraphics();

        double pxo = (double)size.width / (double)lcd_width;
        double pyo = (double)size.height / (double)lcd_height;

        double pxi = pxo * 0.75;
        double pyi = pyo * 0.75;

        double pos_y = 0;
        double pos_x = 0;

        int bytepos = 0;

        graphic.setColor(bg);
        graphic.fillRect(0, 0, size.width, size.height);
        graphic.setColor(fg);

        for (int y = 0; y < lcd_height; y++) {
            pos_x = 0;
            for (int x = 0; x < lcd_width; x += 8) {
                int mask = 0x80;
                for (int z = 0; z < 8; z++) {        
                    if ((screenData[bytepos] & mask) != 0) {
                        graphic.fillRect((int)Math.round(pos_x), (int)Math.round(pos_y), (int)Math.round(pxi), (int)Math.round(pyi));
                    }
                    mask = mask >> 1;
                    pos_x += pxo;
                }
                bytepos++;
            }
            pos_y += pyo;
        }

        screen.drawImage(offscreen, 0, 0, null);
    }

    public Dimension getSize() { return new Dimension(lcd_width * 4, lcd_height * 4); }
    public Dimension getPreferredSize() { return new Dimension(lcd_width * 4, lcd_height * 4); }
    public Dimension getMinimumSize() { return new Dimension(lcd_width * 2, lcd_height * 2); }
    public Dimension getMaximumSize() { return new Dimension(lcd_width * 8, lcd_height * 8); }

    public void setByte(int offset, byte data) {
        if (offset < lcd_bytes) {
            screenData[offset] = data;
        }
        repaint();
    }

    public void setData(byte[] data) {
        int len = data.length;
        if (len > screenData.length) {
            len = screenData.length;
        }

        for (int i = 0; i < len; i++) {
            screenData[i] = data[i];
        }
        repaint();
    }

    public void setDimensions(Dimension size) {
        lcd_width = size.width;
        lcd_height = size.height;
        lcd_bytes = (lcd_width / 8) * lcd_height;
        screenData = new byte[lcd_bytes];
        repaint();
    }

    public void setBackground(int r, int g, int b) {
        bg = new Color(r, g, b);
        repaint();
    }

    public void setForeground(int r, int g, int b) {
        fg = new Color(r, g, b);
        repaint();
    }

    public void setPixel(int x, int y, boolean state) {
        if ((x < 0) || (y < 0) || (x >= lcd_width) || (y >= lcd_height)) {
            return;
        }
        int offset = ((lcd_width * y) / 8) + (x / 8);
        int bitpos = 7 - (x % 8);
        if (offset >= lcd_bytes) {
            return;
        }
        if (state) {
            screenData[offset] |= (1<<bitpos);
        } else {
            screenData[offset] &= ~(1<<bitpos);
        }
        repaint();
    }
}
