package org.uecide;

import javax.swing.*;
import java.awt.*;
import org.markdown4j.Markdown4jProcessor;
import java.util.*;

public class MarkdownPane extends JTextPane {

    String content;

    public MarkdownPane(String text) {
        content = text;
        renderIt();
    }

    public MarkdownPane() {
        content = "";
        renderIt();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (Preferences.getBoolean("theme.fonts.editor_aa")) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
    }

    @Override
    public void setText(String text) {
        content = text;
        renderIt();
    }

    String generateCSSForFont(Font f, Color c) {
        HashMap<String, String> values = new HashMap<String, String>();

        values.put("font-family", f.getFamily());
        values.put("font-size", f.getSize() + "px"); 

        if (f.isBold()) {
            values.put("font-weight", "bold");
        }

        if (f.isItalic()) {
            values.put("font-style", "italic");
        }

        values.put("color", String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));

        StringBuilder out = new StringBuilder();

        for (String k : values.keySet()) {
            out.append(k);
            out.append(": ");
            out.append(values.get(k));
            out.append(";\n");
        }

        return out.toString();
        
    }

    void renderIt() {
        try {
            setContentType("text/html");
            setEditable(false);
            String fp = generateCSSForFont(Preferences.getFont("theme.markdown.font.p"), Preferences.getColor("theme.markdown.color.p"));
            String fli = generateCSSForFont(Preferences.getFont("theme.markdown.font.li"), Preferences.getColor("theme.markdown.color.li"));
            String fh1 = generateCSSForFont(Preferences.getFont("theme.markdown.font.h1"), Preferences.getColor("theme.markdown.color.h1"));
            String fh2 = generateCSSForFont(Preferences.getFont("theme.markdown.font.h2"), Preferences.getColor("theme.markdown.color.h2"));
            String fh3 = generateCSSForFont(Preferences.getFont("theme.markdown.font.h3"), Preferences.getColor("theme.markdown.color.h3"));
            String fpre = generateCSSForFont(Preferences.getFont("theme.markdown.font.pre"), Preferences.getColor("theme.markdown.color.pre"));
            String html = new Markdown4jProcessor()
                .addHtmlAttribute("style", fpre, "pre")
                .addHtmlAttribute("style", fp, "p")
                .addHtmlAttribute("style", fli, "li")
                .addHtmlAttribute("style", fh1, "h1")
                .addHtmlAttribute("style", fh2, "h2")
                .addHtmlAttribute("style", fh3, "h3")
                .process(content);

            super.setText("<html><body>" + html + "</body></html>");
        } catch (Exception e) {
        }
    }
}
