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
        Board board = ctx.getBoard();
        Core core = ctx.getCore();
        Compiler compiler = ctx.getCompiler();
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
        if (compiler != null) ctx.action("setCompiler", compiler);
        if (core != null) ctx.action("setCore", core);
        if (board != null) ctx.action("setBoard", board);
        return true;
    }
}
