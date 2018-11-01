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
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.JToolBar;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.modes.*;
import org.fife.ui.rtextarea.*;

public class markdown extends JPanel implements EditorBase {
    File file = null;
    boolean modified = false;
    RSyntaxTextArea textArea;
    RSyntaxDocument rDocument;
    RTextScrollPane scrollPane;
    JScrollPane outputScroll;
    MarkdownPane output;
    JSplitPane split;

    Sketch sketch;
    Editor editor;

    Gutter gutter;

    public markdown(Sketch s, File f, Editor e) {
        editor = e;
        sketch = s;
        this.setLayout(new BorderLayout());

        if (f == null) return;

        rDocument = new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_NONE);

        textArea = new RSyntaxTextArea(rDocument);
        output = new MarkdownPane();
        textArea.setLineWrap(true);

        rDocument.addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                setModified(true);
                output.setText(getText());
            }
            public void removeUpdate(DocumentEvent e) {
                setModified(true);
                output.setText(getText());
            }
            public void insertUpdate(DocumentEvent e) {
                setModified(true);
                output.setText(getText());
            }
        });


        scrollPane = new RTextScrollPane(textArea);

        outputScroll = new JScrollPane();
        outputScroll.setViewportView(output);

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, outputScroll);

        this.add(split, BorderLayout.CENTER);

        loadFile(f);

        refreshSettings();
        split.setResizeWeight(0.5d);
    }

    public void requestFocus() {
        textArea.requestFocus();
    }

    public void refreshSettings() {
        boolean external = Preferences.getBoolean("editor.external");
        textArea.setEditable(!external);

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
    }

    public void removeFlag(int line) {
    }

    public void removeFlagGroup(int group) {
    }

    public void flagLine(int line, Icon icon, int group) {
    }

    public void updateFlags() {
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
    }

    public void addKeyword(String name, int type) {
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

    public boolean getUpdateFlag() {
        return false;
    }

    public static String emptyFile() { return ""; }

}
