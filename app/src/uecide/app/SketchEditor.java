package uecide.app;

import uecide.plugin.*;
import uecide.app.debug.*;
import processing.core.*;

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

import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import gnu.io.*;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

class SketchEditor extends JPanel {
    File file = null;
    boolean modified = false;
    RSyntaxTextArea textArea;
    RTextScrollPane scrollPane;

    public SketchEditor(File file) {
        super();
        setLayout(new BorderLayout());
        textArea = new RSyntaxTextArea();
        if (file.getName().endsWith(".ino") || file.getName().endsWith(".pde")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ARDUINO);
        }
        if (file.getName().endsWith(".cpp") || file.getName().endsWith(".h")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        }
        if (file.getName().endsWith(".c")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        }
        if (file.getName().endsWith(".S")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_AVR);
        }
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setMarkOccurrences(true);
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
        scrollPane.setFoldIndicatorEnabled(true);
        Gutter g = scrollPane.getGutter();
        g.setBackground(Theme.getColor("status.gutter.bgcolor"));
        g.setForeground(Theme.getColor("status.gutter.fgcolor"));
        setBackground(Theme.getColor("editor.bgcolor"));
        setFont(Preferences.getFont("editor.font"));

        this.add(scrollPane, BorderLayout.CENTER);
        if (file != null) {
            loadFile(file);
        }
    }

    public boolean loadFile(File file) {
        if (file == null) {
            return false;
        }
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            return false;
        }
        this.file = file;
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            textArea.read(r, null);
            r.close();
        } catch (IOException ioe) {
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            return false;
        }
        setCaretPosition(0);
        scrollTo(0);
        setModified(false);
        return true;
    }

    public boolean writeFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.isDirectory()) {
            return false;
        }
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            textArea.write(w);
            w.close();
        } catch (IOException ioe) {
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            return false;
        }
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

    public void setModified(boolean m) {
        if (m != modified) {
            modified = m;
            JTabbedPane tabs = (JTabbedPane)this.getParent();
            if (tabs != null) {
                int myIndex = tabs.indexOfComponent(this);
                if (modified) {
                    tabs.setIconAt(myIndex, new ImageIcon(Base.getContentFile("lib/theme/modified.png").getAbsolutePath()));
                } else {
                    tabs.setIconAt(myIndex, null);
                }
            }
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
}
