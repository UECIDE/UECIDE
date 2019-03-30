package org.uecide;

import javax.swing.UIManager;
import mdlaf.*;
import mdlaf.animation.*;
import java.awt.Color;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

class ArduinoLAF extends LookAndFeel {
    public static String getName() { return "Material"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel(new MaterialLookAndFeel());
            UIManager.put("Button.background", new Color(1f, 1f, 1f, 0f));
            UIManager.put("Button.mouseHoverColor", new Color(0f, 0f, 0f, 0.2f));

            // Arduino colour scheme
            UIManager.put("MenuBar.background", new Color(0xef, 0xf0, 0xf1));
            UIManager.put("MenuBar.foreground", new Color(0x2d, 0x30, 0x31));
            UIManager.put("ToolBar.background", new Color(0x00, 0x64, 0x68));
            UIManager.put("ToolBar.foreground", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("Button.background", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("Button.mouseHoverColor", new Color(0xff, 0xff, 0xff));

            UIManager.put("Panel.background", new Color(0x17, 0xa1, 0xa5));
            UIManager.put("TabbedPane.background", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("TabbedPane.borderHighlightColor", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("TabbedPane.darkShadow", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("TabbedPane.foreground", new Color(0x00, 0x5b, 0x5b));
            UIManager.put("TabbedPane.highlight", new Color(0xff, 0xff, 0xff));
            UIManager.put("TabbedPane.shadow", new Color(0x4d, 0xb7, 0xbb));

//            UIManager.put("ScrollBar.arrowButtonBackground",
//            UIManager.put("ScrollBar.thumbDarkShadow", 
//            UIManager.put("ScrollBar.thumbHighlight", 
//            UIManager.put("ScrollBar.thumbShadow", 
            UIManager.put("ScrollBar.thumb", new Color(0x3d, 0xae, 0xe9));
            UIManager.put("ScrollBar.track", new Color(0xb6, 0xb8, 0xba));

            UIManager.put("TabbedPane.contentAreaColor", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("TextPane.background", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("ScrollPane.background", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("Separator.background", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("Label.background", new Color(0f, 0f, 0f, 0f));

            UIManager.put("SplitPane.shadow", new Color(0x4d, 0xb7, 0xbb));
            UIManager.put("SplitPane.darkshadow", new Color(0x4d, 0xb7, 0xbb));

            UIManager.put("MenuBar.border", new EmptyBorder(0, 0, 0, 0));
            UIManager.put("ProgressBar.background", new Color(0xb6, 0xb8, 0xba));



        } catch (Exception e) {
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
