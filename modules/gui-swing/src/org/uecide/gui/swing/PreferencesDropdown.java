package org.uecide.gui.swing;

import org.uecide.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import java.awt.BorderLayout;

import java.util.Arrays;
import java.util.ArrayList;

public class PreferencesDropdown extends PreferencesSetting {
    JLabel label;
    JComboBox<KVPair> options;
    KVPair[] values;

    class KVPair {
        public String key;
        public String value;
        public KVPair(String k, String v) {
            key = k;
            value = v;
        }
        public String toString() { return value; }
    }

    public PreferencesDropdown(String key) {
        super(key);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        label = new JLabel(Preferences.preferencesTree.get(key + ".name") + ":");
        add(label, BorderLayout.WEST);

        ArrayList<KVPair> valueArray = new ArrayList<KVPair>();

        String[] optionKeys = Preferences.preferencesTree.childKeysOf(key + ".options");
        Arrays.sort(optionKeys);
        int selIdx = -1;
        String selValue = Preferences.get(key);
        int i = 0;
        for (String option : optionKeys) {
            String val = Preferences.preferencesTree.get(key + ".options." + option);
            KVPair pair = new KVPair(option, val);
            valueArray.add(pair);
            if (option.equals(selValue)) {
                selIdx = i;
            }
            i++;
        }

        values = valueArray.toArray(new KVPair[0]);

        options = new JComboBox<KVPair>(values);
        options.setSelectedIndex(selIdx);
        add(options, BorderLayout.CENTER);

    }

    public void saveSetting() {
        KVPair p = (KVPair)options.getSelectedItem();
        Preferences.set(key, p.key);
    }
}
