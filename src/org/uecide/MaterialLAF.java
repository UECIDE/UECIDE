package org.uecide;

import javax.swing.UIManager;
import mdlaf.*;
import mdlaf.animation.*;
import java.awt.Color;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

class MaterialLAF extends LookAndFeel {
    public static String getName() { return "Material"; }

    public static void applyLAF() {
        try {
            UIManager.setLookAndFeel(new MaterialLookAndFeel());
            UIManager.put("Button.background", new Color(1f, 1f, 1f, 0f));
            UIManager.put("Button.mouseHoverColor", new Color(0f, 0f, 0f, 0.1f));
            UIManager.put("Button.backgroundCircle", true);

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
