package org.uecide.actions;

import org.uecide.Board;
import org.uecide.Core;
import org.uecide.Compiler;
import org.uecide.Programmer;
import org.uecide.Tool;
import org.uecide.Context;
import org.uecide.LibraryManager;
import org.uecide.UECIDE;

public class RescanAllAction extends Action {

    public RescanAllAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "RescanAll"
        };
    }

    public String getCommand() { return "rescanall"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        String board = ctx.getBoard().getName();
        String core = ctx.getCore().getName();
        String compiler = ctx.getCompiler().getName();
        ctx.bullet("Reloading all internal structures.");
        ctx.bullet2("Caching files...");
        UECIDE.cacheSystemFiles();
        ctx.bullet2("Libraries...");
        LibraryManager.rescanAllLibraries();
        ctx.bullet2("Boards...");
        Board.load();
        ctx.bullet2("Cores...");
        Core.load();
        ctx.bullet2("Compilers...");
        Compiler.load();
        ctx.bullet2("Programmers...");
        Programmer.load();
        ctx.bullet2("Tools...");
        Tool.load();
        ctx.bullet("Done");
        ctx.action("RefreshGui");
        ctx.action("setCompiler", compiler);
        ctx.action("setCore", core);
        ctx.action("setBoard", board);
        return true;
    }
}
