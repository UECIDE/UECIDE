package org.uecide.gui.cli;

import org.uecide.gui.*;
import org.uecide.*;
import org.uecide.actions.*;

import org.jline.reader.*;
import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.builtins.Completers.TreeCompleter;

import java.util.*;
import java.io.*;

import org.uecide.Compiler;
import org.uecide.Package;

public class CliGui extends Gui {

    Context ctx;

    LineReader _reader;
    Completer _completer = null;
    APT _apt;

    public CliGui(Context c) {
        ctx = c;
    }

    public void setContext(Context c) { ctx = c; }
    public Context getContext() { return ctx; }

    void updateComplete() {
        ArrayList<String> flist = new ArrayList<String>();
        for (SketchFile f : ctx.getSketch().getSketchFiles().values()) {
            flist.add(f.toString());
        }

        String[] fileNames = flist.toArray(new String[0]);


        ArrayList<String> brdList = new ArrayList<String>();
        for (Board b : Base.boards.values()) {
            brdList.add(b.getName());
        }
        String[] boardNames = brdList.toArray(new String[0]);

        String family = "none";
        Board b = ctx.getBoard();
        if (b != null) {
            family = ctx.getBoard().getFamily();
        }

        ArrayList<String> corList = new ArrayList<String>();
        for (Core c : Base.cores.values()) {
            if (c.getFamily().equals(family)) {
                corList.add(c.getName());
            }
        }
        String[] coreNames = corList.toArray(new String[0]);

        ArrayList<String> comList = new ArrayList<String>();
        for (Compiler c : Base.compilers.values()) {
            if (c.getFamily().equals(family)) {
                comList.add(c.getName());
            }
        }
        String[] compNames = comList.toArray(new String[0]);

        ArrayList<String> uninstalledPackages = new ArrayList<String>();
        ArrayList<String> installedPackages = new ArrayList<String>();


        Package[] pkgs = _apt.getPackages();

        for (Package p : pkgs) {
            Package instPack = _apt.getInstalledPackage(p.getName());
            if (instPack != null) {
                installedPackages.add(p.getName());
            } else {
                uninstalledPackages.add(p.getName());
            }
        }



        TreeMap<String, Programmer> pl = new TreeMap<String, Programmer>();

        for (String k : Base.programmers.keySet()) {
            Programmer p = Base.programmers.get(k);
            if (p.worksWith(ctx.getBoard())) {
                pl.put(k, p);
            }
        }
       _completer = new TreeCompleter(
            TreeCompleter.node(
                new StringsCompleter(new String[] { "vi", "edit" }),
                    TreeCompleter.node(new StringsCompleter(fileNames))
            ),
            TreeCompleter.node(
                new StringsCompleter((new String[] { "ls", "dir", "quit", "compile", "make", "upload", "info", "purge", "rescan", "pkg-update", "help" }))
            ),
            TreeCompleter.node(
                new StringsCompleter("pkg-install"), TreeCompleter.node(
                    new StringsCompleter(uninstalledPackages.toArray(new String[0]))
                )
            ),
            TreeCompleter.node(
                new StringsCompleter("pkg-remove"), TreeCompleter.node(
                    new StringsCompleter(installedPackages.toArray(new String[0]))
                )
            ),
            TreeCompleter.node(
                new StringsCompleter("load"), TreeCompleter.node(
                    new FileNameCompleter()
                )
            ),
           TreeCompleter.node(
                new StringsCompleter("board"), TreeCompleter.node(
                    new StringsCompleter(boardNames)
                )
            ),
            TreeCompleter.node(
                new StringsCompleter("core"), TreeCompleter.node(
                    new StringsCompleter(coreNames)
                )
            ),
            TreeCompleter.node(
                new StringsCompleter("compiler"), TreeCompleter.node(
                    new StringsCompleter(compNames)
                )
            ),
            TreeCompleter.node(
                new StringsCompleter("programmer"), TreeCompleter.node(
                    new StringsCompleter(pl.keySet().toArray(new String[0]))
                )
            ),
            TreeCompleter.node(
                new StringsCompleter("port"), TreeCompleter.node(
                    new StringsCompleter(Serial.getPortList().toArray(new String[0]))
                )
            ),
            TreeCompleter.node(
                new StringsCompleter(new String[] { "verbose" }), TreeCompleter.node(
                    new StringsCompleter(new String[] { "on", "off" })
                )
            )
        );
    }

    String genPrompt() {
        return ctx.getSketch().getName();
    }

    public void open() {
        String line;

        try {
            _apt = APT.factory();
        } catch (Exception ex) {
            ctx.error(ex);
        }

        updateComplete();
        _reader = LineReaderBuilder.builder().completer(_completer).build();

        info();

        try {
            while ((line = _reader.readLine(genPrompt() + "> ")) != null) {
                line = line.trim();
                if (line.equals("")) {
                    continue;
                }
                String[] args = line.split(" ");
                String command = args[0].toLowerCase();

                switch (command) {
                    case "make":
                        if (args.length == 1) {
                            ctx.action("build");
                        } else if (args[1].equals("clean")) {
                            ctx.action("purge");
                        } else if (args[1].equals("install")) {
                            ctx.action("build");
                            ctx.action("upload", ctx.getSketch().getName());
                        }
                        break;
                    case "compile":
                        ctx.action("build");
                        break;
                    case "purge":
                        ctx.action("purge");
                        break;
                    case "ls":
                    case "dir":
                        for (SketchFile f : ctx.getSketch().getSketchFiles().values()) {
                            ctx.message(f.toString());
                        }
                        break;
                    case "edit":
                    case "vi":
                        editFile(args[1]);
                        break;
                
                    case "quit":
                        Preferences.save();
                        System.exit(0);
                        break;

                    case "info":
                        info();
                        break;

                    case "board":
                        if (args.length != 2) {
                            ctx.error("Usage: board <name>");
                        } else {
                            ctx.action("SetBoard", args[1]);
                        }
                        break;

                    case "core":
                        if (args.length != 2) {
                            ctx.error("Usage: core <name>");
                        } else {
                            ctx.action("SetCore", args[1]);
                        }
                        break;

                    case "compiler":
                        if (args.length != 2) {
                            ctx.error("Usage: compiler <name>");
                        } else {
                            ctx.action("SetCompiler", args[1]);
                        }
                        break;

                    case "programmer":
                        if (args.length != 2) {
                            ctx.error("Usage: programmer <name>");
                        } else {
                            ctx.action("SetProgrammer", args[1]);
                        }
                        break;

                    case "port":
                        if (args.length != 2) {
                            ctx.error("Usage: port <name>");
                        } else {
                            ctx.action("SetPort", args[1]);
                        }
                        break;

                    default:
                        ctx.error("Unknown command");
                        break;
                }
            }
        } catch (UserInterruptException ex) {
            message("Quit");
            System.exit(10);
        } catch (EndOfFileException ex) {
            Preferences.save();
            message("Bye");
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Preferences.save();
            System.exit(10);
        }
    }


    public void message(String m) {
        System.out.println(m);
    }

    public void warning(String m) {
        if (!Base.isWindows()) {
            System.out.println("[33m" + m + "[0m");
        } else {
            System.out.println(m);
        }
    }

    public void error(String m) {
        if (!Base.isWindows()) {
            System.err.println("[31m" + m + "[0m");
        } else {
            System.err.println(m);
        }
    }

    public void error(Throwable m) {
        if (!Base.isWindows()) {
            System.err.print("[31m");
        } 
        m.printStackTrace();
        if (!Base.isWindows()) {
            System.err.print("[0m");
        } 
    }

    public void heading(String m) {
        System.out.println(m);
        for (int i = 0; i < m.length(); i++) {
            System.out.print("=");
        }
        System.out.println();
    }

    public void command(String m) {
        if (!Base.isWindows()) {
            System.out.println("[32m" + m + "[0m");
        } else {
            System.out.println(m);
        }
    }

    public void bullet(String m) {
        System.out.println(" * " + m);
    }

    public void bullet2(String m) {
        System.out.println("   * " + m);
    }

    public void bullet3(String m) {
        System.out.println("     * " + m);
    }

    public void openSplash() {
        System.out.println("UECIDE version 0.11.0");
        System.out.println("(c) 2019 Majenko Technologies");
    }

    public void closeSplash() {
        System.out.println();
    }

    public void splashMessage(String message, int percent) {
        System.out.println(" * " + message);
    }



    void editFile(String fn) {
        fn = fn.trim();
        for (SketchFile f : ctx.getSketch().getSketchFiles().values()) {
            if (f.toString().equals(fn)) {
                try {

                    String [] commandline = new String[3];
                    commandline[0] = "/bin/bash";
                    commandline[1] = "-c";
                    commandline[2] = "vi </dev/tty >/dev/tty '" + f.getFile().getAbsolutePath() + "'";
                    Process p = Runtime.getRuntime().exec(commandline);

                    p.waitFor();
                    ctx.getSketch().rescanFileTree();
                } catch (Exception e) {
                    ctx.error(e);
                }
                return;
            }
        }
        ctx.error("File '" + fn + "' not found!");
    }

    void info() {

        if (ctx.getBoard() == null) {
            System.out.println("Board:      No Board Selected!!!");
        } else {
            System.out.println("Board:      " + ctx.getBoard().getName() + " (" + ctx.getBoard() + ")");
        }
        if (ctx.getCore() == null) {
            System.out.println("Core:       No Core Selected!!!");
        } else {
            System.out.println("Core:       " + ctx.getCore().getName() + " (" + ctx.getCore() + ")");
        }
        if (ctx.getCompiler() == null) {
            System.out.println("Compiler:   No Compiler Selected!!!");
        } else {
            System.out.println("Compiler:   " + ctx.getCompiler().getName() + " (" + ctx.getCompiler() + ")");
        }
        if (ctx.getProgrammer() == null) {
            System.out.println("Programmer: No Programmer Selected!!!");
        } else {
            System.out.println("Programmer: " + ctx.getProgrammer().getName() + " (" + ctx.getProgrammer() + ")");
        }
        if (ctx.getDevice() == null) {
            System.out.println("Port:       No Port Selected!!!");
        } else {
            System.out.println("Port:       " + ctx.getDevice().getName());
        }
    }

    public static void init() {
    }

}
