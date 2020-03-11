package org.uecide.gui.swing;

import org.uecide.UECIDE;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class SketchFolderFilter extends FileFilter {
    public boolean accept(File f) {
        if(UECIDE.isSketchFolder(f)) {
            return true;
        }

        if(f.isDirectory()) {
            return true;
        }

        return false;
    }

    public String getDescription() {
        return UECIDE.i18n.string("filter.sketch");
    }
}
