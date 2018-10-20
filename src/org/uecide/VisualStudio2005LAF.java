package org.uecide;

import javax.swing.UIManager;

class VisualStudio2005LAF extends LookAndFeel {
    public static String getName() { return "Office XP"; }

    public static void applyLAF() {
        if (!Base.isWindows()) {
            Base.error("The selected Look and Feel is only compatible with Windows. Select another.");
            return;
        }
        try {
            UIManager.setLookAndFeel("org.fife.plaf.VisualStudio2005.VisualStudio2005LookAndFeel");
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return Base.isWindows(); }
}
