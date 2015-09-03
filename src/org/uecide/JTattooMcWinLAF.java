package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooMcWinLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("McWin");
    }

    public static String getName() { return "JTattoo: McWin"; }
    public static boolean isCompatible() { return true; }

}
