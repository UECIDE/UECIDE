package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

import java.lang.reflect.Method;

public abstract class JTattooLAF extends LookAndFeel {
    public void doApplyLAF(String name) {
        try {
            Properties p = new Properties();
            p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
            p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
            p.put("logoString", "UECIDE");
            p.put("textAntiAliasing", "on");

            String fontData = Preferences.get("theme.jtattoo.aafont");
            String colourData = Preferences.get("theme.jtattoo.themes." + name.toLowerCase());

            if (fontData == null) fontData = "Default";
            if (colourData == null) colourData = "Default";

            String full = colourData + "-" + fontData;

            full = full.replace("Default-", "");
            full = full.replace("-Default", "");

            String className = "com.jtattoo.plaf." + name.toLowerCase() + "." + name + "LookAndFeel";

            Class<?> c = Class.forName(className);

            Method m1 = c.getMethod("setTheme", String.class);
            Method m2 = c.getMethod("setTheme", Properties.class);

            m1.invoke(null, full);
            m2.invoke(null, p);
            
            UIManager.setLookAndFeel(className);

        } catch (Exception e) {
            UECIDE.error(e); 
        }
    }
}
