/*
 * Copyright (c) 2015, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide;

import org.uecide.plugin.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.regex.*;
import java.awt.image.*;

import java.awt.datatransfer.*;

public class Console extends JTextPane implements ClipboardOwner {
    MutableAttributeSet body = new SimpleAttributeSet();
    MutableAttributeSet warning = new SimpleAttributeSet();
    MutableAttributeSet error = new SimpleAttributeSet();
    MutableAttributeSet heading = new SimpleAttributeSet();
    MutableAttributeSet command = new SimpleAttributeSet();
    MutableAttributeSet bullet = new SimpleAttributeSet();
    MutableAttributeSet bullet2 = new SimpleAttributeSet();
    MutableAttributeSet bullet3 = new SimpleAttributeSet();
    MutableAttributeSet link = new SimpleAttributeSet();

    MutableAttributeSet fgBlack = new SimpleAttributeSet();
    MutableAttributeSet fgRed = new SimpleAttributeSet();
    MutableAttributeSet fgGreen = new SimpleAttributeSet();
    MutableAttributeSet fgYellow = new SimpleAttributeSet();
    MutableAttributeSet fgBlue = new SimpleAttributeSet();
    MutableAttributeSet fgMagenta = new SimpleAttributeSet();
    MutableAttributeSet fgCyan = new SimpleAttributeSet();
    MutableAttributeSet fgWhite = new SimpleAttributeSet();

    public final static int BODY = 1;
    public final static int WARNING = 2;
    public final static int ERROR = 3;
    public final static int HEADING = 4;
    public final static int COMMAND = 5;
    public final static int BULLET = 6;
    public final static int LINK = 7;
    public final static int BULLET2 = 8;
    public final static int BULLET3 = 9;

    public final static int BLACK   = 1000;
    public final static int RED     = 1001;
    public final static int GREEN   = 1002;
    public final static int YELLOW  = 1003;
    public final static int BLUE    = 1004;
    public final static int MAGENTA = 1005;
    public final static int CYAN    = 1006;
    public final static int WHITE   = 1007;

    BufferedStyledDocument document;

    BufferedImage topLeft = null;
    BufferedImage topMiddle = null;
    BufferedImage topRight = null;
    BufferedImage middleLeft = null;
    BufferedImage middleMiddle = null;
    BufferedImage middleRight = null;
    BufferedImage bottomLeft = null;
    BufferedImage bottomMiddle = null;
    BufferedImage bottomRight = null;

    boolean couldEraseLine = false;

    private final static String LINK_ATTRIBUTE = "linkact";

    Editor urlClickListener = null;

    public Console() {
        super();

        updateStyleSettings();

        document = new BufferedStyledDocument(10000, 2000);
        document.setParagraphAttributes(0, 0, body, true);

        setDocument(document);
        setEditable(false);

        addMouseListener(new TextClickListener());
        addMouseMotionListener(new TextMotionListener());
        setOpaque(true);
    }   

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (Preferences.getBoolean("theme.editor.fonts.editor_aa")) {
            Graphics2D graphics2d = (Graphics2D) g;
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        int w = getWidth();
        int h = getHeight();

        g2d.setPaint(Preferences.getColor("theme.console.background"));
        g2d.fillRect(0, 0, w, h);

        int fillStart = 0;
        int fillEnd = getHeight();

        int lineStart = 0;
        int lineEnd = getWidth();

        if (topLeft != null) {
            fillStart = topLeft.getHeight();
            lineStart = topLeft.getWidth();
            g2d.drawImage(topLeft, 0, 0, this);
        }

        if (topRight != null) {
            fillStart = topRight.getHeight();
            lineEnd = getWidth() - topRight.getWidth();
            g2d.drawImage(topRight, lineEnd, 0, this);
        }

        if (topMiddle != null) {
            fillStart = topMiddle.getHeight();
            int linePos = lineStart;
            int blockWidth = topMiddle.getWidth();
            int blockHeight = topMiddle.getHeight();
            while (linePos < lineEnd) {
                int copyWidth = Math.min(blockWidth, lineEnd - linePos);
                g2d.drawImage(topMiddle, 
                    linePos, 0, linePos + copyWidth, blockHeight,
                    0, 0, copyWidth, blockHeight,
                    this
                );
                linePos += blockWidth;
            }
        }

        if (bottomLeft != null) {
            fillEnd = getHeight() - bottomLeft.getHeight();
            lineStart = bottomLeft.getWidth();
            g2d.drawImage(bottomLeft, 0, fillEnd, this);
        }

        if (bottomRight != null) {
            fillEnd = getHeight() - bottomRight.getHeight();
            lineEnd = getWidth() - bottomRight.getWidth();
            g2d.drawImage(bottomRight, lineEnd, fillStart, this);
        }

        if (bottomMiddle != null) {
            fillEnd = getHeight() - bottomMiddle.getHeight();
            int linePos = lineStart;
            int blockWidth = bottomMiddle.getWidth();
            int blockHeight = bottomMiddle.getHeight();
            while (linePos < lineEnd) {
                int copyWidth = Math.min(blockWidth, lineEnd - linePos);
                g2d.drawImage(bottomMiddle, 
                    linePos, fillEnd, linePos + copyWidth, fillEnd + blockHeight,
                    0, 0, copyWidth, blockHeight,
                    this
                );
                linePos += blockWidth;
            }
        }

        if (middleLeft != null) {
            lineStart = middleLeft.getWidth();
            int fillPos = fillStart;
            int blockWidth = middleLeft.getWidth();
            int blockHeight = middleLeft.getHeight();
            while (fillPos < fillEnd) {
                int copyHeight = Math.min(blockHeight, fillEnd - fillPos);
                g2d.drawImage(middleLeft, 
                    0, fillPos, blockWidth, fillPos + copyHeight,
                    0, 0, blockWidth, copyHeight,
                    this
                );
                fillPos += blockHeight;
            }
        }

        if (middleRight != null) {
            lineEnd = getWidth() - middleRight.getWidth();
            int fillPos = fillStart;
            int blockWidth = middleRight.getWidth();
            int blockHeight = middleRight.getHeight();
            while (fillPos < fillEnd) {
                int copyHeight = Math.min(blockHeight, fillEnd - fillPos);
                g2d.drawImage(middleRight, 
                    lineEnd, fillPos, lineEnd + blockWidth, fillPos + copyHeight,
                    0, 0, blockWidth, copyHeight,
                    this
                );
                fillPos += blockHeight;
            }
        }

        if (middleMiddle != null) {
            int fillPos = fillStart;
            int blockWidth = middleMiddle.getWidth();
            int blockHeight = middleMiddle.getHeight();
            while (fillPos < fillEnd) {
                int copyHeight = Math.min(blockHeight, fillEnd - fillPos);
                int linePos = lineStart;
                while (linePos < lineEnd) {
                    int copyWidth = Math.min(blockWidth, lineEnd - linePos);
                    g2d.drawImage(middleMiddle,
                        linePos, fillPos, linePos + copyWidth, fillPos + copyHeight,
                        0, 0, copyWidth, copyHeight,
                        this
                    );
                    linePos += blockWidth;
                }
                fillPos += blockHeight;
            }
        }

            
                

        super.paintComponent(g);
    }

    public void updateStyleSettings() {
        setStyle(body, "body");
        setStyle(warning, "warning");
        setStyle(error, "error");
        setStyle(heading, "heading");
        setStyle(command, "command");
        setStyle(bullet, "bullet");
        setStyle(bullet2, "bullet2");
        setStyle(bullet3, "bullet3");
        setStyle(link, "link");

        StyleConstants.setForeground(fgBlack,   new Color(0, 0, 0));
        StyleConstants.setForeground(fgRed,     new Color(255, 0, 0));
        StyleConstants.setForeground(fgGreen,   new Color(0, 255, 0));
        StyleConstants.setForeground(fgYellow,  new Color(255, 255, 0));
        StyleConstants.setForeground(fgBlue,    new Color(0, 0, 255));
        StyleConstants.setForeground(fgMagenta, new Color(255, 0, 255));
        StyleConstants.setForeground(fgCyan,    new Color(0, 255, 255));
        StyleConstants.setForeground(fgWhite,   new Color(255, 255, 255));

        setBackground(new Color(1,1,1, (float) 0.01));
//        PropertyFile theme = Base.getTheme();
//        topLeft = loadImage(theme.get("console.background.image.topleft"));
//        topMiddle = loadImage(theme.get("console.background.image.topmiddle"));
//        topRight = loadImage(theme.get("console.background.image.topright"));
//        middleLeft = loadImage(theme.get("console.background.image.left"));
//        middleMiddle = loadImage(theme.get("console.background.image.middle"));
//        middleRight = loadImage(theme.get("console.background.image.right"));
//        bottomLeft = loadImage(theme.get("console.background.image.bottomleft"));
//        bottomMiddle = loadImage(theme.get("console.background.image.bottommiddle"));
//        bottomRight = loadImage(theme.get("console.background.image.bottomright"));

        repaint();
    }

    BufferedImage loadImage(String path) {
        if (path == null) {
            return null;
        }
        BufferedImage in;
        if (path.startsWith("res://")) {
            return Base.loadImageFromResource(path.substring(5));
        }
        return null;
    }

    void setStyle(MutableAttributeSet set, String name) {
//        PropertyFile theme = Base.getTheme();

        Font font = getFont(name);

        StyleConstants.setForeground(set, getColor(name));
        StyleConstants.setFontSize(set, font.getSize());
        StyleConstants.setFontFamily(set, font.getFamily());
        StyleConstants.setBold(set, font.isBold());
        StyleConstants.setItalic(set, font.isItalic());
//        StyleConstants.setUnderline(set, Base.theme.getBoolean("console." + name + ".underline"));
        StyleConstants.setAlignment(set, StyleConstants.ALIGN_LEFT);
        StyleConstants.setLeftIndent(set, getIndent(name));
    }

    Color getColor(String name) {
        Color c = Preferences.getColor("theme.console.colors." + name);
        if (c == null) {
            System.err.println("Unknown colour: theme.console.colors." + name);
        }
        return c;
    }

    Font getFont(String name) {
        Font f = Preferences.getFont("theme.console.fonts." + name);
        if (f == null) {
            System.err.println("Unknown font: theme.console.fonts." + name);
        }
        return f;
    }

    int getIndent(String name) {
        Integer i = Preferences.getInteger("theme.console.indents." + name);
        if (i == null) {
            System.err.println("Unknown indent: theme.console.indents." + name);
            return 0;
        }
        return i;
    }

    void doAppendString(String message, MutableAttributeSet type) {
        try {
            String[] chars = message.split("(?!^)");
            for (String c : chars) {
                if (c.equals("\010")) {
                    document.remove(document.getEndPosition().getOffset()-2, 1);
                    continue;
                } 
                if (c.equals("\r")) {
                    couldEraseLine = true;
                    continue;
                }
                if (!c.equals("\n") && couldEraseLine) {
                    removeLastLine();
                }
                document.appendString(c, type);
                couldEraseLine = false;
            }
            document.insertAll();
            setCaretPosition(document.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeLastLine() {
        try {
            String content = document.getText(0, document.getLength());
            int lastLineBreak = content.lastIndexOf('\n') + 1;
            document.remove(lastLineBreak, document.getLength() - lastLineBreak); 
        } catch (Exception ignored) {
        }
    }

    void append(String message, int type) {
        if (type == BODY) {
            doAppendString(message, body);
        } else if (type == WARNING) {
            doAppendString(message, warning);
        } else if (type == ERROR) {
            doAppendString(message, error);
        } else if (type == HEADING) {
            doAppendString(message, heading);
        } else if (type == COMMAND) {
            doAppendString(message, command);
        } else if (type == BULLET) {
            String bchar = Preferences.get("theme.console.characters.bullet1");
            doAppendString(bchar + " " + message, bullet);
        } else if (type == BULLET2) {
            String bchar = Preferences.get("theme.console.characters.bullet2");
            doAppendString(bchar + " " + message, bullet2);
        } else if (type == BULLET3) {
            String bchar = Preferences.get("theme.console.characters.bullet3");
            doAppendString(bchar + " " + message, bullet3);
        } else if (type == LINK) {
            String[] chunks = message.split("\\|");
            link.addAttribute(LINK_ATTRIBUTE, new URLLinkAction(chunks[0]));
            doAppendString(chunks[1], link);
        } else if (type == BLACK) {
            doAppendString(message, fgBlack);
        } else if (type == RED) {
            doAppendString(message, fgRed);
        } else if (type == GREEN) {
            doAppendString(message, fgGreen);
        } else if (type == YELLOW) {
            doAppendString(message, fgYellow);
        } else if (type == BLUE) {
            doAppendString(message, fgBlue);
        } else if (type == MAGENTA) {
            doAppendString(message, fgMagenta);
        } else if (type == CYAN) {
            doAppendString(message, fgCyan);
        } else if (type == WHITE) {
            doAppendString(message, fgWhite);
        }
    }

    // Parse a string for sub-types and append each chunk
    // to the console with the right type.  Pass the chunks through
    // append() to do the formatting.  Chunks are plain text (body),
    // or {\type text...} to embed a certain type inside the text
    public void appendParsed(String message) {

        Pattern pat = Pattern.compile("\\{\\\\(\\w+)\\s*(.*)\\}");

        int openBracketLocation = message.indexOf("{\\");
        
        // No open sequence means there's no parsing to do - just
        // append the text as BODY and leave it at that.
        if (openBracketLocation == -1) {
            append(message, BODY);
            return;
        }

        while (openBracketLocation >= 0) {
            String leftChunk = message.substring(0, openBracketLocation);
            String rightChunk = message.substring(openBracketLocation);
            int closeBracketLocation = rightChunk.indexOf("}");
            if (closeBracketLocation == -1) { 
                // Oops - something went wrong! No close bracket!
                System.err.println("Badly formatted message: " + message);
                return;
            }
            String block = rightChunk.substring(0, closeBracketLocation + 1);
            String remainder = rightChunk.substring(closeBracketLocation + 1);

            if (!leftChunk.equals("")) {
                append(leftChunk, BODY);
            }

            Matcher m = pat.matcher(block);
            if (m.find()) {
                String type = m.group(1);
                String text = m.group(2);
                if (type.equals("body")) {
                    append(text, BODY);
                } else if (type.equals("warning")) {
                    append(text, WARNING);
                } else if (type.equals("error")) {
                    append(text, ERROR);
                } else if (type.equals("command")) {
                    append(text, COMMAND);
                } else if (type.equals("heading")) {
                    append(text, HEADING);
                } else if (type.equals("bullet")) {
                    append(text, BULLET);
                } else if (type.equals("bullet2")) {
                    append(text, BULLET2);
                } else if (type.equals("bullet3")) {
                    append(text, BULLET3);
                } else if (type.equals("link")) {
                    append(text, LINK);
                }
            }
            
            message = remainder;
            openBracketLocation = message.indexOf("{\\");
        }
        if (!message.equals("")) {
            append(message, BODY);
        }
    }

    void clear() {
        try {
            document.remove(0, document.getLength());
        } catch (Exception e) {
        }
    }


    private class URLLinkAction extends AbstractAction {
        private String url;

        URLLinkAction(String bac) {
            url=bac;
        }

        protected void execute() {
            if (Console.this.urlClickListener != null) {
                Console.this.urlClickListener.urlClicked(url);
            }
        }

        public void actionPerformed(ActionEvent e) {
            execute();
        }
    }

    private class TextClickListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                JPopupMenu menu = new JPopupMenu();
                JMenuItem copy = new JMenuItem(Base.i18n.string("menu.copy"));
                menu.add(copy);
                copy.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        StringSelection ss = new StringSelection(getSelectedText());
                        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                        cb.setContents(ss, Console.this);
                    }
                });

                JMenuItem copyAll = new JMenuItem(Base.i18n.string("menu.copyall"));
                menu.add(copyAll);
                copyAll.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        StringSelection ss = new StringSelection(getText());
                        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                        cb.setContents(ss, Console.this);
                    }
                });

                if (urlClickListener != null) {
                    urlClickListener.addMenuChunk(menu, Plugin.MENU_POPUP_CONSOLE | Plugin.MENU_TOP);
                    urlClickListener.addMenuChunk(menu, Plugin.MENU_POPUP_CONSOLE | Plugin.MENU_MID);
                    urlClickListener.addMenuChunk(menu, Plugin.MENU_POPUP_CONSOLE | Plugin.MENU_BOTTOM);
                }

                menu.show(Console.this, e.getX(), e.getY());
            }
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                try {
                    Element elem = document.getCharacterElement(Console.this.viewToModel2D(e.getPoint()));
                    AttributeSet as = elem.getAttributes();
                    URLLinkAction fla = (URLLinkAction)as.getAttribute(LINK_ATTRIBUTE);
                    if (fla != null) {
                        fla.execute();
                    }
                } catch(Exception x) {
                }
            }
        }
    }

    private class TextMotionListener extends MouseInputAdapter {
        public void mouseMoved(MouseEvent e) {
            Element elem = document.getCharacterElement(Console.this.viewToModel2D(e.getPoint()));
            AttributeSet as = elem.getAttributes();
            if (StyleConstants.isUnderline(as)) {
                Console.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                Console.this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }


    public void setURLClickListener(Editor e) {
        urlClickListener = e;
    }

    @Override 
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
    }
}
