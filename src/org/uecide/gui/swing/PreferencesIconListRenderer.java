package org.uecide.gui.swing;

import org.uecide.Debug;
import javax.swing.JList;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import java.io.IOException;
import java.util.HashMap;


public class PreferencesIconListRenderer extends DefaultListCellRenderer {

    HashMap<String, CleverIcon> icons = new HashMap<String, CleverIcon>();

    public PreferencesIconListRenderer() {
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        String iconSetName = (String)value;

        this.setText(IconManager.getDescription(iconSetName));

        CleverIcon icon = icons.get(iconSetName);
        if (icon == null) {
            try {
                icon = new CleverIcon(24, IconManager.getIconFileFromSet(iconSetName, "icon.main.save"));
                icons.put(iconSetName, icon);
            } catch (IOException ex) {
                Debug.exception(ex);
            }
        }
        this.setIcon(icon);

        return this;
    }
}
