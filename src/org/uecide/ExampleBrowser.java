package org.uecide;

import javax.swing.*;
import java.awt.*;

public class ExampleBrowser extends JDialog {
    Editor editor;

    public ExampleBrowser(Editor e) {
        editor = e;
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle(Base.i18n.string("win.example"));
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        JScrollPane mainScroll = new JScrollPane(mainPanel);
        add(mainScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        add(buttons, BorderLayout.SOUTH);

        setSize(600, 500);
        setLocationRelativeTo(editor);
        setVisible(true);
    }
    
}
