package org.uecide.gui.swing;

import org.uecide.Preferences;

import javax.swing.JCheckBox;
import javax.swing.BorderFactory;

import java.awt.BorderLayout;
import java.awt.Color;

public class PreferencesCheckBox extends PreferencesSetting {
    JCheckBox checkbox;

    public PreferencesCheckBox(String key) {
        super(key);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
        checkbox = new JCheckBox(Preferences.preferencesTree.get(key + ".name"));
        add(checkbox, BorderLayout.CENTER);
        checkbox.setSelected(Preferences.getBoolean(key));
    }

    public void saveSetting() {
        Preferences.setBoolean(key, checkbox.isSelected());
    }
}
