package org.uecide;

import javax.swing.UIManager;
import mdlaf.*;
import mdlaf.animation.*;
import java.awt.Color;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

class ArduinoLAF extends LookAndFeel {
    public static String getName() { return "Arduino"; }

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

    public static void applyLAF() {
        try {

            UIManager.setLookAndFeel(new MaterialLookAndFeel());

            UIManager.put("Button.background",                  NOTHING);
            UIManager.put("Button.mouseHoverColor",             BLACK_TINT);

            // Arduino colour scheme
            UIManager.put("Button.background",                  LIGHT_BLUE);
            UIManager.put("Button.backgroundCircle",            true);
            UIManager.put("Button.backgroundPad",               new Integer(2));
            UIManager.put("Button.disabledColor",               MID_BLUE);
            UIManager.put("Button.mouseHoverColor",             WHITE);
            UIManager.put("CheckBox.background",                MID_BLUE);
            UIManager.put("Label.background",                   NOTHING);
            UIManager.put("MenuBar.background",                 LIGHT_GRAY);
            UIManager.put("MenuBar.foreground",                 DARK_GRAY);
            UIManager.put("OptionPane.background",              MID_BLUE);
            UIManager.put("Panel.background",                   MID_BLUE);
            UIManager.put("ProgressBar.foreground",             VIBRANT_BLUE);
            UIManager.put("ProgressBar.background",             MID_GRAY);
            UIManager.put("ScrollBar.thumb",                    VIBRANT_BLUE);
            UIManager.put("ScrollBar.thumbDarkShadow",          VIBRANT_BLUE);
            UIManager.put("ScrollBar.thumbHighlight",           VIBRANT_BLUE);
            UIManager.put("ScrollBar.thumbShadow",              VIBRANT_BLUE);
            UIManager.put("ScrollBar.track",                    MID_GRAY);
            UIManager.put("ScrollPane.background",              MID_BLUE);
            UIManager.put("Separator.background",               MID_BLUE);
            UIManager.put("SplitPane.darkshadow",               MID_BLUE);
            UIManager.put("SplitPane.shadow",                   MID_BLUE);
            UIManager.put("TabbedPane.background",              LIGHT_BLUE);
            UIManager.put("TabbedPane.borderHighlightColor",    MID_BLUE);
            UIManager.put("TabbedPane.contentAreaColor",        MID_BLUE);
            UIManager.put("TabbedPane.darkShadow",              MID_BLUE);
            UIManager.put("TabbedPane.foreground",              DARK_BLUE);
            UIManager.put("TabbedPane.highlight",               WHITE);
            UIManager.put("TabbedPane.shadow",                  MID_BLUE);
            UIManager.put("TextPane.background",                MID_BLUE);
            UIManager.put("ToolBar.background",                 DARK_BLUE);
            UIManager.put("ToolBar.foreground",                 LIGHT_BLUE);

            UIManager.put("MenuBar.border", new EmptyBorder(0, 0, 0, 0));



        } catch (Exception e) {
            Base.exception(e);
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public static boolean isCompatible() { return true; }
}
/*
Button.background
Button.foreground
Button.mouseHoverColor
CheckBox.background
CheckBox.foreground
CheckBoxMenuItem.background
ComboBox.background
ComboBox.buttonBackground
ComboBox.foreground
ComboBox.mouseHoverColor
ComboBox.selectedInDropDownBackground
Control
FormattedTextField.inactiveBackground
FormattedTextField.inactiveForeground
FormattedTextField.selectionBackground
FormattedTextField.selectionForeground
Label.background
Label.foreground
Menu.background
MenuBar.background
MenuBar.foreground
Menu.foreground
MenuItem.background
MenuItem.foreground
Panel.background
PopupMenu.background
PopupMenu.foreground
RadioButton.background
RadioButton.foreground
RadioButtonMenuItem.background
ScrollBar.arrowButtonBackground
ScrollBar.thumb
ScrollBar.thumbDarkShadow
ScrollBar.thumbHighlight
ScrollBar.thumbShadow
ScrollBar.track
Slider.background
Slider.foreground
Slider.trackColor
Spinner.arrowButtonBackground
Spinner.background
Spinner.foreground
Spinner.mouseHoverColor
TabbedPane.background
TabbedPane.borderHighlightColor
TabbedPane.darkShadow
TabbedPane.foreground
TabbedPane.highlight
TabbedPane.shadow
Table.background
Table.foreground
Table.gridColor
TableHeader.background
Table.selectionBackground
Table.selectionForeground
TaskPane.borderColor
TaskPane.contentBackground
TextField.inactiveBackground
TextField.inactiveForeground
TextField.selectionBackground
TextField.selectionForeground
ToggleButton.background
ToggleButton.foreground
ToolBar.background
ToolBar.dockingBackground
ToolBar.floatingBackground
ToolBar.foreground
Tree.background
Tree.foreground
Tree.selectionBackground
Tree.selectionBorderColor
Tree.selectionForeground
*/
