package org.uecide;

import jline.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class InteractiveCLI implements AptPercentageListener {
    String[] _argv;
    Sketch _loadedSketch;
    ConsoleReader _reader;
    Completor _completor = null;
    PluginManager _pman;
    APT _apt;

    public InteractiveCLI() {
        _argv = null;
    }

    public InteractiveCLI(String[] argv) {
        _argv = argv;
    }

    public void updateComplete() {
        if (_loadedSketch != null) {
            if (_completor != null) {
                _reader.removeCompletor(_completor);
            }

            ArrayList<String> flist = new ArrayList<String>();
            for (File f : _loadedSketch.sketchFiles) {
                flist.add(f.getName());
            }
            String[] fileNames = flist.toArray(new String[0]);

            ArrayList<String> brdList = new ArrayList<String>();
            for (Board b : Base.boards.values()) {
                brdList.add(b.getName());
            }
            String[] boardNames = brdList.toArray(new String[0]);

            String family = "none";
            Board b = _loadedSketch.getBoard();
            if (b != null) {
                family = _loadedSketch.getBoard().getFamily();
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



            TreeMap<String, String> pl = _loadedSketch.getProgrammerList();

            _completor = new MultiCompletor(new Completor[] {
                new ArgumentCompletor( 
                    new Completor[] {
                        new SimpleCompletor(new String[] { "vi", "edit"}),
                        new SimpleCompletor(fileNames),
                        new NullCompletor()
                    }
                ),
                new SimpleCompletor(new String[] { "ls", "quit", "compile", "make", "upload", "info", "purge", "rescan", "pkg-update", "help" }),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor("pkg-install"),
                        new SimpleCompletor(uninstalledPackages.toArray(new String[0]))
                    }
                ),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor("pkg-remove"),
                        new SimpleCompletor(installedPackages.toArray(new String[0])),
                        new NullCompletor()
                    }
                ),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor("load"),
                        new FileNameCompletor(),
                        new NullCompletor()
                    }
                ),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor("board"),
                        new SimpleCompletor(boardNames),
                        new NullCompletor()
                    }
                ),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor("core"),
                        new SimpleCompletor(coreNames),
                        new NullCompletor()
                    }
                ),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor("compiler"),
                        new SimpleCompletor(compNames),
                        new NullCompletor()
                    }
                ),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor("programmer"),
                        new SimpleCompletor(pl.keySet().toArray(new String[0])),
                        new NullCompletor()
                    }
                ),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor("port"),
                        new SimpleCompletor(Serial.getPortList().toArray(new String[0])),
                        new NullCompletor()
                    }
                ),
                new ArgumentCompletor(
                    new Completor[] {
                        new SimpleCompletor(new String[] { "verbose" }),
                        new SimpleCompletor(new String[] { "on", "off" }),
                        new NullCompletor()
                    }
                ),
                new NullCompletor()
            });
            _reader.addCompletor(_completor);
        }
    }

    public void run() {
        try { 
            _loadedSketch = null;
            if (_argv != null) {
                if (_argv.length > 0) {
                    _loadedSketch = new Sketch(_argv[0]);
                }
            }

            if (_loadedSketch == null) {
                _loadedSketch = new Sketch((File)null);
            }

            System.out.print("Loaded sketch: ");
            if (_loadedSketch == null) {
                System.out.println("none");
            } else {
                System.out.println(_loadedSketch.getName());
            }

            _pman = new PluginManager();
            _apt = _pman.getApt();

            String line = null;
            _reader = new ConsoleReader();

            updateComplete(); 
            info();

            while ((line = _reader.readLine(genPrompt() + "> ")) != null) {
                line = line.trim();
                if (line.startsWith("!")) {
                    blockingExecute(line.substring(1));
                    continue;
                }
                String[] args = line.split(" ");
                if (args.length == 0) {
                    continue;
                }
                if (args[0].equals("quit")) {
                    return;
                }
                if (args[0].equals("ls")) {
                    listFiles();
                }
                if (args[0].equals("purge")) {
                    if (_loadedSketch != null) {
                        _loadedSketch.purgeCache();
                    }
                }
                if (args[0].equals("update")) {
                    rescanEverything();
                    if (_loadedSketch != null) {
                        _loadedSketch.rescanFileTree();
                    }
                }
                if (args[0].equals("vi")) {
                    if (args.length == 2) {
                        editFile(args[1]);
                    } else {
                        System.out.println("Usage: vi <filename>");
                    }
                }
                if (args[0].equals("pkg-update")) {
                    _apt.update();
                }
                if (args[0].equals("pkg-install")) {
                    if (args.length >= 2) {
                        for (int i = 1; i < args.length; i++) {
                            Package p = _apt.getPackage(args[i]);
                            if (p == null) {
                                System.out.println("Package '" + args[i] + "' not found");
                            } else {
                                p.attachPercentageListener(this);
                                _apt.installPackage(p);
                                System.out.println();
                            }
                        }
                        updateComplete(); 
                        rescanEverything();
                    }
                }
                if (args[0].equals("pkg-remove")) {
                    if (args.length == 2) {
                        Package p = _apt.getPackage(args[1]);
                        if (p == null) {
                            System.out.println("Package not found");
                        } else {
                            p.attachPercentageListener(this);
                            System.out.println(_apt.uninstallPackage(p, false));
                        }
                        updateComplete(); 
                        rescanEverything();
                    }
                }
                if (args[0].equals("compile") || args[0].equals("make")) {
                    if (_loadedSketch != null) {
                        _loadedSketch.rescanFileTree();
                        if (_loadedSketch.prepare()) {
                            _loadedSketch.compile();
                            if (args.length > 1) {
                                if (args[1].equals("install")) {
                                    _loadedSketch.upload();
                                }
                            }
                        }
                    } else {
                        System.out.println("No sketch loaded!");
                    }
                }
                if (args[0].equals("upload")) {
                    if (_loadedSketch != null) {
                        _loadedSketch.rescanFileTree();
                        if (_loadedSketch.prepare()) {
                            _loadedSketch.compile();
                            _loadedSketch.upload();
                        }
                    } else {
                        System.out.println("No sketch loaded!");
                    }
                }
                if (args[0].equals("info")) {
                    if (_loadedSketch != null) {
                        info();
                    }
                }
                if (args[0].equals("board")) {
                    if (args.length == 2) {
                        if (_loadedSketch != null) {
                            _loadedSketch.setBoard(args[1]);
                            updateComplete(); 
                        }
                    } else {
                        System.out.println("Usage: board <name>");
                    }
                }
                if (args[0].equals("core")) {
                    if (args.length == 2) {
                        if (_loadedSketch != null) {
                            _loadedSketch.setCore(args[1]);
                            updateComplete(); 
                        }
                    } else {
                        System.out.println("Usage: core <name>");
                    }
                }
                if (args[0].equals("compiler")) {
                    if (args.length == 2) {
                        if (_loadedSketch != null) {
                            _loadedSketch.setCompiler(args[1]);
                        }
                    } else {
                        System.out.println("Usage: compiler <name>");
                    }
                }
                if (args[0].equals("programmer")) {
                    if (args.length == 2) {
                        if (_loadedSketch != null) {
                            _loadedSketch.setProgrammer(args[1]);
                        }
                    } else {
                        System.out.println("Usage: programmer <name>");
                    }
                }
                if (args[0].equals("port")) {
                    if (args.length == 2) {
                        if (_loadedSketch != null) {
                            _loadedSketch.setSerialPort(args[1]);
                        }
                    } else {
                        System.out.println("Usage: port <name>");
                    }
                }
                if (args[0].equals("list")) {
                    if (args.length == 2) {
                        if (args[1].equals("boards")) {
                        }
                    } else {
                        System.out.println("Usage: list <boards|cores|compilers|programmers|ports>");
                    }
                }

                if (args[0].equals("verbose")) {
                    if (args.length == 2) {
                        if (args[1].equals("on")) {
                            Base.preferences.setBoolean("compiler.verbose", true);
                        } else if (args[1].equals("off")) {
                            Base.preferences.setBoolean("compiler.verbose", false);
                        }
                    }
                }

                if (args[0].equals("load")) {
                    boolean ok = true;
                    if (args.length == 2) {
                        File f = new File(args[1]);
                        if (!f.exists()) {
                            f = new File(Base.getSketchbookFolder(), args[1]);
                            if (!f.exists()) {
                                System.out.println("Sketch not found");
                            } else {
                                if (f.isDirectory()) {
                                    if (!Base.isSketchFolder(f)) {
                                        ok = false;
                                        System.out.println("Not a sketch");
                                    }
                                } else {
                                    if (f.getName().endsWith(".ino") || f.getName().endsWith(".pde")) {
                                        f = f.getParentFile();
                                        if (!Base.isSketchFolder(f)) {
                                            ok = false;
                                            System.out.println("Not a sketch");
                                        }
                                    }
                                }
                            }
                        }
                        if (ok) {
                            System.out.println("Loading " + f.getAbsolutePath());
                            _loadedSketch = new Sketch(f);
                            updateComplete(); 
                            info();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Base.preferences.save();
    }

    String genPrompt() {
        if (_loadedSketch == null) {
            return "none";
        }
        return _loadedSketch.getName();
    }

    void listFiles() {
        if (_loadedSketch == null) {
            System.out.println("No sketch loaded!");
            return;
        }
        for (File f : _loadedSketch.sketchFiles) {
            System.out.println(f.getName());
        }
    }

    void editFile(String fn) {
        if (_loadedSketch == null) {
            System.out.print("No sketch loaded!");
            return;
        }
        fn = fn.trim();
        for (File f : _loadedSketch.sketchFiles) {
            if (f.getName().equals(fn)) {
                try {

                    String [] commandline = new String[3];
                    commandline[0] = "/bin/bash";
                    commandline[1] = "-c";
                    commandline[2] = "vi </dev/tty >/dev/tty '" + f.getAbsolutePath() + "'";
                    Process p = Runtime.getRuntime().exec(commandline);

                    p.waitFor();
                    _loadedSketch.rescanFileTree();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        System.out.println("File '" + fn + "' not found!");
    }

    void blockingExecute(String fn) {
        fn = fn.trim();
        try {
            String [] commandline = new String[3];
            commandline[0] = "/bin/bash";
            commandline[1] = "-c";
            commandline[2] = fn + " </dev/tty >/dev/tty";
            Process p = Runtime.getRuntime().exec(commandline);

            p.waitFor();
            _loadedSketch.rescanFileTree();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatePercentage(Package p, int pct) {
        if (pct > 100) pct = 100;
        System.out.print("\r" + p.getName() + " ... " + pct + "%" + Character.toString((char)27) + "[0K");
    }

    public void rescanEverything() {
        Editor.bulletAll("Updating serial ports...");
        Serial.updatePortList();
        Serial.fillExtraPorts();

        Base.rescanCompilers();
        Base.rescanCores();
        Base.rescanBoards();
        Base.rescanPlugins();
        Base.rescanLibraries();
        Editor.bulletAll("Update complete.");
        Editor.updateAllEditors();
        Editor.selectAllEditorBoards();

    }

    public void info() {
        if (_loadedSketch.getBoard() == null) {
            System.out.println("Board:      No Board Selected!!!");
        } else {
            System.out.println("Board:      " + _loadedSketch.getBoard().getName() + " (" + _loadedSketch.getBoard() + ")");
        }
        if (_loadedSketch.getCore() == null) {
            System.out.println("Core:       No Core Selected!!!");
        } else {
            System.out.println("Core:       " + _loadedSketch.getCore().getName() + " (" + _loadedSketch.getCore() + ")");
        }
        if (_loadedSketch.getCompiler() == null) {
            System.out.println("Compiler:   No Compiler Selected!!!");
        } else {
            System.out.println("Compiler:   " + _loadedSketch.getCompiler().getName() + " (" + _loadedSketch.getCompiler() + ")");
        }
        if (_loadedSketch.getProgrammer() == null) {
            System.out.println("Programmer: No Programmer Selected!!!");
        } else {
            System.out.println("Programmer: " + _loadedSketch.getProgrammer());
        }
        if (_loadedSketch.getSerialPort() == null) {
            System.out.println("Port:       No Port Selected!!!");
        } else {
            System.out.println("Port:       " + _loadedSketch.getSerialPort());
        }
    }

}

