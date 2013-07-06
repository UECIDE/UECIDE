package uecide.plugin;

import uecide.app.*;
import uecide.app.debug.*;

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


public class JTerminal extends JComponent implements MessageConsumer,KeyListener,MouseListener,FocusListener
{
    BufferedImage offscreen = null;
    
    Point cursorPosition = new Point(0, 0);
    Dimension textSize = new Dimension(80, 24);
    Dimension characterSize = new Dimension(8, 20);
    Dimension screenSize = new Dimension(640,480);;
    int fontDescent;
    Font font = null;
    Graphics2D graphic;

    boolean hasFocus = false;

    boolean cursorShow = true;

    int brightness = 0;

    boolean cursorBox = false;

    Color[] normalColors = new Color[8];
    Color[] brightColors = new Color[8];
    Color[] dimColors = new Color[8];

    final int BLACK = 0;
    final int RED = 1;
    final int GREEN = 2;
    final int YELLOW = 3;
    final int BLUE = 4;
    final int MAGENTA = 5;
    final int CYAN = 6;
    final int WHITE = 7;

    final String ESC = Character.toString((char)27);

    int color;
    int background;
    int cursorColor;

    MessageConsumer keyPressConsumer = null;

    public JTerminal()
    {
        offscreen = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_RGB);
        graphic = offscreen.createGraphics();
        cursorPosition = new Point(0, 0);

        normalColors[BLACK] = new Color(0,0,0);
        normalColors[RED] = new Color(192, 0, 0);
        normalColors[GREEN] = new Color(0, 192, 0);
        normalColors[YELLOW] = new Color(192, 192, 0);
        normalColors[BLUE] = new Color(0, 0, 192);
        normalColors[MAGENTA] = new Color(192, 0, 192);
        normalColors[CYAN] = new Color(0, 192, 192);
        normalColors[WHITE] = new Color(192, 192, 192);

        brightColors[BLACK] = new Color(0,0,0);
        brightColors[RED] = new Color(255, 0, 0);
        brightColors[GREEN] = new Color(0, 255, 0);
        brightColors[YELLOW] = new Color(255, 255, 0);
        brightColors[BLUE] = new Color(0, 0, 255);
        brightColors[MAGENTA] = new Color(255, 0, 255);
        brightColors[CYAN] = new Color(0, 255, 255);
        brightColors[WHITE] = new Color(255, 255, 255);

        dimColors[BLACK] = new Color(0,0,0);
        dimColors[RED] = new Color(128, 0, 0);
        dimColors[GREEN] = new Color(0, 128, 0);
        dimColors[YELLOW] = new Color(128, 128, 0);
        dimColors[BLUE] = new Color(0, 0, 128);
        dimColors[MAGENTA] = new Color(128, 0, 128);
        dimColors[CYAN] = new Color(0, 128, 128);
        dimColors[WHITE] = new Color(128, 128, 128);

        color = WHITE;
        background = BLACK;
        cursorColor = WHITE;
        addKeyListener(this);
        setFocusable(true);
        addMouseListener(this);
        addFocusListener(this);
    }

    public void setKeypressConsumer(MessageConsumer m)
    {
        keyPressConsumer = m;
    }

    public void showCursor(boolean sc)
    {
        cursorShow = sc;
        repaint();
    }

    public void boxCursor(boolean bc) {
        cursorBox = bc;
    }

    public void setFont(Font f)
    {
        font = f;
        setDimension();
    }

    public void setDimension()
    {
        graphic.setFont(font);
        FontMetrics fm = graphic.getFontMetrics();

        int ascent = fm.getMaxAscent();
        fontDescent = fm.getMaxDescent();
        int width = fm.charWidth('W');


        characterSize = new Dimension(
            width,
            ascent + fontDescent
        );
        screenSize = new Dimension(
            characterSize.width * textSize.width,
            characterSize.height * textSize.height
        );
        clearScreen();
    }

    public Color getFGColor()
    {
        if (brightness < 0) {
            return dimColors[color];
        }
        if (brightness > 0) {
            return brightColors[color];
        }
        return normalColors[color];
    }

    public Color getBGColor()
    {
        return normalColors[background];
    }

    public void clearScreen()
    {
        offscreen = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_RGB);
        graphic = offscreen.createGraphics();

        cursorPosition = new Point(0, 0);
        graphic.setColor(getBGColor());
        graphic.fillRect(0, 0, screenSize.width, screenSize.height);
        graphic.setFont(font);
    }

    public Point charToGraphic(Point p) {
        return new Point(
            p.x * characterSize.width,
            p.y * characterSize.height
        );
    }

    public Point charToGraphicBaseline(Point p) {
        return new Point(
            p.x * characterSize.width,
            (p.y+1) * characterSize.height - fontDescent
        );
    }

    public void paintComponent(Graphics screen) 
    {
        screen.drawImage(offscreen, 0, 0, null);
        if (cursorShow) {
            screen.setColor(brightColors[cursorColor]);

            if (cursorBox) {
                Point cursorStart = charToGraphic(cursorPosition);
                if (hasFocus) {
                    screen.setXORMode(getBGColor());
                    screen.fillRect(cursorStart.x, cursorStart.y, characterSize.width, characterSize.height);
                    screen.setPaintMode();
                } else {
                    screen.drawRect(cursorStart.x, cursorStart.y, characterSize.width, characterSize.height);
                }
            } else {
                Point cursorStart = charToGraphicBaseline(cursorPosition);

                screen.drawLine(cursorStart.x, cursorStart.y + 1,
                    cursorStart.x + characterSize.width - 1, cursorStart.y + 1);
                screen.drawLine(cursorStart.x, cursorStart.y + 2,
                    cursorStart.x + characterSize.width - 1, cursorStart.y + 2);
            }
        }
    }

    public void scrollUp()
    {
        graphic.drawImage(offscreen, 0, 0 - characterSize.height, null);
    }

    public void drawCharacter(char c)
    {
        graphic.setFont(font);
        graphic.setColor(getBGColor());
        graphic.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Point cPos = charToGraphic(cursorPosition);

        graphic.fillRect(cPos.x, cPos.y, characterSize.width, characterSize.height);
        graphic.setColor(getFGColor());

        String text = Character.toString(c);

        cPos = charToGraphicBaseline(cursorPosition);

        graphic.drawString(text, cPos.x, cPos.y);
        cursorPosition.x++;
        if (cursorPosition.x == textSize.width) {
            cursorPosition.x = 0;
            cursorPosition.y++;
            if (cursorPosition.y == textSize.height) {
                cursorPosition.y = textSize.height - 1;
                scrollUp();
            }
        }
    }

    public void message(String m, int c)
    {
        message(m);
    }

    boolean inEscapeSequence = false;
    String escapeSequence = "";

    public void message(String m)
    {
        char[] chars = m.toCharArray();
        for (char c : chars) {
            switch (c) {
                case 27:
                    if (inEscapeSequence) {
                        drawCharacter('^');
                        drawCharacter('[');
                        inEscapeSequence = false;
                    } else {
                        inEscapeSequence = true;
                        escapeSequence = "";
                    }
                    break;
                case '\n':
                    inEscapeSequence = false;
                    cursorPosition.y++;
                    if (cursorPosition.y == textSize.height) {
                        cursorPosition.y = textSize.height - 1;
                        scrollUp();
                    }
                    break;
                case '\r':
                    inEscapeSequence = false;
                    cursorPosition.x = 0;
                    break;
                case 8:
                    inEscapeSequence = false;
                    cursorPosition.x--;
                    if (cursorPosition.x < 0) {
                        cursorPosition.x = textSize.width - 1;
                        cursorPosition.y--;
                        if (cursorPosition.y < 0) {
                            cursorPosition.y = 0;
                        }
                    }
                    break;
                default:
                    if (inEscapeSequence) {
                        escapeSequence += Character.toString(c);
                        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                            executeEscapeSequence(escapeSequence);
                            escapeSequence = "";
                            inEscapeSequence = false;
                        }
                    } else {
                        drawCharacter(c);
                    }
            }
        }
        repaint();
        revalidate();
        this.getParent().revalidate();
        this.getParent().repaint();
    }

    public void executeEscapeSequence(String sequence)
    {
        try {
            if (sequence.startsWith("[")) {
                String subSequence = "0";
                if (sequence.length() > 2) {
                    subSequence = sequence.substring(1, sequence.length()-1);
                }
                if (sequence.equals("[2J")) {
                    clearScreen();
                    return;
                }

                if (sequence.endsWith("A")) {
                    int moveAmount = Integer.parseInt(subSequence);
                    if (moveAmount <= 0) {
                        moveAmount = 1;
                    }

                    cursorPosition.y -= moveAmount;
                    if (cursorPosition.y < 0) {
                        cursorPosition.y = 0;
                    }
                    return;
                }

                if (sequence.endsWith("B")) {
                    int moveAmount = Integer.parseInt(subSequence);
                    if (moveAmount <= 0) {
                        moveAmount = 1;
                    }

                    cursorPosition.y += moveAmount;
                    if (cursorPosition.y >= textSize.height) {
                        cursorPosition.y = textSize.height - 1;
                    }
                    return;
                }

                if (sequence.endsWith("C")) {
                    int moveAmount = Integer.parseInt(subSequence);
                    if (moveAmount <= 0) {
                        moveAmount = 1;
                    }

                    cursorPosition.x += moveAmount;
                    if (cursorPosition.x >= textSize.width) {
                        cursorPosition.x = textSize.width - 1;
                    }
                    return;
                }

                if (sequence.endsWith("D")) {
                    int moveAmount = Integer.parseInt(subSequence);
                    if (moveAmount <= 0) {
                        moveAmount = 1;
                    }

                    cursorPosition.x -= moveAmount;
                    if (cursorPosition.x < 0) {
                        cursorPosition.x = 0;
                    }
                    return;
                }

                if (sequence.endsWith("E")) {
                    int moveAmount = Integer.parseInt(subSequence);
                    if (moveAmount <= 0) {
                        moveAmount = 1;
                    }

                    cursorPosition.y += moveAmount;
                    if (cursorPosition.y >= textSize.height) {
                        cursorPosition.y = textSize.height - 1;
                    }
                    cursorPosition.x = 0;
                    return;
                }

                if (sequence.endsWith("F")) {
                    int moveAmount = Integer.parseInt(subSequence);
                    if (moveAmount <= 0) {
                        moveAmount = 1;
                    }

                    cursorPosition.y -= moveAmount;
                    if (cursorPosition.y < 0) {
                        cursorPosition.y = 0;
                    }
                    cursorPosition.x = 0;
                    return;
                }

                if (sequence.endsWith("G")) {
                    int moveAmount = Integer.parseInt(subSequence);
                    if (moveAmount < 0) {
                        moveAmount = 0;
                    }

                    cursorPosition.y = moveAmount;
                    if (cursorPosition.y >= textSize.width) {
                        cursorPosition.y = textSize.width - 1;
                    }
                    return;
                }

                if (sequence.endsWith("H")) {
                    String bits[] = subSequence.split(";");
                    if (bits.length == 2) {
                        if (bits[0] == "") bits[0] = "1";
                        if (bits[1] == "") bits[1] = "1";
                        cursorPosition = new Point(
                            Integer.parseInt(bits[1]),
                            Integer.parseInt(bits[0])
                        );
                        if (cursorPosition.x >= textSize.width) {
                            cursorPosition.x = textSize.width - 1;
                        }
                        if (cursorPosition.y >= textSize.height) {
                            cursorPosition.y = textSize.height - 1;
                        }
                    }
                    return;
                }

                if (sequence.endsWith("m")) {
                    int code = Integer.parseInt(subSequence);
                    switch (code) {
                        case 0:
                            brightness = 0;
                            break;
                        case 1:
                            brightness = 1;
                            break;
                        case 2:
                            brightness = -1;
                            break;
                        case 30:
                            color = BLACK;
                            break;
                        case 31:
                            color = RED;
                            break;
                        case 32:
                            color = GREEN;
                            break;
                        case 33:
                            color = YELLOW;
                            break;
                        case 34:
                            color = BLUE;
                            break;
                        case 35:
                            color = MAGENTA;
                            break;
                        case 36:
                            color = CYAN;
                            break;
                        case 37:
                            color = WHITE;
                            break;
                        case 40:
                            background = BLACK;
                            break;
                        case 41:
                            background = RED;
                            break;
                        case 42:
                            background = GREEN;
                            break;
                        case 43:
                            background = YELLOW;
                            break;
                        case 44:
                            background = BLUE;
                            break;
                        case 45:
                            background = MAGENTA;
                            break;
                        case 46:
                            background = CYAN;
                            break;
                        case 47:
                            background = WHITE;
                            break;
                    
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Dimension getPreferredSize() {
        return screenSize;
    }


    public Dimension getMinimumSize() {
        return screenSize;
    }


    public Dimension getMaximumSize() {
        return screenSize;
    }

    public void keyTyped(KeyEvent k) {
        if (keyPressConsumer != null) {
            keyPressConsumer.message(Character.toString(k.getKeyChar()), 1);
        }
    }

    public void keyPressed(KeyEvent k) {
        if (keyPressConsumer != null) {
            switch (k.getKeyCode()) {
                case KeyEvent.VK_UP:
                    keyPressConsumer.message(ESC + "[A");
                    break;
                case KeyEvent.VK_DOWN:
                    keyPressConsumer.message(ESC + "[B");
                    break;
                case KeyEvent.VK_RIGHT:
                    keyPressConsumer.message(ESC + "[C");
                    break;
                case KeyEvent.VK_LEFT:
                    keyPressConsumer.message(ESC + "[D");
                    break;
            }
        }
    }

    public void keyReleased(KeyEvent k) {
    }

    public void mouseClicked(MouseEvent e) {
        //Since the user clicked on us, let us get focus!
        requestFocusInWindow();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void focusGained(FocusEvent e) {
        hasFocus = true;
        repaint();
    }

    public void focusLost(FocusEvent e) {
        hasFocus = false;
        repaint();
    }

}
