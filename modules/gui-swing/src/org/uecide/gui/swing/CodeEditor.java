package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;
import org.uecide.SketchFile;
import org.uecide.Preferences;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.Debug;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.CaretStyle;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;


public class CodeEditor extends TabPanel implements ContextEventListener, FocusListener, CopyAndPaste {
    Context ctx;
    SketchFile file;

    RTextScrollPane scrollPane;
    RSyntaxTextArea textArea;
    AbstractDocument document;
    Gutter gutter;
    SyntaxScheme scheme;

    JPanel tabPanel = null;
    JLabel tabLabel = null;

    int savedCaretPosition = 0;

    int fontScale = 100;

    public CodeEditor(Context c, AutoTab def, SketchFile f) {
        super(f.getFile().getName(), def);
        ctx = c;
        file = f;

        document = f.getDocument();
        if (!(document instanceof RSyntaxDocument)) {
            f.promoteDocument(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
            document = f.getDocument();
        }
        textArea = new RSyntaxTextArea((RSyntaxDocument)document);
        scrollPane = new RTextScrollPane(textArea);
        gutter = scrollPane.getGutter();
        add(scrollPane, BorderLayout.CENTER);
        textArea.addFocusListener(this);
        textArea.setText(f.getFileData());
        textArea.requestFocus();
        textArea.setAntiAliasingEnabled(Preferences.getBoolean("theme.editor.fonts.editor_aa"));

        ctx.listenForEvent("sketchDataModified", this);
        ctx.listenForEvent("saveCursorLocation", this);
        ctx.listenForEvent("restoreCursorLocation", this);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gotoLine(1);
            }
        });

        refreshSettings();
    }

    public SketchFile getSketchFile() {
        return file;
    }

    public void flushData() {
//        file.setFileData(textArea.getText());
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                tabPanel.repaint();
//                tabPanel.revalidate();
//            }
//        });
    }

    public void requestFocus() {
        textArea.requestFocus();
    }

    @Override
    public Component getTab() {
        if (tabPanel == null) {
            tabPanel = new JPanel();
            tabPanel.setOpaque(false);
            tabPanel.setLayout(new BorderLayout());
            tabLabel = new JLabel(file.getFile().getName());
            tabLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
            tabPanel.add(tabLabel, BorderLayout.CENTER);
            tabLabel.setOpaque(false);
            try {
                JLabel ico = new JLabel("");
                ico.setOpaque(false);
//                ico.setIcon(IconManager.getIcon(16, "mime." + FileType.getIcon(file.getFile().getName())));
                tabPanel.add(ico, BorderLayout.WEST);
            } catch (Exception ex) { 
                Debug.exception(ex);
            }
            
            try {
                JButton ico = new JButton(IconManager.getIcon(16, "tabs.close"));
                ico.setBorderPainted(false);
                ico.setFocusPainted(false);
                ico.setContentAreaFilled(false);
                ico.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ctx.action("closeSketchFile", file);
                    }
                });
                tabPanel.add(ico, BorderLayout.EAST);
            } catch (Exception ex) { 
                Debug.exception(ex);
            }
        }

        JLabel fl = new JLabel("");
        Font basefont = fl.getFont();

        Font newfont;

        if (file.isModified()) {
            newfont = basefont.deriveFont(Font.BOLD);
        } else {
            newfont = basefont.deriveFont(Font.PLAIN);
        }
        tabLabel.setFont(newfont);

        return tabPanel;
    }

    public void gotoLine(int lineno) {
        lineno--;
        if  (lineno < 0) lineno = 0;
        try {
            textArea.setCaretPosition(textArea.getLineStartOffset(lineno));
        } catch(BadLocationException e) {
            Debug.exception(e);
        }
    }

    public void contextEventTriggered(ContextEvent evt) {
        if (evt.getEvent().equals("sketchDataModified")) {
//            tabPanel.getTopLevelAncestor().revalidate();
//            tabPanel.getTopLevelAncestor().repaint();
        }

        if (evt.getEvent().equals("saveCursorLocation")) {
            SketchFile sf = (SketchFile)evt.getObject();
            if (sf == file) {
                savedCaretPosition = textArea.getCaretPosition();
            }
        }

        if (evt.getEvent().equals("restoreCursorLocation")) {
            SketchFile sf = (SketchFile)evt.getObject();
            if (sf == file) {
                textArea.setCaretPosition(savedCaretPosition);
            }
        }

    }

    public void focusGained(FocusEvent evt) {
        ((SwingGui)ctx.getGui()).setActiveTab(this);
    }

    public void focusLost(FocusEvent evt) {
    }


    public void copy() { textArea.copy(); }
    public void cut() { textArea.cut(); }
    public void paste() { textArea.paste(); }
    public void selectAll() { textArea.selectAll(); }
    public void copyAll(String prefix, String suffix) {
        StringSelection selection = new StringSelection(prefix + "\n" + file.getFileData() + "\n" + suffix + "\n");
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    public void undo() { textArea.undoLastAction(); }
    public void redo() { textArea.redoLastAction(); }


    public void refreshSettings() {
        boolean external = Preferences.getBoolean("editor.external");
        textArea.setEditable(!external);
        textArea.setCodeFoldingEnabled(true);
//        toolbar.setVisible(!Preferences.getBoolean("editor.toolbars.sub_hidden") && !Preferences.getBoolean("editor.layout.minimal"));

        textArea.setMarkOccurrences(Preferences.getBoolean("editor.mark"));

        if(Preferences.get("editor.tabs.size") != null) {
            int ts = Preferences.getInteger("editor.tabs.size");
            if (ts == 0) {
                ts = 4;
            }
            textArea.setTabSize(ts);
        } else {
            textArea.setTabSize(4);
        }

// setMargin doesn't work for the MaterialLAF at the moment.
        textArea.setMargin(new Insets(
            Preferences.getInteger("theme.editor.margins.top"),
            Preferences.getInteger("theme.editor.margins.left"),
            Preferences.getInteger("theme.editor.margins.bottom"),
            Preferences.getInteger("theme.editor.margins.right")
        ));

        textArea.setTabsEmulated(Preferences.getBoolean("editor.tabs.expand"));
        textArea.setPaintTabLines(Preferences.getBoolean("editor.tabs.show"));

        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.setIconRowHeaderEnabled(true);
        setBackground(Preferences.getColor("theme.editor.colors.background"));
        textArea.setBackground(Preferences.getColor("theme.editor.colors.background"));

        textArea.setForeground(Preferences.getColor("theme.editor.colors.foreground"));
        textArea.setFont(Preferences.getScaledFont("theme.editor.fonts.default.font", fontScale));

        gutter = scrollPane.getGutter();


        gutter.setLineNumberFont(Preferences.getScaledFont("theme.editor.gutter.font", fontScale));
        gutter.setBackground(Preferences.getColor("theme.editor.gutter.background"));
        gutter.setLineNumberColor(Preferences.getColor("theme.editor.gutter.foreground"));
        gutter.setBorderColor(Preferences.getColor("theme.editor.gutter.foreground"));
        gutter.setFoldBackground(Preferences.getColor("theme.editor.gutter.background"));
        gutter.setFoldIndicatorForeground(Preferences.getColor("theme.editor.gutter.foreground"));

        textArea.setCurrentLineHighlightColor(Preferences.getColor("theme.editor.highlight.linecolor"));
        textArea.setFadeCurrentLineHighlight(Preferences.getBoolean("theme.editor.highlight.linefade"));
        textArea.setHighlightCurrentLine(Preferences.getBoolean("theme.editor.highlight.lineenabled"));
        textArea.setRoundedSelectionEdges(Preferences.getBoolean("theme.editor.highliht.rounded"));

        textArea.setCaretColor(Preferences.getColor("theme.editor.caret.color"));

        String caretStyle = Preferences.get("theme.editor.caret.insert");

        if(caretStyle.equals("box")) {
            textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.BLOCK_BORDER_STYLE);
        } else if(caretStyle.equals("block")) {
            textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.BLOCK_STYLE);
        } else if(caretStyle.equals("line")) {
            textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.VERTICAL_LINE_STYLE);
        } else if(caretStyle.equals("thick")) {
            textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.THICK_VERTICAL_LINE_STYLE);
        } else if(caretStyle.equals("underline")) {
            textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.UNDERLINE_STYLE);
        }

        caretStyle = Preferences.get("theme.editor.caret.replace");
        if(caretStyle.equals("box")) {
            textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.BLOCK_BORDER_STYLE);
        } else if(caretStyle.equals("block")) {
            textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.BLOCK_STYLE);
        } else if(caretStyle.equals("line")) {
            textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.VERTICAL_LINE_STYLE);
        } else if(caretStyle.equals("thick")) {
            textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.THICK_VERTICAL_LINE_STYLE);
        } else if(caretStyle.equals("underline")) {
            textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.UNDERLINE_STYLE);
        }

        textArea.setMarkOccurrencesColor(Preferences.getColor("theme.editor.highlight.markall"));
        textArea.setMarkAllHighlightColor(Preferences.getColor("theme.editor.highlight.markall"));
        textArea.setPaintMarkOccurrencesBorder(Preferences.getBoolean("theme.editor.highlight.markallborder"));

        textArea.setMatchedBracketBGColor(Preferences.getColor("theme.editor.highlight.bracketbg"));
        textArea.setMatchedBracketBorderColor(Preferences.getColor("theme.editor.highlight.bracketborder"));
        textArea.setPaintMatchedBracketPair(Preferences.getBoolean("theme.editor.highlight.border"));
        textArea.setSelectionColor(Preferences.getColor("theme.editor.highlight.select"));


        scheme = textArea.getSyntaxScheme();
        applyThemeSettings();

        textArea.setAntiAliasingEnabled(false);
        gutter.setAntiAliasingEnabled(false);
        textArea.setAntiAliasingEnabled(Preferences.getBoolean("theme.editor.fonts.editor_aa"));
        gutter.setAntiAliasingEnabled(Preferences.getBoolean("theme.editor.fonts.editor_aa"));
    }

    public void applyThemeFont(int section, String type) {
        applyThemeFGColor(section, "theme.editor.fonts." + type + ".foreground");
        applyThemeBGColor(section, "theme.editor.fonts." + type + ".background");
        applyThemeFontFace(section, "theme.editor.fonts." + type + ".font");
        applyThemeUnderline(section, "theme.editor.fonts." + type + ".underline");
        applyThemeBold(section, "theme.editor.fonts." + type + ".bold");
    }

    public void applyThemeBGColor(int index, String cs) {
        String cspec = Preferences.get(cs);
        if (cspec == null) cspec = "transparent";
        if (cspec.equals("")) cspec = "transparent";
        if (cspec.equals("none")) cspec = "transparent";
        if (cspec.equals("default")) cspec = "transparent";

        Color c;

        if (cspec.equals("transparent")) {
            c = new Color(0f, 0f, 0f, 0f);
        } else {
            c = Preferences.getColor(cs);
        }

        org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
        s.background = c;
        scheme.setStyle(index, s);
    }

    public void applyThemeFontFace(int index, String fs) {
        String fspec = Preferences.get(fs);
        Font f = Preferences.getScaledFont(fs, fontScale);
        if (fspec == null) {
            f = Preferences.getScaledFont("theme.editor.fonts.default.font", fontScale);
        } else if (fspec.equals("default")) {
            f = Preferences.getScaledFont("theme.editor.fonts.default.font", fontScale);
        } else if (fspec.equals("")) {
            f = Preferences.getScaledFont("theme.editor.fonts.default.font", fontScale);
        }

        org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
        s.font = f;
        scheme.setStyle(index, s);
    }

    public void applyThemeUnderline(int index, String us) {
        boolean u = Preferences.getBoolean(us);
        org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
        s.underline = u;
        scheme.setStyle(index, s);
    }

    public void applyThemeBold(int index, String us) {
        boolean b = Preferences.getBoolean(us);
        org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
        Font f = s.font;
        if (b) {
            s.font = f.deriveFont(Font.BOLD);
        }
        scheme.setStyle(index, s);
    }
    public void applyThemeFGColor(int index, String cs) {
        String cspec = Preferences.get(cs);
        if (cspec == null) return;
        if (cspec.equals("transparent")) return;
        if (cspec.equals("")) return;
        if (cspec.equals("none")) return;

        if (cspec.equals("default")) {
            cs = "theme.editor.colors.foreground";
        }

        Color c = Preferences.getColor(cs);
        org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
        s.foreground = c;
        scheme.setStyle(index, s);
    }

    public void applyThemeSettings() {

        applyThemeFont(SyntaxScheme.ANNOTATION, "annotation");
        applyThemeFont(SyntaxScheme.COMMENT_DOCUMENTATION,  "comment");
        applyThemeFont(SyntaxScheme.COMMENT_EOL,  "comment");
        applyThemeFont(SyntaxScheme.COMMENT_KEYWORD, "comment");
        applyThemeFont(SyntaxScheme.COMMENT_MARKUP, "comment");
        applyThemeFont(SyntaxScheme.COMMENT_MULTILINE, "comment");
        applyThemeFont(SyntaxScheme.MARKUP_TAG_NAME, "tags");
        applyThemeFont(SyntaxScheme.DATA_TYPE, "datatype");
        applyThemeFont(SyntaxScheme.ERROR_CHAR, "error");
        applyThemeFont(SyntaxScheme.ERROR_IDENTIFIER, "error");
        applyThemeFont(SyntaxScheme.ERROR_NUMBER_FORMAT, "error");
        applyThemeFont(SyntaxScheme.ERROR_STRING_DOUBLE, "error");
        applyThemeFont(SyntaxScheme.FUNCTION, "function");
        applyThemeFont(SyntaxScheme.IDENTIFIER, "identifier");
        applyThemeFont(SyntaxScheme.LITERAL_BACKQUOTE, "literal");
        applyThemeFont(SyntaxScheme.LITERAL_BOOLEAN, "literal");
        applyThemeFont(SyntaxScheme.LITERAL_CHAR, "literal");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_BINARY, "literal");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT, "literal");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_FLOAT, "literal");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL, "literal");
        applyThemeFont(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE, "literal");
        applyThemeFont(SyntaxScheme.OPERATOR, "operator");
        applyThemeFont(SyntaxScheme.PREPROCESSOR, "preprocessor");
        applyThemeFont(SyntaxScheme.REGEX, "regex");
        applyThemeFont(SyntaxScheme.RESERVED_WORD, "reserved");
        applyThemeFont(SyntaxScheme.RESERVED_WORD_2, "reserved");
        applyThemeFont(SyntaxScheme.VARIABLE, "variable");
        applyThemeFont(SyntaxScheme.SEPARATOR, "brackets");

        textArea.setSyntaxScheme(scheme);
    }


    @Override
    public void refreshPanel() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                refreshSettings();
            }
        });
    }

}
