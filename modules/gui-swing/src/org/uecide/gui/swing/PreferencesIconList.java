package org.uecide.gui.swing;

import org.uecide.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import java.awt.BorderLayout;

import java.util.Arrays;
import java.util.ArrayList;

public class PreferencesIconList extends PreferencesSetting {
    JLabel label;
    JComboBox<String> options;
    String[] values;

    public PreferencesIconList(String key) {
        super(key);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        label = new JLabel(Preferences.preferencesTree.get(key + ".name") + ":");
        add(label, BorderLayout.WEST);

        values = IconManager.getIconList().keySet().toArray(new String[0]);
        int selIdx = -1;
        int i = 0;
        String val = Preferences.get(key);
        for (String value : values) {
            if (value.equals(val)) {
                selIdx = i;
            }
            i++;
        }

        options = new JComboBox<String>(values);
        options.setSelectedIndex(selIdx);
        options.setRenderer(new PreferencesIconListRenderer());
        add(options, BorderLayout.CENTER);

    }

    public void saveSetting() {
        String p = (String)options.getSelectedItem();
        Preferences.set(key, p);
    }
}
