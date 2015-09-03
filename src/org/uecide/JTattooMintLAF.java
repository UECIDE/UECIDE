package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooMintLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Mint");
    }

    public static String getName() { return "JTattoo: Mint"; }
    public static boolean isCompatible() { return true; }

}
