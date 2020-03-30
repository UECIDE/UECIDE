package org.uecide;

import javax.swing.UIManager;

class LiquidLAF extends LookAndFeel {
    public static String getName() { return "Liquid"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel("com.birosoft.liquid.LiquidLookAndFeel");
        } catch (Exception e) {
            Base.exception(e);
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return true; }
}
