package uk.co.majenko.bmpedit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;
import java.io.*;

public class EditBar extends JToolBar {
    BMPEdit editor;

    public EditBar(BMPEdit ed) {
        super();
        editor = ed;
        setFloatable(false);
        setOrientation(JToolBar.HORIZONTAL);

        try {
            JButton undo = new JButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/uk/co/majenko/bmpedit/icons/undo.png"))));
            undo.setSize(new Dimension(32, 32));
            undo.setMinimumSize(new Dimension(32, 32));
            undo.setMaximumSize(new Dimension(32, 32));
            undo.setPreferredSize(new Dimension(32, 32));
            undo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editor.undo();
                }
            });
            undo.setToolTipText("Undo last edit");
            add(undo);

            addSeparator();

            JButton scale = new JButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/uk/co/majenko/bmpedit/icons/scale.png"))));
            scale.setSize(new Dimension(32, 32));
            scale.setMinimumSize(new Dimension(32, 32));
            scale.setMaximumSize(new Dimension(32, 32));
            scale.setPreferredSize(new Dimension(32, 32));
            scale.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editor.scale();
                }
            });
            scale.setToolTipText("Scale image");
            add(scale);

            JButton crop = new JButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/uk/co/majenko/bmpedit/icons/crop.png"))));
            crop.setSize(new Dimension(32, 32));
            crop.setMinimumSize(new Dimension(32, 32));
            crop.setMaximumSize(new Dimension(32, 32));
            crop.setPreferredSize(new Dimension(32, 32));
            crop.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editor.crop();
                }
            });
            crop.setToolTipText("Crop image");
            add(crop);

            JButton conv = new JButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/uk/co/majenko/bmpedit/icons/conv.png"))));
            conv.setSize(new Dimension(32, 32));
            conv.setMinimumSize(new Dimension(32, 32));
            conv.setMaximumSize(new Dimension(32, 32));
            conv.setPreferredSize(new Dimension(32, 32));
            conv.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editor.conv();
                }
            });
            conv.setToolTipText("Apply convolution matrix");
            add(conv);

            

        } catch (Exception ignored) {
        }


    }
}
