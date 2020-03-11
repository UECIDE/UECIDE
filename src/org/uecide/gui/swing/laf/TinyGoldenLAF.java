package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

public class TinyGoldenLAF extends LookAndFeel {
    public String getName() { return "Tiny: Golden"; }

    public void applyLAF() {
        try {
            UIManager.setLookAndFeel("de.muntjak.tinylookandfeel.TinyLookAndFeel");


            de.muntjak.tinylookandfeel.ThemeDescription[] tinyThemes = de.muntjak.tinylookandfeel.Theme.getAvailableThemes();

            for(de.muntjak.tinylookandfeel.ThemeDescription td : tinyThemes) {
                if(td.getName().equals("Golden")) {
                    de.muntjak.tinylookandfeel.Theme.loadTheme(td);
                    break;
                }
            }

        } catch (Exception e) {
            UECIDE.error(e);
        }

    }

}
