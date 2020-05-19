package edu.mit.blocks.codeblockutil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.text.Format;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentListener;

public class CTextField extends JFormattedTextField implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 328149080250L;
    private boolean pressed = false;
    private boolean mouseover = false;

    public CTextField() {
        this("");
    }

    public CTextField(Format format) {
        super(format);
        this.setBorder(null);
        this.setFont(new Font("Ariel", Font.PLAIN, 13));

        this.setOpaque(false);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.revalidate();
        this.repaint();
    }

    public CTextField(String text) {
        super(text);
        this.setBorder(null);
        this.setFont(new Font("Ariel", Font.PLAIN, 13));

        this.setOpaque(false);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.revalidate();
        this.repaint();
    }

    @Override
    public Insets getInsets() {
        int h = this.getHeight();
        return new Insets(h / 6, h / 2, h / 6, h);
    }

    private Shape getXCross(int w, int h) {
        GeneralPath shape = new GeneralPath();
        shape.moveTo(w - h * 2 / 3, h / 3);
        shape.lineTo(w - h / 3, h * 2 / 3);
        shape.moveTo(w - h / 3, h / 3);
        shape.lineTo(w - h * 2 / 3, h * 2 / 3);
        return shape;
    }

    private Shape getXBox(int w, int h) {
        Ellipse2D.Double box = new Ellipse2D.Double(w - 5 * h / 6, h / 6, 2 * h / 3, 2 * h / 3);
        return box;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int w = this.getWidth();
        int h = this.getHeight();
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);


        g2.setColor(Color.white);
        g2.fillRoundRect(0, 0, w, h, h, h);

        if (mouseover) {
            if (pressed) {
                g2.setColor(new Color(170, 0, 0));
            } else {
                g2.setColor(Color.red);
            }
        } else {
            g2.setColor(Color.pink);
        }
        g2.fill(this.getXBox(w, h));

        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(2));
        g2.draw(this.getXCross(w, h));

        super.paint(g);
    }

    public void addDocumentListener(DocumentListener l) {
        this.getDocument().addDocumentListener(l);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseover = false;
        this.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getX() > this.getWidth() - this.getHeight() * 5 / 6) {
            pressed = true;
            this.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getX() > this.getWidth() - this.getHeight() * 5 / 6) {
            this.setText("");
            pressed = false;
            this.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (e.getX() > this.getWidth() - this.getHeight() * 5 / 6) {
            if (mouseover == false) {
                mouseover = true;
                this.repaint();
            }
        } else {
            if (mouseover == true) {
                mouseover = false;
                this.repaint();
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

}
