package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooLunaLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Luna");
    }

    public static String getName() { return "JTattoo: Luna"; }
    public static boolean isCompatible() { return true; }

}
