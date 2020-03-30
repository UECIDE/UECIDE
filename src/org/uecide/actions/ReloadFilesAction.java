package org.uecide.actions;

import org.uecide.Context;
import org.uecide.Debug;
import org.uecide.Sketch;
import org.uecide.SketchFile;

import java.io.IOException;

public class ReloadFilesAction extends Action {

    public ReloadFilesAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "ReloadFiles"
        };
    }

    public String getCommand() { return "reloadfiles"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        for (SketchFile f : ctx.getSketch().getSketchFiles().values()) {
            try {
                f.loadFileData();
            } catch (IOException ex) {
                Debug.exception(ex);
                throw new ActionException(ex.getMessage());
            }
        }
        return true;
    }
}
