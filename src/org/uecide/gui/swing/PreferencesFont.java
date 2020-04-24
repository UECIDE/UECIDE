package org.uecide.gui.swing;

import org.uecide.Preferences;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.BorderFactory;

import say.swing.JFontChooser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Font;

public class PreferencesFont extends PreferencesSetting implements KeyListener, ActionListener {
    JLabel label;
    JTextField text;
    JButton select;
    JButton def;

    public PreferencesFont(String key) {
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

        c.weightx = 0.5d;
        text = new JTextField(Preferences.get(key));
        add(text, c);
        c.gridx++;

        select = new JButton("Select...");
        c.weightx = 0.2d;
        select.addActionListener(this);
        add(select, c);
        c.gridx++;

        def = new JButton("Default");
        c.weightx = 0.2d;
        def.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                text.setText("default");
                updateFont();
            }
        });
        add(def, c);
        c.gridx++;




        updateFont();
        text.addKeyListener(this);
    }

    public void saveSetting() {
        if (text.getText().equals("default")) {
            Preferences.set(key, "default");
        } else {
            Preferences.setFont(key, getTheFont());
        }
    }
    
    public Font getTheFont() {
        return Preferences.stringToFont(text.getText());
    }

    public void updateFont() {
        text.setFont(getTheFont());
        revalidate();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent evt) {
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        updateFont();
    }

    @Override
    public void keyTyped(KeyEvent evt) {
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        JFontChooser fc = new JFontChooser();
        fc.setSelectedFont(Preferences.stringToFont(text.getText()));
        int res = fc.showDialog(null);

        if(res == JFontChooser.OK_OPTION) {
            Font fnt = fc.getSelectedFont();
            text.setText(Preferences.fontToString(fnt));
            updateFont();
        }
    }

}
