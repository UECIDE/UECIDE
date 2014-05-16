/*
 * Copyright (c) 2014, Majenko Technologies
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

package uecide.app.editors;

import uecide.app.*;
import uecide.plugin.*;
import uecide.app.debug.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.JToolBar;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

public class text extends JPanel implements EditorBase {
    File file = null;
    boolean modified = false;
    RSyntaxTextArea textArea;
    RTextScrollPane scrollPane;
    SyntaxScheme scheme;
    Sketch sketch;
    Editor editor;

    public text(Sketch s, File f, Editor e) {
        editor = e;
        sketch = s;
        this.setLayout(new BorderLayout());
        textArea = new RSyntaxTextArea();

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
        refreshSettings();
        this.add(scrollPane, BorderLayout.CENTER);
        if (f != null) {
            loadFile(f);
        }
    }

    public void refreshSettings() {
    }

    public boolean loadFile(File f) {
        file = f;
        textArea.setText(sketch.getFileContent(file));
        setCaretPosition(0);
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
        } catch (Exception ex) {
            return "";
        }
    }
    
    public void setText(String text) {
        textArea.setText(text);
    }

    public void setCaretPosition(int position) {
        textArea.setCaretPosition(position);
    }

    public void setEditable(boolean e) {
        textArea.setEditable(e);
    }

    public void setBackground(Color c) {
        if (c != null && textArea != null) {
            textArea.setBackground(c);
        }
    }

    public void setFont(Font f) {
        if (f != null && textArea != null) {
            textArea.setFont(f);
        }
    }

    public boolean isSelectionActive() {
        return textArea.getSelectedText() != null;
    }

    public void cut() {
        textArea.cut();
    }

    public void copy() {
        textArea.copy();
    }

    public void selectAll() {
        textArea.selectAll();
    }

    public void paste() {
        textArea.paste();
    }

    public void beginAtomicEdit() {
        textArea.beginAtomicEdit();
    }

    public void endAtomicEdit() {
        textArea.endAtomicEdit();
    }

    public void insert(String what, int caret) {
        textArea.insert(what, caret);
    }

    public void insert(String what) {
        textArea.insert(what, textArea.getCaretPosition());
    }

    public int getCaretPosition() {
        return textArea.getCaretPosition();
    }

    public void setSelectedText(String what) {
        textArea.replaceSelection(what);
    }

    public String getSelectedText() {
        return textArea.getSelectedText();
    }

    public int getDocumentLength() {
        return textArea.getDocument().getLength();
    }

    public void select(int s, int e) {
        textArea.select(s, e);
    }

    public int getSelectionStart() {
        return textArea.getSelectionStart();
    }

    public int getSelectionStop() {
        return textArea.getSelectionEnd();
    }

    public String getLineText(int line) {
        try {
            int start = textArea.getLineStartOffset(line);
            int end = textArea.getLineEndOffset(line);
            return getText(start, end);
        } catch (Exception e) {
            return "";
        }
    }

    public int getLineStartOffset(int line) {
        try {
            return textArea.getLineStartOffset(line);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getLineEndOffset(int line) {
        try {
            return textArea.getLineEndOffset(line);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getLineCount() {
        return textArea.getLineCount();
    }

    public int getSelectionStartLine() {
        try {
            return textArea.getLineOfOffset(getSelectionStart());
        } catch (Exception e) {
            return 0;
        }
    }

    public int getSelectionStopLine() {
        try {
            return textArea.getLineOfOffset(getSelectionStop());
        } catch (Exception e) {
            return 0;
        }
    }

    public void selectLines(int start, int end) {
        if (start > end) {
            int x = end;
            end = start;
            start = x;
        }
        try {
            int startChar = textArea.getLineStartOffset(start);
            int endChar = textArea.getLineEndOffset(end);
            textArea.setSelectionStart(startChar);
            textArea.setSelectionEnd(endChar);
        } catch (Exception e) {
        }
    }

    public void setModified(boolean m) {
        if (m != modified) {
            modified = m;
            JTabbedPane tabs = (JTabbedPane)this.getParent();
            if (tabs != null) {
//                int myIndex = tabs.indexOfComponent(this.getParent());
//                if (modified) {
//                    tabs.setIconAt(myIndex, new ImageIcon(Base.getContentFile("lib/theme/modified.png").getAbsolutePath()));
//                } else {
//                    tabs.setIconAt(myIndex, null);
//                }
            }
        }
    }

    public void setFile(File f) {
        file = f;
        JTabbedPane tabs = (JTabbedPane)this.getParent();
        if (tabs != null) {
            int myIndex = tabs.indexOfComponent(this.getParent());
            tabs.setTitleAt(myIndex, f.getName());
        }
    }

    public void undo() {
        textArea.undoLastAction();
    }

    public void redo() {
        textArea.redoLastAction();
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

    public void setNumberOffset(int off) {
        scrollPane.getGutter().setLineNumberingStartIndex(off);
    }

    public int getCaretLineNumber() {
        return textArea.getCaretLineNumber();
    }

    public void setCaretLineNumber(int lineNumber) {
        setCaretPosition(getLineStartOffset(lineNumber-1));
    }

    public void addLineHighlight(int lineNumber, Color c) {
        try {
            textArea.addLineHighlight(lineNumber - 1, c);
        } catch (Exception ignored) {}
    }

    public void removeAllLineHighlights() {
        textArea.removeAllLineHighlights();
    }

    public void applyThemeFGColor(int index, String cs) {
        if (Base.theme.get(cs) != null) {
            Color c = Base.theme.getColor(cs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.foreground = c;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeBGColor(int index, String cs) {
        if (Base.theme.get(cs) != null) {
            Color c = Base.theme.getColor(cs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.background = c;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeFont(int index, String fs) {
        if (Base.theme.get(fs) != null) {
            Font f = Base.theme.getFont(fs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.font = f;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeUnderline(int index, String us) {
        if (Base.theme.get(us) != null) {
            boolean u = Base.theme.getBoolean(us);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.underline = u;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeSettings() {
    }

    public void createExtraTokens() {
    }

    public void populateMenu(JMenu menu, int flags) {
        JMenuItem item;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();


        switch (flags) {
            case (BasePlugin.MENU_FILE | BasePlugin.MENU_MID):
                item = new JMenuItem(Translate.t("Revert File"));
                menu.add(item);
                break;

            case (BasePlugin.MENU_EDIT | BasePlugin.MENU_TOP):
                item = new JMenuItem(Translate.t("Copy"));
                item.setAccelerator(KeyStroke.getKeyStroke('C', modifiers));
                menu.add(item);
                item = new JMenuItem(Translate.t("Cut"));
                item.setAccelerator(KeyStroke.getKeyStroke('X', modifiers));
                menu.add(item);
                item = new JMenuItem(Translate.t("Paste"));
                item.setAccelerator(KeyStroke.getKeyStroke('V', modifiers));
                menu.add(item);
                break;

            case (BasePlugin.MENU_EDIT | BasePlugin.MENU_MID):
                item = new JMenuItem(Translate.t("Select All"));
                item.setAccelerator(KeyStroke.getKeyStroke('A', modifiers));
                menu.add(item);
                break;

            case (BasePlugin.MENU_EDIT | BasePlugin.MENU_BOTTOM):
                item = new JMenuItem(Translate.t("Find & Replace"));
                item.setAccelerator(KeyStroke.getKeyStroke('F', modifiers));
                menu.add(item);
                item = new JMenuItem(Translate.t("Undo"));
                item.setAccelerator(KeyStroke.getKeyStroke('Z', modifiers));
                menu.add(item);
                item = new JMenuItem(Translate.t("Redo"));
                item.setAccelerator(KeyStroke.getKeyStroke('Y', modifiers));
                menu.add(item);
                break;
        }
    }

    // Save the file contents to disk
    public boolean save() {
        if (file == null) {
            return false;
        }

        try {
            PrintWriter pw = new PrintWriter(file);
            pw.write(textArea.getText());
            pw.close();
            setModified(false);
            return true;
        } catch (Exception e) {
            sketch.error(e);
        }
        return false;
    }
    
    public void reloadFile() {
        textArea.setText(sketch.getFileContent(file, true));
        setCaretPosition(0);
        scrollTo(0);
        setModified(false);
    }
}
