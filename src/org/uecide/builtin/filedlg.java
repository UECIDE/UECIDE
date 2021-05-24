package org.uecide.builtin;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.Preferences;
import org.uecide.SketchFileView;
import org.uecide.SketchFolderFilter;

import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import java.io.File;

public class filedlg extends BuiltinCommand {
    public void kill() {}
    public boolean main(Context ctx, String[] args) throws BuiltinCommandException {
        if (args.length < 2) {
            throw new BuiltinCommandException("Syntax Error");
        }

        if (ctx.getEditor() == null) {
            throw new BuiltinCommandException("Unable to save when not in editor");
        }

        switch (args[0]) {
            case "savedir":
                break;
            case "savesdir":
                return savesdir(ctx, args);
            case "opendir":
                break;
            case "opensdir":
                break;
            case "savefile":
                break;
            case "openfile":
                break;
            default:
                throw new BuiltinCommandException("Invalid filedlg operation");
        }
        return false;
    }


    boolean savesdir(Context ctx, String[] args) {
        JFileChooser fc = new JFileChooser();
        FileFilter filter = new SketchFolderFilter();
        fc.setFileFilter(filter);

        FileView view = new SketchFileView();
        fc.setFileView(view);

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.savesketch");
            if (loc == null) {
                loc = Base.getSketchbookFolder();
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(Base.getSketchbookFolder());
        }

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        int rv = fc.showSaveDialog(ctx.getSketch().getEditor());

        if(rv == JFileChooser.APPROVE_OPTION) {
            File newFile = fc.getSelectedFile();

            if(newFile.exists()) {
                int n = ctx.getEditor().twoOptionBox(
                    JOptionPane.WARNING_MESSAGE,
                    Base.i18n.string("msg.overwrite.title"),
                    Base.i18n.string("msg.overwrite.body", newFile.getName()),
                    Base.i18n.string("misc.yes"),
                    Base.i18n.string("misc.no")
                );

                if(n != 0) {
                    return false;
                }

                Base.tryDelete(newFile);
            }

            if (Preferences.getBoolean("editor.save.remloc")) {
                Preferences.setFile("editor.locations.savesketch", newFile.getParentFile());
            }
            ctx.set(args[1], newFile.getAbsolutePath());
            return true;
        }
        return false;
    }
}


