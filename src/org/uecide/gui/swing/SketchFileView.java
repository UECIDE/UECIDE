package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.PropertyFile;

import java.io.File;

import javax.swing.filechooser.FileView;
import javax.swing.Icon;

public class SketchFileView extends FileView {
    public String getTypeDescription(File f) {
        if(Base.isSketchFolder(f)) {
            File sketchConfigFile = new File(f, "sketch.cfg");
            if (sketchConfigFile.exists()) {
                PropertyFile sketchConfig = new PropertyFile(sketchConfigFile);
                if ((sketchConfig.get("summary") != null) && !(sketchConfig.get("summary").equals(""))) {
                    return sketchConfig.get("summary");
                }
            }
            return Base.i18n.string("filters.sketch");
        }

        return Base.i18n.string("misc.directory");
    }

    public Boolean isTraversable(File f) {
        if(Base.isSketchFolder(f)) {
            return false;
        }

        return true;
    }

    public Icon getIcon(File f) {
        try {
            if(Base.isSketchFolder(f)) {
                return IconManager.getIcon(16, "apps.uecide");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
