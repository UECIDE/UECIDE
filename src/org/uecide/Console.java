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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.regex.*;

public class Console extends JTextPane {
    MutableAttributeSet body = new SimpleAttributeSet();
    MutableAttributeSet warning = new SimpleAttributeSet();
    MutableAttributeSet error = new SimpleAttributeSet();
    MutableAttributeSet heading = new SimpleAttributeSet();
    MutableAttributeSet command = new SimpleAttributeSet();
    MutableAttributeSet bullet = new SimpleAttributeSet();
    MutableAttributeSet bullet2 = new SimpleAttributeSet();
    MutableAttributeSet link = new SimpleAttributeSet();

    public final static int BODY = 1;
    public final static int WARNING = 2;
    public final static int ERROR = 3;
    public final static int HEADING = 4;
    public final static int COMMAND = 5;
    public final static int BULLET = 6;
    public final static int LINK = 7;
    public final static int BULLET2 = 8;

    BufferedStyledDocument document;

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
    }   

    @Override
    public void paintComponent(Graphics g) {
        if (Preferences.getBoolean("theme.fonts.editor_aa")) {
            Graphics2D graphics2d = (Graphics2D) g;
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
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
        setStyle(link, "link");

        setBackground(Base.getTheme().getColor("console.color"));

    }

    void setStyle(MutableAttributeSet set, String name) {
        PropertyFile theme = Base.getTheme();
        Color bgColor = theme.getColor("console.color");

        Font font = getFont(name);
        StyleConstants.setBackground(set, bgColor);
        StyleConstants.setForeground(set, getColor(name));
        StyleConstants.setFontSize(set, font.getSize());
        StyleConstants.setFontFamily(set, font.getFamily());
        StyleConstants.setBold(set, font.isBold());
        StyleConstants.setItalic(set, font.isItalic());
        StyleConstants.setUnderline(set, Base.theme.getBoolean("console." + name + ".underline"));
        StyleConstants.setAlignment(set, StyleConstants.ALIGN_LEFT);
        StyleConstants.setLeftIndent(set, getIndent(name));
    }

    Color getColor(String name) {
        PropertyFile theme = Base.getTheme();

        Color color = theme.getColor("console.output.color");
        if (theme.get("console." + name + ".color") != null) {
            color = theme.getColor("console." + name + ".color");
        }
        return color;
    }

    Font getFont(String name) {
        PropertyFile theme = Base.getTheme();

        Font font = Preferences.getFontNatural("theme.fonts.console");

        System.err.println("Font class: " + name);

        if (theme.get("console." + name + ".font") != null) {
            System.err.println("Looking for theme console font");
            font = theme.getFontNatural("console." + name + ".font");
        }
        System.err.println(font);
        return font;
    }

    int getIndent(String name) {
        PropertyFile theme = Base.getTheme();

        int indent = theme.getInteger("console.indent", 5);
        if (theme.get("console." + name + ".indent") != null) {
            indent = theme.getInteger("console." + name + ".indent");
        }
        return indent;
    }

    void doAppendString(String message, MutableAttributeSet type) {
        try {
            String[] chars = message.split("(?!^)");
            for (String c : chars) {
                if (c.equals("\010")) {
                    document.remove(document.getEndPosition().getOffset()-1, 1);
                } else {
                    document.appendString(c, type);
                }
            }
            document.insertAll();
            setCaretPosition(document.getLength());
        } catch (Exception e) {
            e.printStackTrace();
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
            doAppendString("\u2022 " + message, bullet);
        } else if (type == BULLET2) {
            doAppendString("\u2023 " + message, bullet2);
        } else if (type == LINK) {
            String[] chunks = message.split("\\|");
            link.addAttribute(LINK_ATTRIBUTE, new URLLinkAction(chunks[0]));
            doAppendString(chunks[1], link);
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
        public void mouseClicked(MouseEvent e) {
            try {
                Element elem = document.getCharacterElement(Console.this.viewToModel(e.getPoint()));
                AttributeSet as = elem.getAttributes();
                URLLinkAction fla = (URLLinkAction)as.getAttribute(LINK_ATTRIBUTE);
                if (fla != null) {
                    fla.execute();
                }
            } catch(Exception x) {
            }
        }
    }

    private class TextMotionListener extends MouseInputAdapter {
        public void mouseMoved(MouseEvent e) {
            Element elem = document.getCharacterElement(Console.this.viewToModel(e.getPoint()));
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
}
