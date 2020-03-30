package org.uecide.actions;

import org.uecide.*;

public class UploadAction extends Action {

    public UploadAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "Upload <filename>"
        };
    }

    public String getCommand() { return "upload"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {
            ctx.triggerEvent("uploadStart");
            String filename = null;

            if (args.length != 1) {
                ctx.triggerEvent("uploadFail");
                throw new SyntaxErrorActionException();
            }

            if (args[0] instanceof String) {
                filename = (String)args[0];
            } else {
                ctx.triggerEvent("uploadFail");
                throw new BadArgumentActionException();
            }

            Programmer p = ctx.getProgrammer();
            if (p == null) {
                ctx.triggerEvent("uploadFail");
                throw new ActionException("No Programmer Selected");
            }

            boolean r = p.programFile(ctx, filename);
            if (r) {
                ctx.triggerEvent("uploadFinished");
            } else {
                ctx.triggerEvent("uploadFail");
            }
            return r;
        } catch (Exception ex) {
            Debug.exception(ex);
            ex.printStackTrace();
            ctx.triggerEvent("uploadFail");
            throw new ActionException(ex.getMessage());
        }
    }
}
