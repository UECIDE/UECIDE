package edu.mit.blocks.codeblockutil;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

/**
 * A CButton is a swing-compatible widget that allows clients
 * to display an oval button with an optional text.
 * 
 * To add a particular action to this widget, users should invoke
 * this.addCButtonListener( new CButtonListener());
 */
public class CGraphiteButton extends CButton {

    private static final long serialVersionUID = 328149080221L;

    public CGraphiteButton(String text) {
        super(Color.black, CGraphite.blue, text);
    }

    /**
     * re paints this
     */
    @Override
    public void paint(Graphics g) {
        // Set up graphics and buffer
        //super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Set up first layer
        int buttonHeight = this.getHeight() - (INSET * 2);
        int buttonWidth = this.getWidth() - (INSET * 2);
        int arc = buttonHeight / 3;
        Color topColoring;
        Color bottomColoring;
        if (this.pressed || this.selected) {
            if (this.focus) {
                topColoring = this.selectedColor.darker().darker().darker();
                bottomColoring = CGraphite.blue;
            } else {
                topColoring = CGraphite.blue.darker().darker().darker();
                bottomColoring = CGraphite.blue;
            }
        } else {
            if (this.focus) {
                topColoring = this.buttonColor;
                bottomColoring = Color.darkGray;
            } else {
                topColoring = this.buttonColor;
                bottomColoring = this.buttonColor;
            }
        }
        // Paint the first layer
        g2.setPaint(new GradientPaint(0, 0, topColoring, 0, buttonHeight, bottomColoring, false));
        g2.fillRoundRect(INSET, INSET, buttonWidth, buttonHeight, arc, arc);
        g2.setColor(Color.darkGray);
        g2.drawRoundRect(INSET, INSET, buttonWidth, buttonHeight, arc, arc);

        // set up paint data fields for second layer
        int highlightHeight = buttonHeight / 2 - HIGHLIGHT_INSET;
        int highlightWidth = buttonWidth - (HIGHLIGHT_INSET * 2) + 1;
        if (this.pressed || this.selected) {
            if (this.focus) {
                topColoring = Color.white;
                bottomColoring = this.selectedColor;
            } else {
                topColoring = Color.white;
                bottomColoring = this.selectedColor;
            }
        } else {
            if (this.focus) {
                topColoring = Color.white;
                bottomColoring = Color.darkGray;
            } else {
                topColoring = Color.gray;
                bottomColoring = Color.darkGray;
            }
        }
        // Paint the second layer
        g2.setPaint(new GradientPaint(0, 0, topColoring, 0, buttonHeight, bottomColoring, false));
        g2.fillRoundRect(INSET + HIGHLIGHT_INSET, INSET + HIGHLIGHT_INSET + 1, highlightWidth, highlightHeight, arc, arc);
        //g2.setColor(Color.gray);
        //g2.drawRoundRect(INSET+HIGHLIGHT_INSET,INSET+HIGHLIGHT_INSET,highlightWidth,highlightHeight,arc,arc);


        // Draw the text (if any)
        if (this.getText() != null) {
            if (this.focus) {
                g2.setColor(Color.white);
            } else {
                g2.setColor(Color.white);
            }
            Font font = g2.getFont().deriveFont((float) (((float) buttonHeight) * .4));
            g2.setFont(font);
            FontMetrics metrics = g2.getFontMetrics();
            Rectangle2D textBounds = metrics.getStringBounds(this.getText(), g2);
            float x = (float) ((this.getWidth() / 2) - (textBounds.getWidth() / 2));
            float y = (float) ((this.getHeight() / 2) + (textBounds.getHeight() / 2)) - metrics.getDescent();
            g2.drawString(this.getText(), x, y);
        }
    }

}
