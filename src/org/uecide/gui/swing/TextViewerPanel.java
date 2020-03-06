package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;
import org.uecide.SketchFile;
import org.uecide.Preferences;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;


public class TextViewerPanel extends TabPanel implements ContextEventListener {
    Context ctx;
    String text;
    String title;

    JScrollPane scrollPane;
    JTextArea textArea;

    JLabel tabLabel = null;
    JPanel tabPanel = null;

    int savedCaretPosition = 0;

    public TextViewerPanel(Context c, AutoTab def, String ttl, String txt) {
        super(ttl, def);
        ctx = c;
        title = ttl;
        text = txt;

        textArea = new JTextArea();

        scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
        textArea.setText(txt);
        textArea.setEditable(false);
    }

    @Override
    public Component getTab() {
        if (tabPanel == null) {
            tabPanel = new JPanel();
            tabPanel.setOpaque(false);
            tabPanel.setLayout(new BorderLayout());
            tabLabel = new JLabel(title);
            tabLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
            tabPanel.add(tabLabel, BorderLayout.CENTER);
            try {
                JButton ico = new JButton(IconManager.getIcon(16, "tabs.close"));
                ico.setBorderPainted(false);
                ico.setFocusPainted(false);
                ico.setContentAreaFilled(false);
                ico.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        closeViewer();
                    }
                });
                tabPanel.add(ico, BorderLayout.EAST);
            } catch (Exception ex) { }
        }

        JLabel fl = new JLabel("");
        Font basefont = fl.getFont();

        Font newfont;

        newfont = basefont.deriveFont(Font.PLAIN);
        tabLabel.setFont(newfont);

        return tabPanel;
    }

    public void contextEventTriggered(ContextEvent evt) {
    }

    public void setText(String txt) {
        text = txt;
        textArea.setText(txt);
    }

    public void closeViewer() {
        AutoTab at = ((SwingGui)ctx.getGui()).getPanelByTab(this);
        if (at != null) {
            at.remove(this);
            if (at.isSeparateWindow()) {
                if (at.getTabCount() == 0) {
                    at.getParentWindow().dispose();
                }
            }
        }
    }
}
