package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class MetalLAF extends LookAndFeel {
    public static String getName() { return "Metal"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return true; }
}
