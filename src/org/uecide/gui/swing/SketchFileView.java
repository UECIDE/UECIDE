package org.uecide.gui.swing;

import org.uecide.UECIDE;
import org.uecide.PropertyFile;

import java.io.File;

import javax.swing.filechooser.FileView;
import javax.swing.Icon;

public class SketchFileView extends FileView {
    public String getTypeDescription(File f) {
        if(UECIDE.isSketchFolder(f)) {
            File sketchConfigFile = new File(f, "sketch.cfg");
            if (sketchConfigFile.exists()) {
                PropertyFile sketchConfig = new PropertyFile(sketchConfigFile);
                if ((sketchConfig.get("summary") != null) && !(sketchConfig.get("summary").equals(""))) {
                    return sketchConfig.get("summary");
                }
            }
            return UECIDE.i18n.string("filters.sketch");
        }

        return UECIDE.i18n.string("misc.directory");
    }

    public Boolean isTraversable(File f) {
        if(UECIDE.isSketchFolder(f)) {
            return false;
        }

        return true;
    }

    public Icon getIcon(File f) {
        try {
            if(UECIDE.isSketchFolder(f)) {
                return IconManager.getIcon(16, "internal:uecide");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
