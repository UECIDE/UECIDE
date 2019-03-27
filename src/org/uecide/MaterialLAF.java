package org.uecide;

import javax.swing.UIManager;
import mdlaf.*;
import mdlaf.animation.*;

class MaterialLAF extends LookAndFeel {
    public static String getName() { return "Material"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel(new MaterialLookAndFeel());
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return true; }
}
