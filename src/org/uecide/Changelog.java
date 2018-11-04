package org.uecide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Changelog extends JDialog {

    Editor editor;
    JCheckBox hide;

    public Changelog(Editor e) {
        editor = e;
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle(Base.i18n.string("win.changelog"));
        setLayout(new BorderLayout());

        MarkdownPane mainPanel = new MarkdownPane();

        JScrollPane mainScroll = new JScrollPane(mainPanel);
        add(mainScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        add(buttons, BorderLayout.SOUTH);

        hide = new JCheckBox(Base.i18n.string("changelog.hide"));
        buttons.add(hide);

        JButton close = new JButton(Base.i18n.string("misc.close"));
        buttons.add(close);

        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (hide.isSelected()) {
                    Base.preferences.set("changelog.hide", Base.systemVersion.toString());
                }
                Changelog.this.setVisible(false);
            }
        });

        String content = Base.getResourceAsString("/org/uecide/changelog.md");
        mainPanel.setText(content);

        setIconImage(Base.loadImageFromResource("icons/icon.png"));

        setSize(600, 500);
        setLocationRelativeTo(editor);

        setVisible(true);
    }
}
