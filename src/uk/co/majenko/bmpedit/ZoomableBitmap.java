package uk.co.majenko.bmpedit;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import java.util.*;

public class ZoomableBitmap extends JPanel implements MouseListener, MouseMotionListener {
    BufferedImage image;
    double zoomFactor = 1.0d;

    ArrayList<PixelClickListener> listeners = new ArrayList<PixelClickListener>();

    Point lastPixelEntered = new Point(0, 0);

    Rectangle rubberband = null;

    public ZoomableBitmap(BufferedImage i) {
        super();
        image = i;
        addMouseListener(this);
        addMouseMotionListener(this);
        updateComponentSize();

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;

        Dimension size = getSize();

        Color c1 = new Color(64,64,64);
        Color c2 = new Color(192,192,192);

        for (int y = 0; y < size.height; y += 8) {
            for (int x = 0; x < size.width; x += 8) {

                g2d.setColor(c1);
                if ((x & 8) == (y & 8)) {
                    g2d.setColor(c1);
                } else {
                    g2d.setColor(c2);
                }
                g2d.fillRect(x, y, 8, 8);
            }
        }


        g2d.drawImage(image, 0, 0, size.width, size.height, null);

        if (zoomFactor > 8d) {
            int w = image.getWidth();
            int h = image.getHeight();

            g2d.setColor(Color.BLACK);
            for (int y = 0; y < h; y ++) {
                for (int x = 0; x < w; x++) {
                    g2d.drawRect((int)(x * zoomFactor), (int)(y * zoomFactor), (int)zoomFactor, (int)zoomFactor);
                }
            }
        }

        if (rubberband != null) {
            float[] dash1 = {4f};
            BasicStroke dashBlack = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dash1, 0f);
            BasicStroke dashWhite = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dash1, 4f);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(dashBlack);
            g2d.drawRect((int)(rubberband.x * zoomFactor), (int)(rubberband.y * zoomFactor), (int)(rubberband.width * zoomFactor), (int)(rubberband.height * zoomFactor));
            g2d.setColor(Color.WHITE);
            g2d.setStroke(dashWhite);
            g2d.drawRect((int)(rubberband.x * zoomFactor), (int)(rubberband.y * zoomFactor), (int)(rubberband.width * zoomFactor), (int)(rubberband.height * zoomFactor));

        }

    }

    public void setZoomFactor(double zf) {
        zoomFactor = zf;
        updateComponentSize();
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void updateComponentSize() {
        double width = (double)image.getWidth() * zoomFactor;
        double height = (double)image.getHeight() * zoomFactor;

        setSize(new Dimension((int)width, (int)height));
        setMinimumSize(new Dimension((int)width, (int)height));
        setMaximumSize(new Dimension((int)width, (int)height));
        setPreferredSize(new Dimension((int)width, (int)height));
        repaint();
    }

    public void updateImage(BufferedImage i) {
        image = i;
        updateComponentSize();
    }


    // Mouse events

    public void mouseEntered(MouseEvent event) {
    }

    public void mouseExited(MouseEvent event) {
    }

    public void mouseClicked(MouseEvent event) {
        Point p = event.getPoint();
        double px = p.x / zoomFactor;
        double py = p.y / zoomFactor;

        Point newPoint = new Point((int)px, (int)py);
        lastPixelEntered = newPoint;
        PixelClickEvent pe = new PixelClickEvent(newPoint, newPoint, event.getButton(), this, event.getModifiersEx());

        for (PixelClickListener l : listeners) {
            l.pixelClicked(pe);
        }
    }

    public void mouseReleased(MouseEvent event) {
        Point p = event.getPoint();
        double px = p.x / zoomFactor;
        double py = p.y / zoomFactor;

        Point newPoint = new Point((int)px, (int)py);
        PixelClickEvent pe = new PixelClickEvent(newPoint, newPoint, event.getButton(), this, event.getModifiersEx());

        for (PixelClickListener l : listeners) {
            l.pixelReleased(pe);
        }
    }
    
    public void mousePressed(MouseEvent event) {
        Point p = event.getPoint();
        double px = p.x / zoomFactor;
        double py = p.y / zoomFactor;

        Point newPoint = new Point((int)px, (int)py);
        lastPixelEntered = newPoint;
        PixelClickEvent pe = new PixelClickEvent(newPoint, newPoint, event.getButton(), this, event.getModifiersEx());

        for (PixelClickListener l : listeners) {
            l.pixelPressed(pe);
        }
    }

    public void addPixelClickListener(PixelClickListener l) {
        if (listeners.indexOf(l) == -1) {
            listeners.add(l);
        }
    }

    public void removePixelClickListener(PixelClickListener l) {
        listeners.remove(l);
    }

    public void mouseMoved(MouseEvent event) {
        Point p = event.getPoint();
        double px = p.x / zoomFactor;
        double py = p.y / zoomFactor;
        int ipx = (int)px;
        int ipy = (int)py;

        Point newPoint = new Point(ipx, ipy);

        if ((ipx != lastPixelEntered.x) || (ipy != lastPixelEntered.y)) {
            PixelClickEvent pexit  = new PixelClickEvent(lastPixelEntered, lastPixelEntered, event.getButton(), this, event.getModifiersEx());
            PixelClickEvent penter = new PixelClickEvent(newPoint, lastPixelEntered, event.getButton(), this, event.getModifiersEx());

            for (PixelClickListener l : listeners) {
                l.pixelExited(pexit);
                l.pixelEntered(penter);
            }
            lastPixelEntered = new Point(ipx, ipy);
        }
    }

    public void mouseDragged(MouseEvent event) {
        Point p = event.getPoint();
        double px = p.x / zoomFactor;
        double py = p.y / zoomFactor;
        int ipx = (int)px;
        int ipy = (int)py;
        Point newPoint = new Point(ipx, ipy);

        if ((ipx != lastPixelEntered.x) || (ipy != lastPixelEntered.y)) {
            PixelClickEvent pexit  = new PixelClickEvent(lastPixelEntered, lastPixelEntered, event.getButton(), this, event.getModifiersEx());
            PixelClickEvent penter = new PixelClickEvent(newPoint, lastPixelEntered, event.getButton(), this, event.getModifiersEx());

            for (PixelClickListener l : listeners) {
                l.pixelExited(pexit);
                l.pixelEntered(penter);
            }
            lastPixelEntered = new Point(ipx, ipy);
        }
    }

}
