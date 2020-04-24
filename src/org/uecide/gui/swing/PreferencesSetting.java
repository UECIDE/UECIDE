package org.uecide.gui.swing;

import javax.swing.JPanel;

public abstract class PreferencesSetting extends JPanel {
    protected String key;
    public PreferencesSetting(String k) {
        super();
        key = k;
    }

    public String getKey() {
        return key;
    }

    public abstract void saveSetting();
}
