package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;
import java.io.IOException;

public class JTattooHiFiLAF extends JTattooLAF {
    public String getName() { return "JTattoo: HiFi"; }

    public void applyLAF() {
        doApplyLAF("HiFi");
    }

    @Override
    public String getStyleSheet(int type) { 
        switch (type) {
            case STYLESHEET_DIALOG:
                return getStyleSheetDialog();
        }
        return ""; 
    }


    static String styleSheetDialog = null;
    public String getStyleSheetDialog() {
        if (styleSheetDialog == null) {
            try {
                styleSheetDialog = FileManager.loadTextFile("res://org/uecide/gui/swing/laf/JTattooHiFiLAF/dialog.css");
            } catch (IOException ex) {
                styleSheetDialog = "";
                Debug.exception(ex);
            }
        }
        return styleSheetDialog;
    }
}
