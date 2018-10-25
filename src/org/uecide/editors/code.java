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

public class code extends JPanel implements EditorBase {
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
    JButton findButton = new JButton(Base.i18n.string("misc.find"));
    SearchContext search = new SearchContext();
    JCheckBox matchCase = new JCheckBox(Base.i18n.string("misc.case"));
    JCheckBox searchBackwards = new JCheckBox(Base.i18n.string("misc.back"));
    JTextField replaceWith = new JTextField();
    JButton replaceButton = new JButton(Base.i18n.string("misc.replace"));
    JButton replaceAllButton = new JButton(Base.i18n.string("misc.all"));
    JButton findCloseButton;
    Gutter gutter;

    String fileSyntax;

    JToolBar toolbar;

    static class Flag {
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

    public code(Sketch s, File f, Editor e) {
        editor = e;
        sketch = s;
        this.setLayout(new BorderLayout());

        if (f == null) return;

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

        JPopupMenu pm = textArea.getPopupMenu();

        editor.addMenuChunk(pm, Plugin.MENU_POPUP_EDITOR | Plugin.MENU_TOP);
        editor.addMenuChunk(pm, Plugin.MENU_POPUP_EDITOR | Plugin.MENU_MID);
        editor.addMenuChunk(pm, Plugin.MENU_POPUP_EDITOR | Plugin.MENU_BOTTOM);


        scrollPane = new RTextScrollPane(textArea);

        scrollPane.removeMouseWheelListener(scrollPane.getMouseWheelListeners()[0]);

        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    Adjustable adj = scrollPane.getVerticalScrollBar();
                    int scale = Preferences.getInteger("theme.editor.fonts.scale");
                    int rscale = e.getWheelRotation() * 5;
                    scale -= rscale;
                    if (scale < 1) scale = 1;
                    if (scale > 1000) scale = 1000;
                    Preferences.setInteger("theme.editor.fonts.scale", scale);
                    refreshSettings();
                } else if (e.isShiftDown()) {
                    // Horizontal scrolling
                    Adjustable adj = scrollPane.getHorizontalScrollBar();
                    int scroll = e.getUnitsToScroll() * adj.getBlockIncrement();
                    adj.setValue(adj.getValue() + scroll);
                } else {
                    // Vertical scrolling
                    Adjustable adj = scrollPane.getVerticalScrollBar();
                    int scroll = e.getUnitsToScroll() * adj.getBlockIncrement();
                    adj.setValue(adj.getValue() + scroll);
                }
            }
        });
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

        this.add(scrollPane, BorderLayout.CENTER);

        loadFile(f);

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
        refreshSettings();

    }

    public void requestFocus() {
        textArea.requestFocus();
//        textArea.forceCurrentLineHighlightRepaint();
    }

    public void refreshSettings() {
        boolean external = Preferences.getBoolean("editor.external");
        textArea.setEditable(!external);
        textArea.setCodeFoldingEnabled(true);

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

        textArea.setTabsEmulated(Preferences.getBoolean("editor.tabs.expand"));
        textArea.setPaintTabLines(Preferences.getBoolean("editor.tabs.show"));

        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.setIconRowHeaderEnabled(true);
        setBackground(Preferences.getColor("theme.editor.colors.background"));
        textArea.setBackground(Preferences.getColor("theme.editor.colors.background"));

        textArea.setForeground(Preferences.getColor("theme.editor.colors.foreground"));
        textArea.setFont(Preferences.getScaledFont("theme.editor.fonts.default.font"));

        gutter = scrollPane.getGutter();


        gutter.setLineNumberFont(Preferences.getScaledFont("theme.editor.gutter.font"));
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
        Font f = Preferences.getScaledFont(fs);
        if (fspec == null) {
            f = Preferences.getScaledFont("theme.editor.fonts.default.font");
        } else if (fspec.equals("default")) {
            f = Preferences.getScaledFont("theme.editor.fonts.default.font");
        } else if (fspec.equals("")) {
            f = Preferences.getScaledFont("theme.editor.fonts.default.font");
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


    public void applyThemeFont(int section, String type) {
        applyThemeFGColor(section, "theme.editor.fonts." + type + ".foreground");
        applyThemeBGColor(section, "theme.editor.fonts." + type + ".background");
        applyThemeFontFace(section, "theme.editor.fonts." + type + ".font");
        applyThemeUnderline(section, "theme.editor.fonts." + type + ".underline");
        applyThemeBold(section, "theme.editor.fonts." + type + ".bold");
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

    public void revertFile() {
        if(!isModified()) {
            return;
        }

        int n = editor.twoOptionBox(JOptionPane.WARNING_MESSAGE,
                                    Base.i18n.string("msg.revert.title"),
                                    Base.i18n.string("msg.revert.body"),
                                    Base.i18n.string("misc.yes"),
                                    Base.i18n.string("misc.no")
                                   );

        if(n != 0) {
            return;
        }

        reloadFile();
    }
    
    @SuppressWarnings("deprecation")
    public void populateMenu(JMenu menu, int flags) {
        JMenuItem item;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();


        switch(flags) {
        case(Plugin.MENU_FILE | Plugin.MENU_MID):
            item = new JMenuItem(Base.i18n.string("menu.file.revert"));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    revertFile();
                }
            });
            menu.add(item);
            break;

        case(Plugin.MENU_EDIT | Plugin.MENU_TOP):
            item = new JMenuItem(Base.i18n.string("menu.copy"));
            item.setAccelerator(KeyStroke.getKeyStroke('C', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.copy();
                }
            });
            menu.add(item);
            item = new JMenuItem(Base.i18n.string("menu.cut"));
            item.setAccelerator(KeyStroke.getKeyStroke('X', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.cut();
                }
            });
            menu.add(item);
            item = new JMenuItem(Base.i18n.string("menu.paste"));
            item.setAccelerator(KeyStroke.getKeyStroke('V', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.paste();
                }
            });
            menu.add(item);
            break;

        case(Plugin.MENU_EDIT | Plugin.MENU_MID):
            item = new JMenuItem(Base.i18n.string("menu.selectall"));
            item.setAccelerator(KeyStroke.getKeyStroke('A', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.selectAll();
                }
            });
            menu.add(item);
            break;

        case(Plugin.MENU_EDIT | Plugin.MENU_BOTTOM):
            item = new JMenuItem(Base.i18n.string("menu.findreplace"));
            item.setAccelerator(KeyStroke.getKeyStroke('F', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openFindPanel();
                }
            });
            menu.add(item);
            item = new JMenuItem(Base.i18n.string("menu.undo"));
            item.setAccelerator(KeyStroke.getKeyStroke('Z', modifiers));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.undoLastAction();
                }
            });
            menu.add(item);
            item = new JMenuItem(Base.i18n.string("menu.redo"));
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
        int len = textArea.getText().length();
        if (cp > len) cp = len;
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
            textArea.addLineHighlight(line-1, color);
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
                gutter.addLineTrackingIcon(f.getLine() - 1, i);
            } catch (BadLocationException ex) {
            }
        }
    }


    public void gotoLine(final int line) {
        try {
            textArea.setCaretPosition(textArea.getLineStartOffset(line - 1));
        } catch(BadLocationException e) {
        }
    }

    public void setCursorPosition(int pos) {
        int len = textArea.getText().length();
        if (pos > len) pos = len;
        try {
            textArea.setCaretPosition(pos);
        } catch (Exception e) {
        }
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

    public void addKeyword(String name, int type) {
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

    public void populateMenu(JPopupMenu menu, int flags) {
    }

    public Component getContentPane() {
        return (Component)textArea;
    }

    public Rectangle getViewRect() {
        return scrollPane.getViewport().getViewRect();
    }

    public void setViewPosition(Point p) {
        scrollPane.getViewport().setViewPosition(p);
    }

}
