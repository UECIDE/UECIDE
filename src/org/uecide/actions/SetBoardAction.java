package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class SetBoardAction extends Action {

    public SetBoardAction(Context c) { super(c); }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {

            if (args.length != 1) {
                throw new SyntaxErrorActionException();
            }

            Board brd = null;

            if (args[0] instanceof Board) {
                brd = (Board)args[0];
            } else if (args[0] instanceof String) {
                String s = (String)args[0];
                brd = Base.boards.get(s);
            }

            if (brd == null) {
                throw new BadArgumentActionException();
            }
            ctx.setBoard(brd);

            Preferences.set("board.recent", brd.getName());

            String core = Preferences.get("board." + brd.getName() + ".core");
            if (core == null) {
                core = brd.get("core");
            }
            if (core != null) {
                ctx.action("SetCore", core);

                String pgm = Preferences.get("board." + brd.getName() + ".programmer");
                if (pgm != null) {
                    ctx.action("SetProgrammer", pgm);
                }

                String port = Preferences.get("board." + brd.getName() + ".port");
                if (port != null) {
                    ctx.action("SetDevice", port);
                }

                return true;
            }
            throw new ActionException("No Core Available");
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ActionException(ex.getMessage());
        }
    }
}
