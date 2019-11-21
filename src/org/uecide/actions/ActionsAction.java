package org.uecide.actions;

import org.uecide.*;

public class ActionsAction extends Action {

    public ActionsAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "Actions"
        };
    }

    public boolean actionPerformed(Object[] args) throws ActionException {
        Action[] actions = new Action[] {
            new OpenSketchAction(ctx),
            new NewSketchAction(ctx),
            new BuildAction(ctx),
            new BuildAndUploadAction(ctx),
            new PurgeAction(ctx),
            new UploadAction(ctx),
            new SetBoardAction(ctx),
            new SetCoreAction(ctx),
            new SetCompilerAction(ctx),
            new SetProgrammerAction(ctx),
            new SetDeviceAction(ctx),
            new AbortAction(ctx),
            new SetPrefAction(ctx),
            new OpenSketchFileAction(ctx),
            new SaveSketchAction(ctx),
            new SaveSketchAsAction(ctx),
            new CloseSessionAction(ctx),
            new CloseSketchFileAction(ctx),
            new ActionsAction(ctx)
        };

        for (Action a : actions) {
            String[] usage = a.getUsage();
            for (String s : usage) {
                System.out.println(s);
            }
        }
        return true;
    }
}
