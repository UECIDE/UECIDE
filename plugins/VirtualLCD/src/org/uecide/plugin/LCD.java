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
    

    Color bg = new Color(30, 30, 255);
    Color fg = new Color(200, 200, 255);

    int lcd_width = 248;
    int lcd_height = 64;
    int lcd_bytes = 1984;

    public LCD(int w, int h)
    {
        lcd_width = w;
        lcd_height = h;
        lcd_bytes = (w / 8) * h;
        screenData = new byte[lcd_bytes];
        for (int i = 0; i < lcd_bytes; i++) {
            screenData[i] = (byte)(Math.random() * 255.0);
        }
    }

    public void paintComponent(Graphics screen) 
    {
        Rectangle size = getBounds(null);

        BufferedImage offscreen = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphic = offscreen.createGraphics();

        int pxo = size.width / lcd_width;
        int pyo = size.height / lcd_height;

        int pxi = pxo - 1;
        int pyi = pyo - 1;

        int pos_y = 0;
        int pos_x = 0;

        int bytepos = 0;

        for (int y = 0; y < lcd_height; y++) {
            pos_x = 0;
            for (int x = 0; x < lcd_width; x += 8) {
                int mask = 0x80;
                for (int z = 0; z < 8; z++) {        
                    graphic.setColor(bg);
                    graphic.fillRect(pos_x, pos_y, pxo, pyo);
                    if ((screenData[bytepos] & mask) != 0) {
                        graphic.setColor(fg);
                        graphic.fillRect(pos_x, pos_y, pxi, pyi);
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
        screenData[offset] = data;
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
}
