package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;
import org.uecide.SketchFile;
import org.uecide.Preferences;

import java.awt.BorderLayout;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


public class CodeEditor extends TabPanel  {
    Context ctx;
    SketchFile file;

    RTextScrollPane scrollPane;
    RSyntaxTextArea textArea;
    RSyntaxDocument document;

    public CodeEditor(Context c, SketchFile f) {
        super(f.getFile().getName());
        ctx = c;
        file = f;

        document = new RSyntaxDocument(FileType.getSyntaxStyle(file.getFile().getName()));
        textArea = new RSyntaxTextArea(document);
        scrollPane = new RTextScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
        textArea.setText(f.getFileData());
        textArea.requestFocus();
    }

    public SketchFile getSketchFile() {
        return file;
    }

    public void flushData() {
        file.setFileData(textArea.getText());
    }

    public void requestFocus() {
        textArea.requestFocus();
    }
}
