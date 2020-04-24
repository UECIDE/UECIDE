package org.uecide.gui.swing;

import org.uecide.Preferences;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.io.File;

public class PreferencesDir extends PreferencesSetting implements ActionListener {
    JLabel label;
    JTextField text;
    JButton select;

    public PreferencesDir(String key) {
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
    }

    public void saveSetting() {
        Preferences.set(key, text.getText());
    }
    
    public File getTheFile() {
        return new File(text.getText());
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(getTheFile());
        fc.setDialogTitle("Select Directory");

        int res = fc.showDialog(null, "Select");

        if(res == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            text.setText(f.getAbsolutePath());
        }
    }

}
