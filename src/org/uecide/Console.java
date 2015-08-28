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

        setStyle(body, "body");
        setStyle(warning, "warning");
        setStyle(error, "error");
        setStyle(heading, "heading");
        setStyle(command, "command");
        setStyle(bullet, "bullet");
        setStyle(bullet2, "bullet2");
        setStyle(link, "link");

        document = new BufferedStyledDocument(10000, 2000);
        document.setParagraphAttributes(0, 0, body, true);

        setDocument(document);
        setEditable(false);
        String theme = Preferences.get("theme.editor");
        theme = "theme." + theme + ".";
        Color bgColor = Base.theme.getColor(theme + "console.color");

        setBackground(bgColor);
        addMouseListener(new TextClickListener());
        addMouseMotionListener(new TextMotionListener());
    }   

    void setStyle(MutableAttributeSet set, String name) {
        String theme = Preferences.get("theme.editor");
        theme = "theme." + theme + ".";
        Color bgColor = Base.theme.getColor(theme + "console.color");

        Font font = getFont(name);
        StyleConstants.setBackground(set, bgColor);
        StyleConstants.setForeground(set, getColor(name));
        StyleConstants.setFontSize(set, font.getSize());
        StyleConstants.setFontFamily(set, font.getFamily());
        StyleConstants.setBold(set, font.isBold());
        StyleConstants.setItalic(set, font.isItalic());
        StyleConstants.setUnderline(set, Base.theme.getBoolean(theme + "console." + name + ".underline"));
        StyleConstants.setAlignment(set, StyleConstants.ALIGN_LEFT);
        StyleConstants.setLeftIndent(set, getIndent(name));
    }

    Color getColor(String name) {
        String theme = Preferences.get("theme.editor");
        theme = "theme." + theme + ".";

        Color color = Base.theme.getColor(theme + "console.output.color");
        if (Base.theme.get(theme + "console." + name + ".color") != null) {
            color = Base.theme.getColor(theme + "console." + name + ".color");
        }
        return color;
    }

    Font getFont(String name) {
        String theme = Preferences.get("theme.editor");
        theme = "theme." + theme + ".";

        Font font = Base.theme.getFont(theme + "theme.fonts.console");
        if (Base.theme.get(theme + "console." + name + ".font") != null) {
            font = Base.theme.getFont(theme + "console." + name + ".font");
        }
        return font;
    }

    int getIndent(String name) {
        String theme = Preferences.get("theme.editor");
        theme = "theme." + theme + ".";

        int indent = Base.theme.getInteger(theme + "console.indent", 5);
        if (Base.theme.get(theme + "console." + name + ".indent") != null) {
            indent = Base.theme.getInteger(theme + "console." + name + ".indent");
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
