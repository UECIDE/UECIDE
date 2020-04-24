package org.uecide.gui.swing;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

public class PreferencesTreeModel extends DefaultTreeModel {
    PreferencesTreeEntry rootNode;

    public PreferencesTreeModel() {
        super(new DefaultMutableTreeNode("dummy"));
        rootNode = new PreferencesTreeEntry(this, null);
        setRoot(rootNode);
    }

    public void saveAllSettings() {
        rootNode.saveAllSettings();
    }
}
