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
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


public class CodeEditor extends TabPanel implements ContextEventListener, FocusListener, CopyAndPaste {
    Context ctx;
    SketchFile file;

    RTextScrollPane scrollPane;
    RSyntaxTextArea textArea;
    AbstractDocument document;

    JPanel tabPanel = null;
    JLabel tabLabel = null;

    int savedCaretPosition = 0;

    public CodeEditor(Context c, AutoTab def, SketchFile f) {
        super(f.getFile().getName(), def);
        ctx = c;
        file = f;

        document = f.getDocument();
        if (!(document instanceof RSyntaxDocument)) {
            f.promoteDocument(new RSyntaxDocument(FileType.getSyntaxStyle(file.getFile().getName())));
            document = f.getDocument();
        }
        textArea = new RSyntaxTextArea((RSyntaxDocument)document);
        scrollPane = new RTextScrollPane(textArea);
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
            try {
                JLabel ico = new JLabel("");
                ico.setIcon(IconManager.getIcon(16, "mime." + FileType.getIcon(file.getFile().getName())));
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
}
