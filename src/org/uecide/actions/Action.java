package org.uecide.actions;

import org.uecide.*;

public abstract class Action {

    public Context ctx;

    public Action(Context c) {
        ctx = c;
    }

    public abstract boolean actionPerformed(Object[] args) throws ActionException;

    public static boolean run(Context c, String name, Object[] args) {
        Action action = null;

        // I must find a better way to do this...
        switch (name.toLowerCase()) {
            case "opensketch": action = new OpenSketchAction(c); break;
            case "newsketch": action = new NewSketchAction(c); break;
            case "build": action = new BuildAction(c); break;
            case "buildandupload": action = new BuildAndUploadAction(c); break;
            case "purge": action = new PurgeAction(c); break;
            case "upload": action = new UploadAction(c); break;
            case "setboard": action = new SetBoardAction(c); break;
            case "setcore": action = new SetCoreAction(c); break;
            case "setcompiler": action = new SetCompilerAction(c); break;
            case "setprogrammer": action = new SetProgrammerAction(c); break;
            case "setdevice": action = new SetDeviceAction(c); break;
            case "abort": action = new AbortAction(c); break;
            case "setpref": action = new SetPrefAction(c); break;
            case "opensketchfile": action = new OpenSketchFileAction(c); break;
            case "savesketch": action = new SaveSketchAction(c); break;
            case "savesketchas": action = new SaveSketchAsAction(c); break;
            case "closesession": action = new CloseSessionAction(c); break;
            case "closesketchfile": action = new CloseSketchFileAction(c); break;
            case "actions": action = new ActionsAction(c); break;
            case "addlibrary": action = new AddLibraryAction(c); break;
            case "addlibrarylocation": action = new AddLibraryLocationAction(c); break;
            case "rescanlibraries": action = new RescanLibrariesAction(c); break;
            case "reloadfiles": action = new ReloadFilesAction(c); break;
            case "newsketchfile": action = new NewSketchFileAction(c); break;
            default:
                c.error("Unknown action " + name);
                return false;
        }

        try {
            return action.actionPerformed(args);
        } catch (ActionException ex) {
            c.error(ex.toString());
        }
        return false;
    }

    public abstract String[] getUsage();

}
