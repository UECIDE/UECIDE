package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import mdlaf.*;
import mdlaf.animation.*;
import java.awt.Color;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class ArduinoLAF extends LookAndFeel {
    public String getName() { return "Arduino"; }

    final static Color DARK_BLUE = new Color(0x00, 0x64, 0x68);
    final static Color MID_BLUE = new Color(0x17, 0xa1, 0xa5);
    final static Color LIGHT_BLUE = new Color(0x4d, 0xb7, 0xbb);
    final static Color LIGHT_GRAY = new Color(0xef, 0xf0, 0xf1);
    final static Color DARK_GRAY = new Color(0x2d, 0x30, 0x31);
    final static Color NOTHING = new Color(1f, 1f, 1f, 0f);
    final static Color BLACK_TINT = new Color(0f, 0f, 0f, 0.2f);
    final static Color VIBRANT_BLUE = new Color(0x3d, 0xae, 0xe9);
    final static Color WHITE = new Color(0xff, 0xff, 0xff);
    final static Color MID_GRAY = new Color(0xb6, 0xb8, 0xba);

    public void storeAndApply(Object key, Object value) {
        UIManager.put(key, value);
    }

    public void applyLAF() {
        try {

            UIManager.setLookAndFeel(new MaterialLookAndFeel());
    
            storeAndApply("Button.background",                  NOTHING);
            storeAndApply("Button.mouseHoverColor",             BLACK_TINT);

            // Arduino colour scheme
            storeAndApply("Button.background",                  LIGHT_BLUE);
            storeAndApply("Button.backgroundCircle",            true);
            storeAndApply("Button.backgroundPad",               new Integer(2));
            storeAndApply("Button.disabledColor",               MID_BLUE);
            storeAndApply("Button.mouseHoverColor",             WHITE);
            storeAndApply("CheckBox.background",                MID_BLUE);
            storeAndApply("Label.background",                   NOTHING);
            storeAndApply("MenuBar.background",                 LIGHT_GRAY);
            storeAndApply("MenuBar.foreground",                 DARK_GRAY);
            storeAndApply("OptionPane.background",              MID_BLUE);
            storeAndApply("Panel.background",                   MID_BLUE);
            storeAndApply("ProgressBar.foreground",             VIBRANT_BLUE);
            storeAndApply("ProgressBar.background",             MID_GRAY);
            storeAndApply("ScrollBar.thumb",                    VIBRANT_BLUE);
            storeAndApply("ScrollBar.thumbDarkShadow",          VIBRANT_BLUE);
            storeAndApply("ScrollBar.thumbHighlight",           VIBRANT_BLUE);
            storeAndApply("ScrollBar.thumbShadow",              VIBRANT_BLUE);
            storeAndApply("ScrollBar.track",                    MID_GRAY);
            storeAndApply("ScrollPane.background",              MID_BLUE);
            storeAndApply("Separator.background",               MID_BLUE);
            storeAndApply("SplitPane.darkshadow",               MID_BLUE);
            storeAndApply("SplitPane.shadow",                   MID_BLUE);
            storeAndApply("TabbedPane.background",              LIGHT_BLUE);
            storeAndApply("TabbedPane.borderHighlightColor",    MID_BLUE);
            storeAndApply("TabbedPane.contentAreaColor",        MID_BLUE);
            storeAndApply("TabbedPane.darkShadow",              MID_BLUE);
            storeAndApply("TabbedPane.foreground",              DARK_BLUE);
            storeAndApply("TabbedPane.highlight",               WHITE);
            storeAndApply("TabbedPane.shadow",                  MID_BLUE);
            storeAndApply("TextPane.background",                MID_BLUE);
            storeAndApply("ToolBar.background",                 DARK_BLUE);
            storeAndApply("ToolBar.foreground",                 LIGHT_BLUE);

            storeAndApply("MenuBar.border", new EmptyBorder(0, 0, 0, 0));



        } catch (Exception e) {
            UECIDE.error(e);
        }
    }
}
