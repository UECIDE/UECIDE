package org.uecide.builtin;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.Editor;
import org.uecide.Sketch;

import java.io.File;
import java.io.IOException;

public class sketch extends BuiltinCommand {
    public void kill() {}
    public boolean main(Context ctx, String[] args) throws BuiltinCommandException {
        if (args.length == 0) {
            throw new BuiltinCommandException("Syntax Error");
        }

        String op = args[0];


        switch (op) {
            case "open":
                return open(ctx, args[1]);
            case "new":
                return newblank(ctx);
            case "save":
                return save(ctx);
            case "saveas":
                return saveas(ctx, args[1]);
            case "close":
                return close(ctx);

        }
        throw new BuiltinCommandException("Unknown sketch operation");
    }

    boolean newblank(Context ctx) throws BuiltinCommandException {
        try {
            Sketch s = new Sketch((File)null);
            Editor ed = new Editor(s);
            ed.setVisible(true);
            s.loadConfig();
            return true;
        } catch (IOException ex) {
            throw new BuiltinCommandException(ex.toString());
        }
    }

    boolean open(Context ctx, String path) throws BuiltinCommandException {
        File f = new File(path);
        if (f.exists()) {
            try {
                Sketch s = new Sketch(f);
                Editor ed = new Editor(s);
                ed.setVisible(true);
                s.loadConfig();
                Base.updateMRU(f);
                return true;
            } catch (IOException ex) {
                throw new BuiltinCommandException(ex.toString());
            }
        }
        return false;
    }


    boolean save(Context ctx) throws BuiltinCommandException {
        Sketch s = ctx.getSketch();
        try {
            return s.save();
        } catch (IOException ex) {
            throw new BuiltinCommandException(ex.toString());
        }
    }

    boolean saveas(Context ctx, String path) throws BuiltinCommandException {
        Sketch s = ctx.getSketch();
        try {
            return s.saveAs(new File(path));
        } catch (IOException ex) {
            throw new BuiltinCommandException(ex.toString());
        }
    }

    boolean close(Context ctx) throws BuiltinCommandException {
        try {
            Editor ed = ctx.getEditor();
            if (!ed.closeAllTabs()) return false;
            ed.unregisterEditor(ed);
            ed.dispose();
            return true;
        } catch (IOException ex) {
            throw new BuiltinCommandException(ex.toString());
        }
    }

}
