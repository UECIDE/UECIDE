package org.uecide;

import javax.swing.*;
import java.awt.*;

public class JBoldLabel extends JLabel {
    public JBoldLabel(String text) {
        super(text);

        Font f = getFont();
        setFont(f.deriveFont(Font.BOLD));
    }

    public JBoldLabel(String text, float scale) {
        super(text);

        Font f = getFont();
        setFont(f.deriveFont(Font.BOLD, f.getSize() * scale));
    }
}
