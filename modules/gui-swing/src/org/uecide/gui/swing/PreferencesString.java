package org.uecide.gui.swing;

import org.uecide.Preferences;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.BorderFactory;

import java.awt.BorderLayout;

public class PreferencesString extends PreferencesSetting {
    JLabel label;
    JTextField text;

    public PreferencesString(String key) {
        super(key);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        label = new JLabel(Preferences.preferencesTree.get(key + ".name") + ":");
        add(label, BorderLayout.WEST);

        text = new JTextField(Preferences.get(key));
        add(text, BorderLayout.CENTER);
    }

    public void saveSetting() {
        Preferences.set(key, text.getText());
    }
}
