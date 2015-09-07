package org.uecide;

import java.awt.*;
import javax.swing.*;

public class JVertLabel extends JLabel {

    public JVertLabel(String s){
        setText(s);
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;

        g2d.rotate(Math.toRadians(270.0)); 
        g2d.drawString(getText(), 0, 0);
System.err.println(getText());
    }
}
