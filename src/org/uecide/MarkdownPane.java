package org.uecide;

import javax.swing.*;
import java.awt.*;
import org.markdown4j.Markdown4jProcessor;

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

    void renderIt() {
        try {
            setContentType("text/html");
            setEditable(false);
            String fp = Base.getTheme().getFontCSS("editor.markdown.font.p");
            String fli = Base.getTheme().getFontCSS("editor.markdown.font.li");
            String fh1 = Base.getTheme().getFontCSS("editor.markdown.font.h1");
            String fh2 = Base.getTheme().getFontCSS("editor.markdown.font.h2");
            String fh3 = Base.getTheme().getFontCSS("editor.markdown.font.h3");
            String fpre = Base.getTheme().getFontCSS("editor.markdown.font.pre");

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
