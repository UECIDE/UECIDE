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

package org.uecide.editors;

import org.uecide.*;
import org.uecide.plugin.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.JToolBar;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.modes.*;
import org.fife.ui.rtextarea.*;

import org.markdown4j.Markdown4jProcessor;


public class markdown extends JPanel implements EditorBase {
    File file = null;
    boolean modified = false;
    RSyntaxTextArea textArea;
    RSyntaxDocument rDocument;
    RTextScrollPane scrollPane;
    SyntaxScheme scheme;
    Sketch sketch;
    Editor editor;
    JPanel findPanel = null;
    JTextField searchTerm = new JTextField();
    JButton findButton = new JButton("Find");
    SearchContext search = new SearchContext();
    JCheckBox matchCase = new JCheckBox("Case");
    JCheckBox searchBackwards = new JCheckBox("Back");
    JTextField replaceWith = new JTextField();
    JButton replaceButton = new JButton("Replace");
    JButton replaceAllButton = new JButton("All");
    JButton findCloseButton;
    Gutter gutter;

    JSplitPane previewSplit;
    JScrollPane previewScroll;
    JTextPane preview;

    String fileSyntax;

    JToolBar toolbar;

    class Flag {
        Icon icon;
        int group;
        int line;

        public Flag(int l, Icon i, int g) {
            line = l;
            icon = i;
            group = g;
        }

        public int getLine() {
            return line;
        }

        public int getGroup() {
            return group;
        }

        public Icon getIcon() {
            return icon;
        }
    }

    ArrayList<Flag> flagList = new ArrayList<Flag>();

    public void openFindPanel() {
        if(findPanel == null) {
            ImageIcon closeIcon = Base.getIcon("actions", "close", 16);
            findCloseButton = new JButton(closeIcon);
            findCloseButton.setBorder(new EmptyBorder(0, 2, 0, 2));
            findCloseButton.setContentAreaFilled(false);
            findPanel = new JPanel();
            findPanel.setLayout(new BoxLayout(findPanel, BoxLayout.LINE_AXIS));

            if(Preferences.getBoolean("editor.find.keep") == false) {
                findPanel.add(findCloseButton);
            }

            findPanel.add(searchTerm);
            findPanel.add(findButton);
            findPanel.add(matchCase);
            findPanel.add(searchBackwards);
            findPanel.add(replaceWith);
            findPanel.add(replaceButton);
            findPanel.add(replaceAllButton);
            add(findPanel, BorderLayout.SOUTH);
            revalidate();
            repaint();
            findButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(findNext(searchTerm.getText(), matchCase.isSelected(), searchBackwards.isSelected())) {
                        searchTerm.setBackground(UIManager.getColor("TextField.background"));
                    } else {
                        searchTerm.setBackground(textArea.getSyntaxScheme().getStyle(SyntaxScheme.ERROR_IDENTIFIER).background);
                    }
                }
            });
            searchTerm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(findNext(searchTerm.getText(), matchCase.isSelected(), searchBackwards.isSelected())) {
                        searchTerm.setBackground(UIManager.getColor("TextField.background"));
                    } else {
                        searchTerm.setBackground(textArea.getSyntaxScheme().getStyle(SyntaxScheme.ERROR_IDENTIFIER).background);
                    }
                }
            });
            replaceWith.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    replaceNext(searchTerm.getText(), replaceWith.getText(), matchCase.isSelected(), searchBackwards.isSelected());
                }
            });
            replaceButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    replaceNext(searchTerm.getText(), replaceWith.getText(), matchCase.isSelected(), searchBackwards.isSelected());
                }
            });
            replaceAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    replaceAll(searchTerm.getText(), replaceWith.getText(), matchCase.isSelected(), searchBackwards.isSelected());
                }
            });

            findCloseButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    closeFindAndReplace();
                }
            });
        }

        searchTerm.requestFocus();
    }

    public void closeFindAndReplace() {
        remove(findPanel);
        findPanel = null;
        revalidate();
        repaint();
    }

    public boolean findNext(String text, boolean mc, boolean back) {
        search.setSearchFor(text);
        search.setMatchCase(mc);
        search.setSearchForward(!back);
        SearchResult res = SearchEngine.find(textArea, search);

        if(!res.wasFound()) {
            textArea.setCaretPosition(0);
            int cp = textArea.getCaretPosition();
            res = SearchEngine.find(textArea, search);

            if(!res.wasFound()) {
                textArea.setCaretPosition(cp);
                return false;
            }
        }

        return true;
    }

    public void replaceNext(String text, String rep, boolean mc, boolean back) {
        search.setSearchFor(text);
        search.setReplaceWith(rep);
        search.setMatchCase(mc);
        search.setSearchForward(!back);
        SearchEngine.replace(textArea, search);
    }

    public void replaceAll(String text, String rep, boolean mc, boolean back) {
        search.setSearchFor(text);
        search.setReplaceWith(rep);
        search.setMatchCase(mc);
        search.setSearchForward(!back);
        SearchEngine.replaceAll(textArea, search);
    }

    public boolean updateRunning = false;

    public markdown(Sketch s, File f, Editor e) {
        editor = e;
        sketch = s;
        this.setLayout(new BorderLayout());

        rDocument = new RSyntaxDocument(FileType.getSyntaxStyle(f.getName()));

        textArea = new RSyntaxTextArea(rDocument) {
            public String getToolTipText(MouseEvent e) {
                try {
                    HashMap<Integer, String> comments = sketch.getLineComments(file);

                    if(comments == null) {
                        return null;
                    }

                    for(int line : comments.keySet()) {
                        int y = textArea.yForLine(line - 1);

                        if(y == -1) {
                            continue;
                        }

                        int z = y + textArea.getLineHeight();

                        if(e.getY() > y && e.getY() < z) {
                            return comments.get(line);
                        }
                    }
                } catch(Exception ex) {
                }

                return null;
            }
        };

        textArea.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) { }
            public void keyReleased(KeyEvent e) { }
            public void keyTyped(KeyEvent e) {
                if (updateRunning) {
                    return;
                }
                updateRunning = true;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updatePreview();
                        updateRunning = false;
                    }
                });
            }
        });

        previewScroll = new JScrollPane();
        preview = new JTextPane() {
            @Override
            protected void paintComponent(Graphics g) {
                if (Preferences.getBoolean("theme.fonts.editor_aa")) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                }
                super.paintComponent(g);
            }
        };
        preview.setContentType("text/html");
        preview.setEditable(false);

        previewScroll.setViewportView(preview);

        fileSyntax = FileType.getSyntaxStyle(f.getName());
        if (fileSyntax == null) {
            fileSyntax = SyntaxConstants.SYNTAX_STYLE_NONE;
        }
        textArea.setSyntaxEditingStyle(fileSyntax);

        Document d = textArea.getDocument();
        d.addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                setModified(true);
            }
            public void insertUpdate(DocumentEvent e) {
                setModified(true);
            }
            public void removeUpdate(DocumentEvent e) {
                setModified(true);
            }
        });

        scrollPane = new RTextScrollPane(textArea);
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setVisible(!Preferences.getBoolean("editor.toolbars.sub_hidden"));
        this.add(toolbar, BorderLayout.NORTH);

        Editor.addToolbarButton(toolbar, "actions", "copy", "Copy", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.copy();
            }
        });
        Editor.addToolbarButton(toolbar, "actions", "cut", "Cut", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.cut();
            }
        });

        Editor.addToolbarButton(toolbar, "actions", "paste", "Paste", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.paste();
            }
        });

        toolbar.addSeparator();
        Editor.addToolbarButton(toolbar, "actions", "undo", "Undo", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.undoLastAction();
            }
        });

        JButton redoButton = Editor.addToolbarButton(toolbar, "actions", "redo", "Redo", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.redoLastAction();
            }
        });

        toolbar.addSeparator();
        JButton indentButton = Editor.addToolbarButton(toolbar, "actions", "indent-more", "Increase Indent");
        indentButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                increaseIndent(e);
            }
        });
        JButton outdentButton = Editor.addToolbarButton(toolbar, "actions", "indent-less", "Decrease Indent");
        outdentButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                decreaseIndent(e);
            }
        });
        toolbar.addSeparator();
        editor.addPluginsToToolbar(toolbar, Plugin.TOOLBAR_TAB);

        refreshSettings();
        previewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, previewScroll);



        previewSplit.setContinuousLayout(true);
        previewSplit.setResizeWeight(0.5D);
//        previewSplit.setDividerLocation(0.5);

        this.add(previewSplit, BorderLayout.CENTER);

        if(f != null) {
            loadFile(f);
        }

        if(Preferences.getBoolean("editor.find.keep")) {
            openFindPanel();
        }

        for (int i = 0; toolbar.getComponentAtIndex(i) != null; i++) {
            Component c = toolbar.getComponentAtIndex(i);
            if (c instanceof JButton) {
                JButton b = (JButton)c;
                b.setBorderPainted(false);
            }
        }

        updatePreview();

    }

    public void requestFocus() {
        textArea.requestFocus();
//        textArea.forceCurrentLineHighlightRepaint();
    }

    public void refreshSettings() {
        PropertyFile theme = Base.getTheme();

        boolean external = Preferences.getBoolean("editor.external");
        textArea.setEditable(!external);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(false);
        textArea.setAntiAliasingEnabled(Preferences.getBoolean("theme.fonts.editor_aa"));
        textArea.setMarkOccurrences(true);

        if(Preferences.get("editor.tabs.size") != null) {
            textArea.setTabSize(Preferences.getInteger("editor.tabs.size"));
        } else {
            textArea.setTabSize(4);
        }

        textArea.setTabsEmulated(Preferences.getBoolean("editor.tabs.expand"));
        textArea.setPaintTabLines(Preferences.getBoolean("editor.tabs.show"));

        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.setIconRowHeaderEnabled(true);
        setBackground(theme.getColor("editor.bgcolor"));
        textArea.setBackground(theme.getColor("editor.bgcolor"));

        textArea.setForeground(theme.getColor("editor.fgcolor"));
        textArea.setFont(Preferences.getFont("theme.fonts.editor"));

        gutter = scrollPane.getGutter();
        gutter.setAntiAliasingEnabled(false);
        gutter.setAntiAliasingEnabled(Preferences.getBoolean("theme.fonts.editor_aa"));


        if(theme.get("editor.gutter.bgcolor") != null) {
            gutter.setBackground(theme.getColor("editor.gutter.bgcolor"));
        }

        if(theme.get("editor.gutter.fgcolor") != null) {
            gutter.setLineNumberColor(theme.getColor("editor.gutter.fgcolor"));
        }

        if(theme.get("editor.gutter.bordercolor") != null) {
            gutter.setBorderColor(theme.getColor("editor.gutter.bordercolor"));
        }

        if(theme.get("editor.fold.bgcolor") != null) {
            gutter.setFoldBackground(theme.getColor("editor.fold.bgcolor"));
        }

        if(theme.get("editor.fold.fgcolor") != null) {
            gutter.setFoldIndicatorForeground(theme.getColor("editor.fold.fgcolor"));
        }

        if(theme.get("editor.line.bgcolor") != null) {
            textArea.setCurrentLineHighlightColor(theme.getColor("editor.line.bgcolor"));
        }

        textArea.setFadeCurrentLineHighlight(theme.getBoolean("editor.line.fade"));
        textArea.setHighlightCurrentLine(theme.getBoolean("editor.line.enabled"));
        textArea.setRoundedSelectionEdges(theme.getBoolean("editor.select.rounded"));

        if(theme.get("editor.caret.fgcolor") != null) {
            textArea.setCaretColor(theme.getColor("editor.caret.fgcolor"));
        }

        if(theme.get("editor.caret.style.insert") != null) {
            if(theme.get("editor.caret.style.insert").equals("box")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.BLOCK_BORDER_STYLE);
            }

            if(theme.get("editor.caret.style.insert").equals("block")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.BLOCK_STYLE);
            }

            if(theme.get("editor.caret.style.insert").equals("line")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.VERTICAL_LINE_STYLE);
            }

            if(theme.get("editor.caret.style.insert").equals("thick")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.THICK_VERTICAL_LINE_STYLE);
            }

            if(theme.get("editor.caret.style.insert").equals("underline")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.UNDERLINE_STYLE);
            }
        }

        if(theme.get("editor.caret.style.replace") != null) {
            if(theme.get("editor.caret.style.replace").equals("box")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.BLOCK_BORDER_STYLE);
            }

            if(theme.get("editor.caret.style.replace").equals("block")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.BLOCK_STYLE);
            }

            if(theme.get("editor.caret.style.replace").equals("line")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.VERTICAL_LINE_STYLE);
            }

            if(theme.get("editor.caret.style.replace").equals("thick")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.THICK_VERTICAL_LINE_STYLE);
            }

            if(theme.get("editor.caret.style.replace").equals("underline")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.UNDERLINE_STYLE);
            }
        }

        if(theme.get("editor.markall.bgcolor") != null) {
            textArea.setMarkOccurrencesColor(theme.getColor("editor.markall.bgcolor"));
            textArea.setMarkAllHighlightColor(theme.getColor("editor.markall.bgcolor"));
        }

        textArea.setPaintMarkOccurrencesBorder(theme.getBoolean("editor.markall.border"));

        if(theme.get("editor.bracket.bgcolor") != null) {
            textArea.setMatchedBracketBGColor(theme.getColor("editor.bracket.bgcolor"));
        }

        if(theme.get("editor.bracket.bordercolor") != null) {
            textArea.setMatchedBracketBorderColor(theme.getColor("editor.bracket.bordercolor"));
        }

        textArea.setPaintMatchedBracketPair(theme.getBoolean("editor.bracket.pair"));

        if(theme.get("editor.select.bgcolor") != null) {
            textArea.setSelectionColor(theme.getColor("editor.select.bgcolor"));
        }

        scheme = textArea.getSyntaxScheme();
        applyThemeSettings();

    }

    public boolean loadFile(File f) {
        file = f;
        textArea.setText(sketch.getFileContent(file));
        textArea.setCaretPosition(0);
        scrollTo(0);
        setModified(false);
        return true;
    }

    public boolean isModified() {
        return modified;
    }

    public String getText() {
        return textArea.getText();
    }

    public String getText(int s, int e) {
        try {
            return textArea.getText(s, e);
        } catch(Exception ex) {
            return "";
        }
    }

    public void setText(String text) {
        textArea.setText(text);
    }

    public void setModified(boolean m) {
        if(m != modified) {
            modified = m;
            int tab = editor.getTabByEditor(this);

            if(tab > -1) {
                editor.setTabModified(tab, m);
            }
        }
    }

    public void scrollTo(final int pos) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollPane.getVerticalScrollBar().setValue(pos);
            }
        });
    }

    public File getFile() {
        return file;
    }

    public void applyThemeFGColor(int index, String cs) {
        if(Base.getTheme().get(cs) != null) {
            Color c = Base.getTheme().getColor(cs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.foreground = c;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeBGColor(int index, String cs) {
        if(Base.getTheme().get(cs) != null) {
            Color c = Base.getTheme().getColor(cs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.background = c;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeFont(int index, String fs) {
        if(Base.getTheme().get(fs) != null) {
            Font f = Base.getTheme().getFont(fs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.font = f;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeUnderline(int index, String us) {
        if(Base.getTheme().get(us) != null) {
            boolean u = Base.getTheme().getBoolean(us);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.underline = u;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeSettings() {

        // Annotations
        applyThemeFGColor(SyntaxScheme.ANNOTATION,                      "editor.annotation.fgcolor");
        applyThemeBGColor(SyntaxScheme.ANNOTATION,                      "editor.annotation.bgcolor");
        applyThemeFont(SyntaxScheme.ANNOTATION,                         "editor.annotation.font");
        applyThemeUnderline(SyntaxScheme.ANNOTATION,                    "editor.annotation.underline");

        // Global comments
        applyThemeFGColor(SyntaxScheme.COMMENT_DOCUMENTATION,           "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_DOCUMENTATION,           "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_DOCUMENTATION,              "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_DOCUMENTATION,         "editor.comment.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_EOL,                     "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_EOL,                     "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_EOL,                        "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_EOL,                   "editor.comment.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_KEYWORD,                 "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_KEYWORD,                 "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_KEYWORD,                    "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_KEYWORD,               "editor.comment.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_MARKUP,                  "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_MARKUP,                  "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_MARKUP,                     "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_MARKUP,                "editor.comment.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_MULTILINE,               "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_MULTILINE,               "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_MULTILINE,                  "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_MULTILINE,             "editor.comment.underline");

        // Fine granied comments
        applyThemeFGColor(SyntaxScheme.COMMENT_DOCUMENTATION,           "editor.comment.documentation.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_DOCUMENTATION,           "editor.comment.documentation.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_DOCUMENTATION,              "editor.comment.documentation.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_DOCUMENTATION,         "editor.comment.documentation.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_EOL,                     "editor.comment.eol.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_EOL,                     "editor.comment.eol.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_EOL,                        "editor.comment.eol.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_EOL,                   "editor.comment.eol.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_KEYWORD,                 "editor.comment.keyword.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_KEYWORD,                 "editor.comment.keyword.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_KEYWORD,                    "editor.comment.keyword.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_KEYWORD,               "editor.comment.keyword.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_MARKUP,                  "editor.comment.markup.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_MARKUP,                  "editor.comment.markup.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_MARKUP,                     "editor.comment.markup.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_MARKUP,                "editor.comment.markup.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_MULTILINE,               "editor.comment.multiline.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_MULTILINE,               "editor.comment.multiline.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_MULTILINE,                  "editor.comment.multiline.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_MULTILINE,             "editor.comment.multiline.underline");

        // Data Type
        applyThemeFGColor(SyntaxScheme.DATA_TYPE,                       "editor.datatype.fgcolor");
        applyThemeBGColor(SyntaxScheme.DATA_TYPE,                       "editor.datatype.bgcolor");
        applyThemeFont(SyntaxScheme.DATA_TYPE,                          "editor.datatype.font");
        applyThemeUnderline(SyntaxScheme.DATA_TYPE,                     "editor.datatype.underline");

        // Errors global
        applyThemeFGColor(SyntaxScheme.ERROR_CHAR,                      "editor.error.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_CHAR,                      "editor.error.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_CHAR,                         "editor.error.font");
        applyThemeUnderline(SyntaxScheme.ERROR_CHAR,                    "editor.error.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_IDENTIFIER,                "editor.error.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_IDENTIFIER,                "editor.error.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_IDENTIFIER,                   "editor.error.font");
        applyThemeUnderline(SyntaxScheme.ERROR_IDENTIFIER,              "editor.error.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_NUMBER_FORMAT,             "editor.error.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_NUMBER_FORMAT,             "editor.error.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_NUMBER_FORMAT,                "editor.error.font");
        applyThemeUnderline(SyntaxScheme.ERROR_NUMBER_FORMAT,           "editor.error.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_STRING_DOUBLE,             "editor.error.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_STRING_DOUBLE,             "editor.error.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_STRING_DOUBLE,                "editor.error.font");
        applyThemeUnderline(SyntaxScheme.ERROR_STRING_DOUBLE,           "editor.error.underline");

        // Errors fine grained
        applyThemeFGColor(SyntaxScheme.ERROR_CHAR,                      "editor.error.char.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_CHAR,                      "editor.error.char.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_CHAR,                         "editor.error.char.font");
        applyThemeUnderline(SyntaxScheme.ERROR_CHAR,                    "editor.error.char.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_IDENTIFIER,                "editor.error.identifier.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_IDENTIFIER,                "editor.error.identifier.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_IDENTIFIER,                   "editor.error.identifier.font");
        applyThemeUnderline(SyntaxScheme.ERROR_IDENTIFIER,              "editor.error.identifier.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_NUMBER_FORMAT,             "editor.error.number.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_NUMBER_FORMAT,             "editor.error.number.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_NUMBER_FORMAT,                "editor.error.number.font");
        applyThemeUnderline(SyntaxScheme.ERROR_NUMBER_FORMAT,           "editor.error.number.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_STRING_DOUBLE,             "editor.error.string.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_STRING_DOUBLE,             "editor.error.string.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_STRING_DOUBLE,                "editor.error.string.font");
        applyThemeUnderline(SyntaxScheme.ERROR_STRING_DOUBLE,           "editor.error.string.underline");

        // Function
        applyThemeFGColor(SyntaxScheme.FUNCTION,                        "editor.function.fgcolor");
        applyThemeBGColor(SyntaxScheme.FUNCTION,                        "editor.function.bgcolor");
        applyThemeFont(SyntaxScheme.FUNCTION,                           "editor.function.font");
        applyThemeUnderline(SyntaxScheme.FUNCTION,                      "editor.function.underline");

        // Identifier
        applyThemeFGColor(SyntaxScheme.IDENTIFIER,                      "editor.identifier.fgcolor");
        applyThemeBGColor(SyntaxScheme.IDENTIFIER,                      "editor.identifier.bgcolor");
        applyThemeFont(SyntaxScheme.IDENTIFIER,                         "editor.identifier.font");
        applyThemeUnderline(SyntaxScheme.IDENTIFIER,                    "editor.identifier.underline");

        // Literal globals
        applyThemeFGColor(SyntaxScheme.LITERAL_BACKQUOTE,               "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_BACKQUOTE,               "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_BACKQUOTE,                  "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_BACKQUOTE,             "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_BOOLEAN,                 "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_BOOLEAN,                 "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_BOOLEAN,                    "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_BOOLEAN,               "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_CHAR,                    "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_CHAR,                    "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_CHAR,                       "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_CHAR,                  "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_BINARY,           "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_BINARY,           "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_BINARY,              "editor.literal.font");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,      "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,      "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,         "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,    "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_FLOAT,            "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_FLOAT,            "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_FLOAT,               "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_FLOAT,          "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,      "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,      "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,         "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,    "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,     "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,     "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,        "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,   "editor.literal.underline");

        // Literal fine grained
        applyThemeFGColor(SyntaxScheme.LITERAL_BACKQUOTE,               "editor.literal.backquote.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_BACKQUOTE,               "editor.literal.backquote.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_BACKQUOTE,                  "editor.literal.backquote.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_BACKQUOTE,             "editor.literal.backquote.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_BOOLEAN,                 "editor.literal.boolean.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_BOOLEAN,                 "editor.literal.boolean.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_BOOLEAN,                    "editor.literal.boolean.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_BOOLEAN,               "editor.literal.boolean.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_CHAR,                    "editor.literal.char.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_CHAR,                    "editor.literal.char.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_CHAR,                       "editor.literal.char.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_CHAR,                  "editor.literal.char.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_BINARY,           "editor.literal.binary.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_BINARY,           "editor.literal.binary.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_BINARY,              "editor.literal.binary.font");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,      "editor.literal.decimal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,      "editor.literal.decimal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,         "editor.literal.decimal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,    "editor.literal.decimal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_FLOAT,            "editor.literal.float.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_FLOAT,            "editor.literal.float.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_FLOAT,               "editor.literal.float.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_FLOAT,          "editor.literal.float.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,      "editor.literal.hex.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,      "editor.literal.hex.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,         "editor.literal.hex.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,    "editor.literal.hex.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,     "editor.literal.string.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,     "editor.literal.string.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,        "editor.literal.string.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,   "editor.literal.string.underline");

        // Operators
        applyThemeFGColor(SyntaxScheme.OPERATOR,                        "editor.operator.fgcolor");
        applyThemeBGColor(SyntaxScheme.OPERATOR,                        "editor.operator.bgcolor");
        applyThemeFont(SyntaxScheme.OPERATOR,                           "editor.operator.font");
        applyThemeUnderline(SyntaxScheme.OPERATOR,                      "editor.operator.underline");

        // Preprocessor
        applyThemeFGColor(SyntaxScheme.PREPROCESSOR,                    "editor.preprocessor.fgcolor");
        applyThemeBGColor(SyntaxScheme.PREPROCESSOR,                    "editor.preprocessor.bgcolor");
        applyThemeFont(SyntaxScheme.PREPROCESSOR,                       "editor.preprocessor.font");
        applyThemeUnderline(SyntaxScheme.PREPROCESSOR,                  "editor.preprocessor.underline");

        // Regex
        applyThemeFGColor(SyntaxScheme.REGEX,                           "editor.regex.fgcolor");
        applyThemeBGColor(SyntaxScheme.REGEX,                           "editor.regex.bgcolor");
        applyThemeFont(SyntaxScheme.REGEX,                              "editor.regex.font");
        applyThemeUnderline(SyntaxScheme.REGEX,                         "editor.regex.underline");

        // Reserved Word global
        applyThemeFGColor(SyntaxScheme.RESERVED_WORD,                   "editor.reserved.fgcolor");
        applyThemeBGColor(SyntaxScheme.RESERVED_WORD,                   "editor.reserved.bgcolor");
        applyThemeFont(SyntaxScheme.RESERVED_WORD,                      "editor.reserved.font");
        applyThemeUnderline(SyntaxScheme.RESERVED_WORD,                 "editor.reserved.underline");
        applyThemeFGColor(SyntaxScheme.RESERVED_WORD_2,                 "editor.reserved.fgcolor");
        applyThemeBGColor(SyntaxScheme.RESERVED_WORD_2,                 "editor.reserved.bgcolor");
        applyThemeFont(SyntaxScheme.RESERVED_WORD_2,                    "editor.reserved.font");
        applyThemeUnderline(SyntaxScheme.RESERVED_WORD_2,               "editor.reserved.underline");

        // Reserved Word fine grained
        applyThemeFGColor(SyntaxScheme.RESERVED_WORD,                   "editor.reserved.1.fgcolor");
        applyThemeBGColor(SyntaxScheme.RESERVED_WORD,                   "editor.reserved.1.bgcolor");
        applyThemeFont(SyntaxScheme.RESERVED_WORD,                      "editor.reserved.1.font");
        applyThemeUnderline(SyntaxScheme.RESERVED_WORD,                 "editor.reserved.1.underline");
        applyThemeFGColor(SyntaxScheme.RESERVED_WORD_2,                 "editor.reserved.2.fgcolor");
        applyThemeBGColor(SyntaxScheme.RESERVED_WORD_2,                 "editor.reserved.2.bgcolor");
        applyThemeFont(SyntaxScheme.RESERVED_WORD_2,                    "editor.reserved.2.font");
        applyThemeUnderline(SyntaxScheme.RESERVED_WORD_2,               "editor.reserved.2.underline");

        // Variable
        applyThemeFGColor(SyntaxScheme.VARIABLE,                        "editor.variable.fgcolor");
        applyThemeBGColor(SyntaxScheme.VARIABLE,                        "editor.variable.bgcolor");
        applyThemeFont(SyntaxScheme.VARIABLE,                           "editor.variable.font");
        applyThemeUnderline(SyntaxScheme.VARIABLE,                      "editor.variable.underline");

        applyThemeFGColor(SyntaxScheme.SEPARATOR,                       "editor.brackets.fgcolor");
        applyThemeBGColor(SyntaxScheme.SEPARATOR,                       "editor.brackets.bgcolor");
        applyThemeFont(SyntaxScheme.SEPARATOR,                          "editor.brackets.font");
        applyThemeUnderline(SyntaxScheme.SEPARATOR,                     "editor.brackets.underline");


        textArea.setSyntaxScheme(scheme);
    }

    public void revertFile() {
        if(!isModified()) {
            return;
        }

        int n = editor.twoOptionBox(JOptionPane.WARNING_MESSAGE,
                                    "Revert File?",
                                    Translate.w("You have unsaved changes.  Reverting the file will lose those changes.  Are you sure?", 40, "\n"),
                                    "Yes",
                                    "No"
                                   );

        if(n != 0) {
            return;
        }

        reloadFile();
    }

    public void populateMenu(JMenu menu, int flags) {
        JMenuItem item;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();


        switch(flags) {
        case(Plugin.MENU_FILE | Plugin.MENU_MID):
            item = new JMenuItem(Translate.t("Revert File"));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    revertFile();
                }
            });
            menu.add(item);
            break;

        case(Plugin.MENU_EDIT | Plugin.MENU_TOP):
            item = new JMenuItem(Translate.t("Copy"));
            item.setAccelerator(KeyStroke.getKeyStroke('C', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.copy();
                }
            });
            menu.add(item);
            item = new JMenuItem(Translate.t("Cut"));
            item.setAccelerator(KeyStroke.getKeyStroke('X', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.cut();
                }
            });
            menu.add(item);
            item = new JMenuItem(Translate.t("Paste"));
            item.setAccelerator(KeyStroke.getKeyStroke('V', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.paste();
                }
            });
            menu.add(item);
            break;

        case(Plugin.MENU_EDIT | Plugin.MENU_MID):
            item = new JMenuItem(Translate.t("Select All"));
            item.setAccelerator(KeyStroke.getKeyStroke('A', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.selectAll();
                }
            });
            menu.add(item);
            break;

        case(Plugin.MENU_EDIT | Plugin.MENU_BOTTOM):
            item = new JMenuItem(Translate.t("Find & Replace"));
            item.setAccelerator(KeyStroke.getKeyStroke('F', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openFindPanel();
                }
            });
            menu.add(item);
            item = new JMenuItem(Translate.t("Undo"));
            item.setAccelerator(KeyStroke.getKeyStroke('Z', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.undoLastAction();
                }
            });
            menu.add(item);
            item = new JMenuItem(Translate.t("Redo"));
            item.setAccelerator(KeyStroke.getKeyStroke('Y', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.redoLastAction();
                }
            });
            menu.add(item);
            break;
        }
    }

    // Save the file contents to disk
    public boolean save() {
        if(file == null) {
            return false;
        }

        Debug.message("I think I am saving to " + file.getAbsolutePath());

        try {
            PrintWriter pw = new PrintWriter(file, "UTF-8");
            pw.write(textArea.getText());
            pw.close();
            setModified(false);
            return true;
        } catch(Exception e) {
            sketch.error(e);
        }

        return false;
    }

    public boolean saveTo(File f) {
        file = f;
        return save();
    }

    public void reloadFile() {
        int cp = textArea.getCaretPosition();
        textArea.setText(sketch.getFileContent(file, true));
        try {
            textArea.setCaretPosition(cp);
        } catch (Exception e) {
            textArea.setCaretPosition(0);
            scrollTo(0);
        }
        setModified(false);
    }

    public void insertAtCursor(String text) {
        textArea.insert(text, textArea.getCaretPosition());
    }

    public void insertAtStart(String text) {
        textArea.insert(text, 0);
    }

    public void insertAtEnd(String text) {
        textArea.append(text);
    }

    public void increaseIndent(ActionEvent e) {
        boolean wasSelected = false;
        ActionMap map = textArea.getActionMap();
        Action act;
        act = map.get(DefaultEditorKit.insertTabAction);
        String selection = textArea.getSelectedText();

        if(selection == null || selection.equals("")) {
            int off = textArea.getCaretPosition();
            textArea.setCaretPosition(textArea.getLineStartOffsetOfCurrentLine());
            act.actionPerformed(e);
            textArea.setCaretPosition(off + 1);
        } else {
            act.actionPerformed(e);
        }
    }

    public void decreaseIndent(ActionEvent e) {
        ActionMap map = textArea.getActionMap();
        Action di = map.get(RSyntaxTextAreaEditorKit.rstaDecreaseIndentAction);
        di.actionPerformed(e);
    }


    public void setSelection(int start, int end) {
        textArea.setSelectionStart(start);
        textArea.setSelectionEnd(end);
    }

    public int getSelectionStart() {
        return textArea.getSelectionStart();
    }

    public int getSelectionEnd() {
        return textArea.getSelectionEnd();
    }

    public void setSelectedText(String text) {
        textArea.replaceSelection(text);
    }

    public String getSelectedText() {
        return textArea.getSelectedText();
    }

    public void highlightLine(int line, Color color) {
        try {
            textArea.addLineHighlight(line, color);
        } catch(Exception e) {
            Base.error(e);
        }
    }

    public void clearHighlights() {
        textArea.removeAllLineHighlights();
    }

    public void removeAllFlags() {
        flagList.clear();
        updateFlags();
    }

    public void removeFlag(int line) {
        Flag foundFlag = null;
        for (Flag f : flagList) {
            if (f.getLine() == line) {
                foundFlag = f;
                break;
            }
        }
        if (foundFlag != null) {
            flagList.remove(foundFlag);
        }
        updateFlags();
    }

    public void removeFlagGroup(int group) {

        boolean found = false;
        do {
            found = false;
            Iterator it = flagList.iterator();

            while (it.hasNext()) {
                Flag f = (Flag)it.next();
                if (f.getGroup() == group) {
                    it.remove();
                    found = true;
                }
            }
        } while (found == true);
        updateFlags();
    }

    public void flagLine(int line, Icon icon, int group) {
            flagList.add(new Flag(line, icon, group));
            updateFlags();
    }

    public void updateFlags() {
        gutter.removeAllTrackingIcons();
        for (Flag f : flagList) {
            Icon i = f.getIcon();
            try {
                gutter.addLineTrackingIcon(f.getLine(), i);
            } catch (BadLocationException ex) {
            }
        }
    }


    public void gotoLine(final int line) {
        try {
            textArea.setCaretPosition(textArea.getLineStartOffset(line));
        } catch(BadLocationException e) {
        }
    }

    public void setCursorPosition(int pos) {
        textArea.setCaretPosition(pos);
    }

    public int getCursorPosition() {
        return textArea.getCaretPosition();
    }

    public void clearKeywords() {
        if (fileSyntax.equals(SyntaxConstants.SYNTAX_STYLE_EXTENDABLE_CPLUSPLUS)) {
            RSyntaxDocument d = (RSyntaxDocument)textArea.getDocument();
            ExtendableCPlusPlusTokenMaker tm = (ExtendableCPlusPlusTokenMaker)d.getTokenMaker();
            tm.clear();
        }
    }

    public void addKeyword(String name, Integer type) {
        if (fileSyntax.equals(SyntaxConstants.SYNTAX_STYLE_EXTENDABLE_CPLUSPLUS)) {
            RSyntaxDocument d = (RSyntaxDocument)textArea.getDocument();
            ExtendableCPlusPlusTokenMaker tm = (ExtendableCPlusPlusTokenMaker)d.getTokenMaker();
            if (type == KeywordTypes.LITERAL1) {
                tm.addKeyword(name, TokenTypes.VARIABLE);
            } else if (type == KeywordTypes.LITERAL2) {
                tm.addKeyword(name, TokenTypes.PREPROCESSOR);
            } else if (type == KeywordTypes.LITERAL3) {
                tm.addKeyword(name, TokenTypes.PREPROCESSOR);
            } else if (type == KeywordTypes.KEYWORD1) {
                tm.addKeyword(name, TokenTypes.RESERVED_WORD);
            } else if (type == KeywordTypes.KEYWORD2) {
                tm.addKeyword(name, TokenTypes.IDENTIFIER);
            } else if (type == KeywordTypes.KEYWORD3) {
                tm.addKeyword(name, TokenTypes.FUNCTION);
            } else if (type == KeywordTypes.OBJECT) {
                tm.addKeyword(name, TokenTypes.RESERVED_WORD_2);
            } else if (type == KeywordTypes.VARIABLE) {
                tm.addKeyword(name, TokenTypes.VARIABLE);
            } else if (type == KeywordTypes.FUNCTION) {
                tm.addKeyword(name, TokenTypes.FUNCTION);
            } else if (type == KeywordTypes.DATATYPE) {
                tm.addKeyword(name, TokenTypes.DATA_TYPE);
            } else if (type == KeywordTypes.RESERVED) {
                tm.addKeyword(name, TokenTypes.RESERVED_WORD);
            }
        }
    }

    public void repaint() {
        if (textArea != null) {
            textArea.repaint();
        }
    }

    public void updatePreview() {
        try {
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
                .process(textArea.getText());

            preview.setText("<html><body>" + html + "</body></html>");
        } catch (Exception e) {
        }
    }
}
