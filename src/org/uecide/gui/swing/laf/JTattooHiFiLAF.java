package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

import com.jtattoo.plaf.AbstractTheme;
import com.jtattoo.plaf.hifi.HiFiLookAndFeel;

public class JTattooHiFiLAF extends LookAndFeel {
    public static String getName() { return "JTattoo: HiFi"; }
    public static boolean isCompatible() { return true; }

    public static void applyLAF() {
        try {

            UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel");

            Properties p = new Properties();
            p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
            p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
            p.put("logoString", "UECIDE");
            p.put("textAntiAliasing", "on");

            AbstractTheme theme = HiFiLookAndFeel.getTheme();
            theme.setProperties(p);

        } catch (Exception e) {
            Base.error(e); 
        }
    }


}