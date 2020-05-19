package edu.mit.blocks.codeblockutil;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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

import javax.swing.JPanel;
import javax.swing.JTextField;

public class CQueryField extends JPanel implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 328149080259L;
    private JTextField field;
    private boolean pressed = false;
    private boolean mouseover = false;

    public CQueryField() {
        this(null);
    }

    public CQueryField(String text) {
        super(new BorderLayout());
        field = new JTextField(text);
        field.setBorder(null);
        field.setFont(new Font("Ariel", Font.PLAIN, 13));

        this.setBounds(0, 0, 200, 20);
        this.setPreferredSize(new Dimension(200, 20));

        this.setOpaque(false);
        this.add(field, BorderLayout.CENTER);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.revalidate();
        this.repaint();
    }

    @Override
    public Insets getInsets() {
        int h = this.getHeight();
        return new Insets(h / 6, h, h / 6, h);
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
        //RoundRectangle2D.Double box = new RoundRectangle2D.Double(w-5*h/6, h/6, 2*h/3, 2*h/3, h/3, h/3);
        return box;
    }

    private Shape getMag(int w, int h) {
        Ellipse2D.Double e = new Ellipse2D.Double(h / 2, h / 6, h * 1 / 3, h * 1 / 3);
        GeneralPath shape = new GeneralPath();
        shape.moveTo(h / 3, h * 2 / 3);
        shape.lineTo(h / 2, h / 2);
        shape.append(e, false);
        return shape;
    }

    public JTextField getQueryField() {
        return field;
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

        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.darkGray.brighter());
        g2.draw(this.getMag(w, h));

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

    public String getText() {
        return field.getText();
    }

    public void setText(String text) {
        field.setText(text);
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
            field.setText("");
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
