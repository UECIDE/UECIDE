package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

import com.jtattoo.plaf.AbstractTheme;
import com.jtattoo.plaf.smart.SmartLookAndFeel;

public class JTattooSmartLAF extends LookAndFeel {
    public static String getName() { return "JTattoo: Smart"; }
    public static boolean isCompatible() { return true; }

    public static void applyLAF() {
        try {

            UIManager.setLookAndFeel("com.jtattoo.plaf.smart.SmartLookAndFeel");

            Properties p = new Properties();
            p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
            p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
            p.put("logoString", "UECIDE");
            p.put("textAntiAliasing", "on");

            AbstractTheme theme = SmartLookAndFeel.getTheme();
            theme.setProperties(p);

        } catch (Exception e) {
            Base.error(e); 
        }
    }


}
