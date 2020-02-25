package org.uecide.actions;

import org.uecide.*;
import java.io.File;
import org.uecide.Compiler;

public class SetCompilerAction extends Action {

    public SetCompilerAction(Context c) { super(c); }

    public String[] getUsage() {    
        return new String[] {
            "SetCompiler <codename>"
        };
    }

    public String getCommand() { return "setcompiler"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {

            if (args.length != 1) {
                throw new SyntaxErrorActionException();
            }

            if (args[0] instanceof Compiler) {
                ctx.setCompiler((Compiler)args[0]);
                Preferences.set("board." + ctx.getBoard().getName() + ".compiler", ctx.getCompiler().getName());
                return true;
            } else if (args[0] instanceof String) {
                String s = (String)args[0];
                Compiler b = Base.compilers.get(s);
                if (b == null) {
                    throw new ActionException("Unknown Compiler");
                }
                ctx.setCompiler(b);
                Preferences.set("board." + ctx.getBoard().getName() + ".compiler", ctx.getCompiler().getName());
                return true;
            }
            throw new BadArgumentActionException();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ActionException(ex.getMessage());
        }
    }
}
