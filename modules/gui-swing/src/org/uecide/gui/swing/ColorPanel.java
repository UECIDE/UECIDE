package org.uecide.gui.swing;

import javax.swing.JPanel;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;

public class ColorPanel extends JPanel {

    final Dimension size = new Dimension(64, 32);

    public ColorPanel() {
        super();
    }

    public void setColor(Color c) {
        setBackground(c);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Dimension d = getSize();
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, d.width - 1, d.height - 1);
        g.setColor(Color.WHITE);
        g.drawRect(1, 1, d.width - 3, d.height - 3);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

}
