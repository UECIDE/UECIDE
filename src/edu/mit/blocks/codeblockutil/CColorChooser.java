package edu.mit.blocks.codeblockutil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class CColorChooser extends CButton implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 328149080226L;
    public static final String COLOR_CHANGE = "color_change_property";
    private ColorPicker popup;
    private Color previousColor = null;

    public CColorChooser(Color color) {
        super(color, Color.gray, "RGB = 0,0,0");
        popup = new ColorPicker(color);
        popup.setPopupSize(200, 100);
        this.add(popup);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /**
     * Returns the currently selected Color
     * @return the currently selected Color
     */
    public Color getColor() {
        return popup.getColor();
    }

    /**
     * Sets the selected color to the specified color
     * @param color desired Color to set as selected
     */
    public void setColor(Color color) {
        previousColor = buttonColor;
        popup.setColor(color);
        this.buttonColor = popup.getColor();
        this.repaint();
        String colorText = "RGB = " + buttonColor.getRed() + ", " + buttonColor.getGreen() + ", " + buttonColor.getBlue();
        this.setText(colorText);
        this.firePropertyChange(COLOR_CHANGE, previousColor, buttonColor);

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        popup.setVisible(false);
        String colorText = "RGB = " + buttonColor.getRed() + ", " + buttonColor.getGreen() + ", " + buttonColor.getBlue();
        this.setText(colorText);
        this.firePropertyChange(COLOR_CHANGE, previousColor, buttonColor);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        popup.mouseDragged(SwingUtilities.convertMouseEvent(this, e, popup));
        this.previousColor = buttonColor;
        this.buttonColor = popup.getColor();
        this.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        popup.show(this, e.getX(), e.getY());
    }

    /**
     * ColorPicker is a popup menu that displays a spectrum of colors. Users can drag their 
     * mouse over the menu and the location of mouse release is the selected color.
     */
    public class ColorPicker extends JPopupMenu implements MouseListener, MouseMotionListener {

        private static final long serialVersionUID = 328149080227L;
        private float WIDTH = 200;
        private float HEIGHT = 100;
        /** must be between 0 and 1, o being dark */
        private float hue = 0.75f;
        private int mx = 0;
        private int my = 0;
        private Color color = null;
        private BufferedImage buffImg = null;

        /**
         * Constructs a color picker popup
         * @param color the initial selected Color
         */
        public ColorPicker(Color color) {
            super();
            this.color = color;
            this.setBounds(0, 0, 500, 500);
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }

        /**
         * Returns the selected color or null if there is none.
         * @return the selected color.  MAY BE NULL.
         */
        public Color getColor() {
            return color;
        }

        /**
         * Sets the selected color to the the specified color
         * @param color desired color to set as selected
         */
        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public void paint(Graphics g) {
            if (buffImg == null) {
                buffImg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                Graphics gb = buffImg.getGraphics();
                for (int i = 0; i <= WIDTH; i++) {
                    for (int j = 0; j <= HEIGHT; j++) {
                        gb.setColor(new Color(Color.HSBtoRGB(i / WIDTH, j / HEIGHT, hue)));
                        gb.fillRect(i, j, 1, 1);
                    }
                }
            }
            g.drawImage(buffImg, 0, 0, null);
            g.setColor(Color.black);
            g.drawOval(mx - 2, my - 2, 2, 2);
            g.setColor(Color.white);
            g.drawOval(mx - 3, my - 3, 3, 3);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (e.getX() < 0) {
                mx = 0;
            } else if (e.getX() > WIDTH) {
                mx = (int) WIDTH;
            } else {
                mx = e.getX();
            }
            if (e.getY() < 0) {
                my = 0;
            } else if (e.getY() > HEIGHT) {
                my = (int) HEIGHT;
            } else {
                my = e.getY();
            }
            int col = Color.HSBtoRGB(mx / WIDTH, my / HEIGHT, hue);
            color = new Color(col);
            this.repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }
    }

}
