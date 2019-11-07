package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

public class TinyNightlyLAF extends LookAndFeel {
    public static String getName() { return "Tiny: Nightly"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel("de.muntjak.tinylookandfeel.TinyLookAndFeel");


            de.muntjak.tinylookandfeel.ThemeDescription[] tinyThemes = de.muntjak.tinylookandfeel.Theme.getAvailableThemes();

            for(de.muntjak.tinylookandfeel.ThemeDescription td : tinyThemes) {
                if(td.getName().equals("Nightly")) {
                    de.muntjak.tinylookandfeel.Theme.loadTheme(td);
                    break;
                }
            }

        } catch (Exception e) {
            Base.error(e);
        }

    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return true; }
}
