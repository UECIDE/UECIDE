package org.uecide;

import javax.swing.UIManager;

class GnomeLAF extends LookAndFeel {
    public static String getName() { return "Gnome"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (Exception e) {
            Base.exception(e);
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return Base.isLinux(); }
}
