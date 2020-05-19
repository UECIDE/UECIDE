package org.uecide.gui.swing;

import org.uecide.Preferences;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JColorChooser;
import javax.swing.BorderFactory;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Color;

public class PreferencesColor extends PreferencesSetting implements KeyListener, MouseListener {
    JLabel label;
    JTextField text;
    ColorPanel color;

    public PreferencesColor(String key) {
        super(key);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.1d;

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        label = new JLabel(Preferences.preferencesTree.get(key + ".name") + ":");
        add(label, c);
        c.gridx++;

        c.weightx = 0.6d;
        text = new JTextField(Preferences.get(key));
        add(text, c);
        c.gridx++;

        color = new ColorPanel();

        c.weightx = 0.3d;
        color.addMouseListener(this);
        add(color, c);
        c.gridx++;
        updateColor();
        text.addKeyListener(this);
    }

    public void saveSetting() {
        Preferences.set(key, text.getText());
    }
    
    public Color getColor() {
        try {
            return Color.decode(text.getText());
        } catch (Exception ignored) {
        }
        return Color.BLACK;
    }

    public void updateColor() {
        color.setColor(getColor());
    }

    @Override
    public void keyPressed(KeyEvent evt) {
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        updateColor();
    }

    @Override
    public void keyTyped(KeyEvent evt) {
    }

    @Override
    public void mouseClicked(MouseEvent evt) {

        JColorChooser fc = new JColorChooser();

        Color orig = getColor();

        Color clr = fc.showDialog(null, "Select Color", orig);

        if(clr != null) {
            text.setText(String.format("#%02x%02x%02x", clr.getRed(), clr.getGreen(), clr.getBlue()));
            updateColor();
        }
    }

    @Override
    public void mousePressed(MouseEvent evt) {
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
    }

    @Override
    public void mouseEntered(MouseEvent evt) {
    }

    @Override
    public void mouseExited(MouseEvent evt) {
    }

}
