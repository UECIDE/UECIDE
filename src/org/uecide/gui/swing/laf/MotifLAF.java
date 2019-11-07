package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class MotifLAF extends LookAndFeel {
    public static String getName() { return "Motif"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return true; }
}
