package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;
import java.awt.Color;

class JTattooBase extends LookAndFeel {

    public static String getName() { return "JTattoo: B0rk3n"; }

    public static void applyLAF() {
    }

    static void configureLAF(String themeName) {
        try {

            String clsname = "com.jtattoo.plaf." + themeName.toLowerCase() + "." + themeName + "LookAndFeel";
            UIManager.setLookAndFeel(clsname);
            
            Properties p = new Properties();
            p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.system") ? "off" : "on");
            p.put("logoString", "UECIDE");
            p.put("textAntiAliasing", "on");

            applyColorProperty(p, "disabledForegroundColor");
            applyColorProperty(p, "backgroundColor");
            applyColorProperty(p, "alterBackgroundColor");
            applyColorProperty(p, "foregroundColor");
            applyColorProperty(p, "backgroundColorLight");
            applyColorProperty(p, "backgroundColorDark");
            applyColorProperty(p, "inputForegroundColor");
            applyColorProperty(p, "inputBackgroundColor");
            applyColorProperty(p, "selectionForegroundColor");
            applyColorProperty(p, "selectionBackgroundColor");
            applyColorProperty(p, "selectionBackgroundColorLight");
            applyColorProperty(p, "selectionBackgroundColorDark");
            applyColorProperty(p, "frameColor");
            applyColorProperty(p, "gridColor");
            applyColorProperty(p, "focusColor");
            applyColorProperty(p, "focusCellColor");
            applyColorProperty(p, "rolloverColor");
            applyColorProperty(p, "rolloverColorLight");
            applyColorProperty(p, "rolloverColorDark");
            applyColorProperty(p, "buttonForegroundColor");
            applyColorProperty(p, "buttonBackgroundColor");
            applyColorProperty(p, "buttonColorLight");
            applyColorProperty(p, "buttonColorDark");
            applyColorProperty(p, "controlForegroundColor");
            applyColorProperty(p, "controlBackgroundColor");
            applyColorProperty(p, "controlColorLight");
            applyColorProperty(p, "controlColorDark");
            applyColorProperty(p, "windowTitleForegroundColor");
            applyColorProperty(p, "windowTitleBackgroundColor");
            applyColorProperty(p, "windowTitleColorLight");
            applyColorProperty(p, "windowTitleColorDark");
            applyColorProperty(p, "windowBorderColor");
            applyColorProperty(p, "windowIconColor");
            applyColorProperty(p, "windowIconShadowColor");
            applyColorProperty(p, "windowIconRolloverColor");
            applyColorProperty(p, "windowInactiveTitleForegroundColor");
            applyColorProperty(p, "windowTitleBackgroundColor");
            applyColorProperty(p, "windowInactiveTitleColorLight");
            applyColorProperty(p, "windowInactiveTitleColorDark");
            applyColorProperty(p, "windowInactiveBorderColor");
            applyColorProperty(p, "menuForegroundColor");
            applyColorProperty(p, "menuBackgroundColor");
            applyColorProperty(p, "menuSelectionForegroundColor");
            applyColorProperty(p, "menuSelectionBackgroundColor");
            applyColorProperty(p, "menuSelectionBackgroundColorLight");
            applyColorProperty(p, "menuSelectionBackgroundColorDark");
            applyColorProperty(p, "menuColorLight");
            applyColorProperty(p, "menuColorDark");
            applyColorProperty(p, "toolbarForegroundColor");
            applyColorProperty(p, "toolbarBackgroundColor");
            applyColorProperty(p, "toolbarColorLight");
            applyColorProperty(p, "toolbarColorDark");
            applyColorProperty(p, "tabAreaBackgroundColor");
            applyColorProperty(p, "desktopColor");
            applyColorProperty(p, "tooltipForegroundColor");
            applyColorProperty(p, "tooltipBackgroundColor");

            Class<?> cls = Class.forName(clsname);
            Class[] cArg = new Class[1];
            cArg[0] = Properties.class;
            Method mth = cls.getMethod("setCurrentTheme", cArg);
            mth.invoke(cls, p);

        } catch (Exception e) {
            Base.error(e);
        }
    }

    static void applyColorProperty(Properties p, String name) {
        PropertyFile theme = Base.getTheme();
        if (theme.get("laf.jtattoo." + name) != null) {
            Color c = theme.getColor("laf.jtattoo." + name);
            String cval = c.getRed() + " " + c.getGreen() + " " + c.getBlue();
            p.put(name, cval);
            System.err.println("Setting property " + name + " to  " + cval);
        }
    }
        

    public static PropertyFile getPreferencesTree() {
        PropertyFile props = new PropertyFile();
        props.set("theme.jtattoo.type", "section");
        props.set("theme.jtattoo.name", "JTattoo");
        props.set("theme.jtattoo.system.name", "Use System Decorator");
        props.set("theme.jtattoo.system.type", "checkbox");
        props.setBoolean("theme.jtattoo.system.default", true);
        return props;
    }

    public static boolean isCompatible() { return false; }
}
