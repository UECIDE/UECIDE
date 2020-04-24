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

public class MaterialDarkLAF extends LookAndFeel {
    public String getName() { return "Material (Dark)"; }

    public void reset(Object key) {
        UIDefaults def = UIManager.getLookAndFeelDefaults();
        UIManager.put(key, def.get(key));
    }

    public void applyLAF() {
        try {
            UIManager.put("Tree.rendererFillBackground", false);
            MaterialLookAndFeel laf = new MaterialLookAndFeel();
            UIManager.setLookAndFeel(laf);
            MaterialLookAndFeel.changeTheme(new JMarsDarkTheme());
            UIManager.put("Tree.foreground", Color.WHITE);
        } catch (Exception e) {
            UECIDE.error(e);
        }
    }
}
