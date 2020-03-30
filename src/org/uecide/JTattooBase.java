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
            p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
            p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
            p.put("logoString", "UECIDE");
            p.put("textAntiAliasing", "on");

            Class<?> cls = Class.forName(clsname);

            Method getTheme = cls.getMethod("getTheme");
            com.jtattoo.plaf.AbstractTheme theme = (com.jtattoo.plaf.AbstractTheme)getTheme.invoke(cls);
            theme.setProperties(p);

        } catch (Exception e) {
            Base.exception(e);
            Base.error(e);
        }
    }

    public static PropertyFile getPreferencesTree() {
        PropertyFile props = new PropertyFile();
//        props.set("theme.jtattoo.type", "section");
//        props.set("theme.jtattoo.name", "JTattoo");
//        props.set("theme.jtattoo.system.name", "Use System Decorator");
//        props.set("theme.jtattoo.system.type", "checkbox");
//        props.setBoolean("theme.jtattoo.system.default", true);
        return props;
    }

    public static boolean isCompatible() { return false; }
}
