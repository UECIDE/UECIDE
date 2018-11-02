package uk.co.majenko.bmpedit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;

public class ToolButton extends JButton {
    
    Color selectedColor = Color.BLACK;
    Color backgroundColor;

    public ToolButton(String icon, ActionListener l) {
        super();

        try {
            ImageIcon iconImage = new ImageIcon(ImageIO.read(getClass().getResourceAsStream(icon)));
            setIcon(iconImage);
        } catch (Exception ignored) {
        }

        addActionListener(l);

        setSize(new Dimension(32, 32));
        setMinimumSize(new Dimension(32, 32));
        setMaximumSize(new Dimension(32, 32));
        backgroundColor = getBackground();
    }

    public void select() {
    }

    public void setSelectedColor(Color c) {
        selectedColor = c;
    }

    public void setBackground(Color c) {
        backgroundColor = c;
        super.setBackground(c);
    }

    public void setSelected(boolean sel) {
        if (sel) {
            super.setBackground(selectedColor);
        } else {
            super.setBackground(backgroundColor);
        }
    }
}
