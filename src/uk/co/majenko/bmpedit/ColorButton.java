package uk.co.majenko.bmpedit;

import javax.swing.JPanel;
import javax.swing.JColorChooser;
import javax.swing.UIManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ColorButton extends JPanel implements MouseListener {
    Color fg = new Color(255, 255, 255);
    Color bg = new Color(0, 0, 0);

    final Rectangle fgrect = new Rectangle(8,8,32,23);
    final Rectangle bgrect = new Rectangle(24,0,32,23);

    public ColorButton() {
        super();
        addMouseListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(64, 32);
    }

    @Override
    public Dimension getMaximumSize() { return getPreferredSize(); }

    @Override
    public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D)g;

        g2d.setColor(bg);
        g2d.fillRect(bgrect.x, bgrect.y, bgrect.width, bgrect.height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(bgrect.x, bgrect.y, bgrect.width, bgrect.height);

        g2d.setColor(fg);
        g2d.fillRect(fgrect.x, fgrect.y, fgrect.width, fgrect.height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(fgrect.x, fgrect.y, fgrect.width, fgrect.height);
    }
    
    @Override
    public void mouseEntered(MouseEvent evt) {
    }

    @Override
    public void mouseExited(MouseEvent evt) {
    }

    @Override
    public void mousePressed(MouseEvent evt) {
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        if (evt.getButton() == 1) {
            int x = evt.getX();
            int y = evt.getY();


            if (fgrect.contains(x, y)) {
                JColorChooser fc = new JColorChooser();
                Color clr = fc.showDialog(this, "Select Foreground Color", fg);
                if(clr != null) {
                    fg = clr;
                    repaint();
                }
            } else if (bgrect.contains(x, y)) {
                JColorChooser fc = new JColorChooser();
                Color clr = fc.showDialog(this, "Select Background Color", bg);
                if(clr != null) {
                    bg = clr;
                    repaint();
                }
            }
        }
    }

    public Color getForegroundColor() {
        return fg;
    }

    public Color getBackgroundColor() {
        return bg;
    }

    public void setForegroundColor(Color c) {
        fg = c;
        repaint();
    }

    public void setBackgroundColor(Color c) {
        bg = c;
        repaint();
    }

}
