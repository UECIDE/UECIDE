package org.uecide.gui;

import org.uecide.Context;
import org.uecide.SketchFile;
import java.io.File;

public abstract class Gui {

    protected Context ctx;

    public Gui(Context c) {
        ctx = c;
    }

    public void setContext(Context c) { ctx = c; }
    public Context getContext() { return ctx; }


    public abstract void open();
    public abstract void close();
    public abstract void streamMessage(String m);
    public abstract void streamError(String m);
    public abstract void streamWarning(String m);
    public abstract void message(String m);
    public abstract void warning(String m);
    public abstract void error(String m);
    public abstract void error(Throwable m);
    public abstract void heading(String m);
    public abstract void command(String m);
    public abstract void bullet(String m);
    public abstract void bullet2(String m);
    public abstract void bullet3(String m);
    public abstract void openSplash();
    public abstract void closeSplash();
    public abstract void splashMessage(String message, int percent);
    public abstract void openSketchFileEditor(SketchFile f);
    public abstract void closeSketchFileEditor(SketchFile f);
    public abstract String askString(String question, String defaultValue);
    public abstract File askSketchFilename(String question, File location);
    public abstract boolean askYesNo(String question);
    public abstract int askYesNoCancel(String question);
    public abstract File askOpenSketch(String question, File location);
    public abstract void navigateToLine(SketchFile f, Integer lineno);

    public boolean isEphemeral() {
        return false;
    }

    public void alert(String message) {
        error(message);
    }

    public boolean shouldAutoOpen() {
        return false;
    }

}
