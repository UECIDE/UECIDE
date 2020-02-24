package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

import com.jtattoo.plaf.AbstractTheme;
import com.jtattoo.plaf.aero.AeroLookAndFeel;

public class JTattooAeroLAF extends LookAndFeel {
    public static String getName() { return "JTattoo: Aero"; }
    public static boolean isCompatible() { return true; }

    public static void applyLAF() {
        try {

            UIManager.setLookAndFeel("com.jtattoo.plaf.aero.AeroLookAndFeel");

            Properties p = new Properties();
            p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
            p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
            p.put("logoString", "UECIDE");
            p.put("textAntiAliasing", "on");

            AbstractTheme theme = AeroLookAndFeel.getTheme();
            theme.setProperties(p);

        } catch (Exception e) {
            Base.error(e); 
        }
    }


}