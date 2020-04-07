package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class SetCoreAction extends Action {

    public SetCoreAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "SetCore <codename>"
        };
    }

    public String getCommand() { return "setcore"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {

            if (args.length != 1) {
                throw new SyntaxErrorActionException();
            }

            Core core = null;

            if (args[0] instanceof Core) {
                core = (Core)args[0];
            } else if (args[0] instanceof String) {
                String s = (String)args[0];
                core = Core.getCore(s);
            }

            if (core == null) {
                throw new BadArgumentActionException();
            }

            ctx.setCore(core);
            Preferences.set("board." + ctx.getBoard().getName() + ".core", core.getName());

            String compiler = Preferences.get("board." + ctx.getBoard().getName() + ".compiler");
            if (compiler == null) {
                compiler = core.get("compiler");
            }
            if (compiler != null) {
                ctx.action("SetCompiler", compiler);
                return true;
            }
            throw new ActionException("No Compiler Available");
        } catch (Exception ex) {
            Debug.exception(ex);
            throw new ActionException(ex.getMessage());
        }
    }
}
