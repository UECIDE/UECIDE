package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import mdlaf.*;
import mdlaf.themes.*;
import mdlaf.animation.*;
import java.awt.Color;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class MaterialLAF extends LookAndFeel {
    public String getName() { return "Material"; }

    public void reset(Object key) {
        UIDefaults def = UIManager.getLookAndFeelDefaults();
        UIManager.put(key, def.get(key));
    }

    public void applyLAF() {
        try {
            MaterialLookAndFeel laf = new MaterialLookAndFeel();
            UIManager.setLookAndFeel(laf);

            MaterialLookAndFeel.changeTheme(new MaterialLiteTheme());

            UIManager.put("Button.background", new Color(1f, 1f, 1f, 0f));
            UIManager.put("Button.mouseHoverColor", new Color(0f, 0f, 0f, 0.1f));
            UIManager.put("Button.backgroundCircle", true);

        } catch (Exception e) {
            UECIDE.error(e);
        }
    }
}
