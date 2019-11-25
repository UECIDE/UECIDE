package org.uecide.gui.swing;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JTextField;
import java.awt.Dialog;

public class FancyDialog extends JDialog implements AnimationListener {

    JLabel iconLabel;

    public FancyDialog(JFrame owner, String title, String question, CleverIcon icon) {
        super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        setSize(300, 200);
        setLayout(new BorderLayout());

        iconLabel = new JLabel("");
        iconLabel.setIcon(icon);
        icon.addAnimationListener(this);
        add(iconLabel, BorderLayout.WEST);

        setVisible(true);
    }

    public void animationUpdated(CleverIcon i) {
        iconLabel.repaint();
    }
}
