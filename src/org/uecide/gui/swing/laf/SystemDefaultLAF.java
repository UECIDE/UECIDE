package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class SystemDefaultLAF extends LookAndFeel {
    public static String getName() { return "System Default"; }

    public static void applyLAF() {
        try {
            if (Base.isMacOS()) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "UECIDE");
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return true; }
}
