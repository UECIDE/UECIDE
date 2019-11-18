package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;
import org.uecide.SketchFile;
import org.uecide.Preferences;

import java.awt.BorderLayout;
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


public class CodeEditor extends TabPanel  {
    Context ctx;
    SketchFile file;

    RTextScrollPane scrollPane;
    RSyntaxTextArea textArea;
    RSyntaxDocument document;

    JPanel tabPanel = null;

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

    @Override
    public Component getTab() {
        if (tabPanel == null) {
            tabPanel = new JPanel();
            tabPanel.setLayout(new BorderLayout());
            JLabel l = new JLabel(file.getFile().getName());
            tabPanel.add(l, BorderLayout.CENTER);
            try {
                JLabel ico = new JLabel("");
                ico.setIcon(IconManager.getIcon(16, "mime." + FileType.getIcon(file.getFile().getName())));
                tabPanel.add(ico, BorderLayout.WEST);
            } catch (Exception ex) { }
            
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
            } catch (Exception ex) { }
        }
        return tabPanel;
    }
}
