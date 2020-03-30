package org.uecide.gui.swing;

import org.uecide.Debug;
import org.uecide.Preferences;

import org.markdown4j.Markdown4jProcessor;

import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.util.HashMap;

public class MarkdownPane extends JTextPane {

    String content;
    String styleSheet = "";

    public MarkdownPane(String text, String ss) {
        content = text;
        styleSheet = ss;
        renderIt();
    }

    public MarkdownPane(String text) {
        content = text;
        styleSheet = "";
        renderIt();
    }

    public MarkdownPane() {
        content = "";
        styleSheet = "";
        renderIt();
    }

    public void setStyleSheet(String ss) {
        styleSheet = ss;
        renderIt();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
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

    String generateBackgroundColor(Color c) {
        return String.format("background-color: #%02x%02x%02x;", c.getRed(), c.getGreen(), c.getBlue());
    }

    void renderIt() {
        try {
            setContentType("text/html");
            setEditable(false);
//            setBackground(Preferences.getColor("theme.markdown.color.background"));
            setOpaque(false);
            String body = generateBackgroundColor(Preferences.getColor("theme.markdown.color.background"));
            String fp = generateCSSForFont(Preferences.getFont("theme.markdown.font.p"), Preferences.getColor("theme.markdown.color.p"));
            String fli = generateCSSForFont(Preferences.getFont("theme.markdown.font.li"), Preferences.getColor("theme.markdown.color.li"));
            String fh1 = generateCSSForFont(Preferences.getFont("theme.markdown.font.h1"), Preferences.getColor("theme.markdown.color.h1"));
            String fh2 = generateCSSForFont(Preferences.getFont("theme.markdown.font.h2"), Preferences.getColor("theme.markdown.color.h2"));
            String fh3 = generateCSSForFont(Preferences.getFont("theme.markdown.font.h3"), Preferences.getColor("theme.markdown.color.h3"));
            String fpre = generateCSSForFont(Preferences.getFont("theme.markdown.font.pre"), Preferences.getColor("theme.markdown.color.pre"));
//                .addHtmlAttribute("style", body, "body")
            String html = new Markdown4jProcessor()
                .process(content);
//                .addHtmlAttribute("style", fpre, "pre")
//                .addHtmlAttribute("style", fp, "p")
//                .addHtmlAttribute("style", fli, "li")
//                .addHtmlAttribute("style", fh1, "h1")
//                .addHtmlAttribute("style", fh2, "h2")
//                .addHtmlAttribute("style", fh3, "h3")

            html = html.replaceAll("<br  />", "");

            HTMLEditorKit kit = new HTMLEditorKit();
            setEditorKit(kit);
            StyleSheet ss = kit.getStyleSheet();

            ss.addRule(styleSheet);

            Document doc = kit.createDefaultDocument();
            setDocument(doc);
            super.setText("<html><body>" + html + "</body></html>");
        } catch (Exception e) {
            Debug.exception(e);
        }
    }
}
