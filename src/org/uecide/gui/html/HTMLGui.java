package org.uecide.gui.html;

import org.uecide.Context;
import org.uecide.gui.Gui;
import org.uecide.SketchFile;
import java.io.File;

public class HTMLGui extends Gui {

    public HTMLGui(Context c) { super(c); }

    public void open() {}
    public void close() {}
    public void streamMessage(String m) {}
    public void streamError(String m) {}
    public void streamWarning(String m) {}
    public void message(String m) {}
    public void warning(String m) {}
    public void error(String m) {}
    public void error(Throwable m) {}
    public void heading(String m) {}
    public void command(String m) {}
    public void bullet(String m) {}
    public void bullet2(String m) {}
    public void bullet3(String m) {}
    public void openSplash() {}
    public void closeSplash() {}
    public void splashMessage(String message, int percent) {}
    public void openSketchFileEditor(SketchFile f) {}
    public void closeSketchFileEditor(SketchFile f) {}
    public String askString(String question, String defaultValue) { return ""; }
    public File askSketchFilename(String question, File location) { return null; }
    public boolean askYesNo(String question) { return false; }
    public int askYesNoCancel(String question) { return 2; }
    public File askOpenSketch(String question, File location) { return null; }
    public void navigateToLine(SketchFile f, Integer lineno) {}

    public static void init() {
    }

    public static void endinit() {
    }

    @Override
    public SketchFile getActiveSketchFile() {
        return null;
    }

    @Override
    public String askPassword(String prompt, String def) {
        return askString(prompt, def);
    }

}
