package org.uecide.gui.swing;

import org.uecide.Preferences;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.BorderFactory;
import javax.swing.SpinnerNumberModel;

import java.awt.BorderLayout;

public class PreferencesRange extends PreferencesSetting {
    JLabel label;
    JSpinner spinner;

    public PreferencesRange(String key) {
        super(key);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        label = new JLabel(Preferences.preferencesTree.get(key + ".name") + ":");
        add(label, BorderLayout.WEST);

        SpinnerNumberModel model = new SpinnerNumberModel((int)Preferences.getInteger(key), (int)Preferences.preferencesTree.getInteger(key + ".min"), (int)Preferences.preferencesTree.getInteger(key + ".max"), 1);
        spinner = new JSpinner(model);
        add(spinner, BorderLayout.CENTER);
    
    }

    public void saveSetting() {
        Preferences.setInteger(key, (Integer)spinner.getValue());
    }
}
