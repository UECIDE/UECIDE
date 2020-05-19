package org.uecide.gui.swing;

import org.uecide.Preferences;
import org.uecide.PropertyFile;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JComponent;
import java.util.HashMap;
import java.util.Enumeration;

public class PreferencesTreeEntry extends DefaultMutableTreeNode {
    String key;
    PreferencesTreeModel model;
    PropertyFile preferencesTree;
    
    HashMap<String, PreferencesSetting> settings;

    public PreferencesTreeEntry(PreferencesTreeModel mdl, String k) {
        super("updating...");
        key = k;
        model = mdl;
        if (k == null) {
            preferencesTree = Preferences.preferencesTree;
            
        } else {
            preferencesTree = Preferences.preferencesTree.getChildren(k);
        }

        String[] childKeys = preferencesTree.childKeys();
        settings = new HashMap<String, PreferencesSetting>();

        for (String child : childKeys) {
            String type = preferencesTree.get(child + ".type");
            if (type == null) continue;

            String subkey = key + "." + child;
            if (key == null) {
                subkey = child;
            }

            switch (type) {
                case "section": {
                    PreferencesTreeEntry subtree = new PreferencesTreeEntry(model, subkey);
                    add(subtree);
                } break;

                case "checkbox": {
                    PreferencesSetting setting = new PreferencesCheckBox(subkey);
                    settings.put(subkey, setting);
                } break;

                case "range": {
                    PreferencesSetting setting = new PreferencesRange(subkey);
                    settings.put(subkey, setting);
                } break;
        
                case "dropdown": {
                    PreferencesSetting setting = new PreferencesDropdown(subkey);
                    settings.put(subkey, setting);
                } break;
        
                case "string": {
                    PreferencesSetting setting = new PreferencesString(subkey);
                    settings.put(subkey, setting);
                } break;
        
                case "iconlist": {
                    PreferencesSetting setting = new PreferencesIconList(subkey);
                    settings.put(subkey, setting);
                } break;
        
                case "colorselect": {
                    PreferencesSetting setting = new PreferencesColor(subkey);
                    settings.put(subkey, setting);
                } break;
        
                case "fontselect": {
                    PreferencesSetting setting = new PreferencesFont(subkey);
                    settings.put(subkey, setting);
                } break;
        
                case "dirselect": {
                    PreferencesSetting setting = new PreferencesDir(subkey);
                    settings.put(subkey, setting);
                } break;
        
            }
        }

        model.reload(this);
    }

    @Override
    public String toString() {
        return preferencesTree.get("name");
    }

    public HashMap<String, PreferencesSetting> getSettings() {
        return settings;
    }

    public void saveAllSettings() {
        for (PreferencesSetting setting : settings.values()) {
            setting.saveSetting();
        }

        for (Enumeration e = children(); e.hasMoreElements();) {
            PreferencesTreeEntry child = (PreferencesTreeEntry)e.nextElement();
            child.saveAllSettings();
        }
    }
}
