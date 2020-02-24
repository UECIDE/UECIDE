package org.uecide.gui.swing;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Dialog;

public class FancyDialog extends JDialog implements AnimationListener {

    JLabel iconLabel;
    JButton yes = null;
    JButton no = null;
    JButton ok = null;
    JButton cancel = null;
    JTextField input = null;
    JPanel buttons = null;

    int result = -1;
    String defaultValue;
    String questionText;

    static final int QUESTION_YESNO = 1;
    static final int QUESTION_YESNOCANCEL = 2;
    static final int INPUT_OKCANCEL = 3;

    static final int ANSWER_YES = 1;
    static final int ANSWER_NO = 2;
    static final int ANSWER_OK = 3;
    static final int ANSWER_CANCEL = 4;

    public FancyDialog(JFrame owner, String title, String question, CleverIcon icon, int type) {
        this(owner, title, question, icon, type, "");
    }

    public FancyDialog(JFrame owner, String title, String question, CleverIcon icon, int type, String def) {
        super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
//        setSize(300, 200);
        defaultValue = def;
        questionText = question;
        
        setLayout(new BorderLayout());
        buttons = new JPanel();

        iconLabel = new JLabel("");
        iconLabel.setIcon(icon);
        icon.addAnimationListener(this);
        iconLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(iconLabel, BorderLayout.WEST);


        switch (type) {
            case QUESTION_YESNO: qYesNo(); break;
            case QUESTION_YESNOCANCEL: qYesNoCancel(); break;
            case INPUT_OKCANCEL: iOkCancel(); break;
        }

        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);

        Dimension d = getSize();
        d.width *= 1.1;
        d.height *= 1.1;
        setSize(d);
        setVisible(true);
    }

    void qYesNo() {
        JLabel message = new JLabel("<html>" + questionText.replaceAll("\n", "<br/>") + "</html>");
        message.setBorder(new EmptyBorder(5, 15, 5, 5));
        add(message, BorderLayout.CENTER);

        yes = new JButton("Yes");
        yes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent E) {
                result = ANSWER_YES;
                dispose();
            }
        });
        buttons.add(yes);
        no = new JButton("No");
        no.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent E) {
                result = ANSWER_NO;
                dispose();
            }
        });
        buttons.add(no);
    }

    void qYesNoCancel() {
        JLabel message = new JLabel("<html>" + questionText.replaceAll("\n", "<br/>") + "</html>");
        message.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(message, BorderLayout.CENTER);

        yes = new JButton("Yes");
        yes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent E) {
                result = ANSWER_YES;
                dispose();
            }
        });
        buttons.add(yes);
        no = new JButton("No");
        no.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent E) {
                result = ANSWER_NO;
                dispose();
            }
        });
        buttons.add(no);
        cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent E) {
                result = ANSWER_CANCEL;
                dispose();
            }
        });
        buttons.add(cancel);
    }

    void iOkCancel() {

        JPanel inner = new JPanel();
        inner.setLayout(new BorderLayout());

        JLabel message = new JLabel("<html>" + questionText.replaceAll("\n", "<br/>") + "</html>");
        message.setBorder(new EmptyBorder(5, 5, 5, 5));
        inner.add(message, BorderLayout.CENTER);

        input = new JTextField();
        input.setText(defaultValue);
        inner.add(input, BorderLayout.SOUTH);

        add(inner, BorderLayout.CENTER);


        ok = new JButton("Ok");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent E) {
                result = ANSWER_OK;
                dispose();
            }
        });
        buttons.add(ok);
        cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent E) {
                result = ANSWER_CANCEL;
                dispose();
            }
        });
        buttons.add(cancel);
    }

    public void animationUpdated(CleverIcon i) {
        iconLabel.repaint();
    }

    public int getResult() {
        return result;
    }

    public String getText() {
        if (input == null) return null;
        return input.getText();
    }
}
