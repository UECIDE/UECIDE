package org.uecide.gui.cli;

import org.uecide.APT;
import org.uecide.Base;
import org.uecide.Board;
import org.uecide.Compiler;
import org.uecide.Context;
import org.uecide.Core;
import org.uecide.Package;
import org.uecide.Preferences;
import org.uecide.Programmer;
import org.uecide.Serial;
import org.uecide.SketchFile;

import org.uecide.gui.Gui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.builtins.Completers.TreeCompleter;

import java.util.*;

import org.uecide.Compiler;
import org.uecide.Package;

public class CliGui extends Gui {
    LineReader _reader;
    Completer _completer = null;
    APT _apt;

    public CliGui(Context c) {
        super(c);
    }

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

    @Override
    public void close() {
        // We can only have one instance running. Since there's no way to interrupt the 
        // line reader we'll just quit here.
        System.exit(0);
    }

    @Override
    public void open() {
        String line;

        try {
            _apt = APT.factory();
        } catch (IOException ex) {
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
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            Preferences.save();
//            System.exit(10);
        }
    }


    @Override
    public void message(String m) {
        System.out.println(m);
    }

    @Override
    public void warning(String m) {
        if (!Base.isWindows()) {
            System.out.println("[33m" + m + "[0m");
        } else {
            System.out.println(m);
        }
    }

    @Override
    public void error(String m) {
        if (!Base.isWindows()) {
            System.err.println("[31m" + m + "[0m");
        } else {
            System.err.println(m);
        }
    }

    @Override
    public void error(Throwable m) {
        if (!Base.isWindows()) {
            System.err.print("[31m");
        } 
        m.printStackTrace();
        if (!Base.isWindows()) {
            System.err.print("[0m");
        } 
    }

    @Override
    public void heading(String m) {
        System.out.println(m);
        for (int i = 0; i < m.length(); i++) {
            System.out.print("=");
        }
        System.out.println();
    }

    @Override
    public void command(String m) {
        if (!Base.isWindows()) {
            System.out.println("[32m" + m + "[0m");
        } else {
            System.out.println(m);
        }
    }

    @Override
    public void bullet(String m) {
        System.out.println(" * " + m);
    }

    @Override
    public void bullet2(String m) {
        System.out.println("   * " + m);
    }

    @Override
    public void bullet3(String m) {
        System.out.println("     * " + m);
    }

    @Override
    public void openSplash() {
        System.out.println("UECIDE version 0.11.0");
        System.out.println("(c) 2019 Majenko Technologies");
    }

    @Override
    public void closeSplash() {
        System.out.println();
    }

    @Override
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
                } catch (InterruptedException e) {
                    ctx.error(e);
                } catch (IOException e) {
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

    public static void endinit() {
    }

    @Override
    public boolean askYesNo(String question) {
        System.out.print(question + " (Y/N) ");
        try {
            int ch = System.in.read();
            if ((ch == 'n') || (ch == 'N')) {
                System.out.println("No");
                return false;
            }
            if ((ch == 'y') || (ch == 'Y')) {
                System.out.println("Yes");
                return true;
            }
        } catch (IOException ex) {
            ctx.error(ex);
        }
        return false;
    }

    @Override
    public int askYesNoCancel(String question) {
        System.out.print(question + " (Y/N/C) ");
        try {
            int ch = System.in.read();
            if ((ch == 'y') || (ch == 'Y')) {
                System.out.println("Yes");
                return 0;
            }
            if ((ch == 'n') || (ch == 'N')) {
                System.out.println("No");
                return 1;
            }
            if ((ch == 'c') || (ch == 'C')) {
                System.out.println("Cancel");
                return 2;
            }
        } catch (IOException ex) {
            ctx.error(ex);
        }
        return -1;
    }

    @Override
    public File askSketchFilename(String question, File location) {
        System.out.print(question + " ");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String fn = r.readLine();
            if (fn == null) return null;
            if (fn.equals("")) return null;
            File outFile = new File(location, fn);
            return outFile;
        } catch (IOException ex) {
            ctx.error(ex);
        }
        return null;
    }

    @Override
    public File askOpenSketch(String question, File location) {
        System.out.print(question + " ");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String fn = r.readLine();
            if (fn == null) return null;
            if (fn.equals("")) return null;
            File outFile = new File(location, fn);
            return outFile;
        } catch (IOException ex) {
            ctx.error(ex);
        }
        return null;
    }

    @Override
    public String askString(String question, String def) {
        System.out.print(question + " ");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String fn = r.readLine();
            return fn;
        } catch (IOException ex) {
            ctx.error(ex);
        }
        return "";
    }

    @Override
    public void openSketchFileEditor(SketchFile f) {
        editFile(f.getFile().getName());
    }

    @Override
    public void closeSketchFileEditor(SketchFile f) {
    }

}
