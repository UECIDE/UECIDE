package org.uecide;

import javax.swing.UIManager;

class Office2003LAF extends LookAndFeel {
    public static String getName() { return "Office 2003"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel("org.fife.plaf.Office2003.Office2003LookAndFeel");
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return Base.isWindows(); }
}
