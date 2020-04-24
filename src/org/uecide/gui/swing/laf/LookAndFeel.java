package org.uecide.gui.swing.laf;

import org.uecide.*;


import javax.swing.UIDefaults;
import javax.swing.UIManager;

public abstract class LookAndFeel {
    public static final int             STYLESHEET_DIALOG           = 1;

    public abstract String getName();
    public abstract void applyLAF();
    public PropertyFile getPreferencesTree() { return null; }
    public boolean isCompatible() { return true; }
    public String getStyleSheet(int type) { return ""; }
}
