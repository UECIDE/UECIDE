/*
 * Copyright (c) 2015, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide;

import java.io.*;
import java.util.*;
import java.lang.*;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.impl.completer.FileNameCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.builtins.Completers.TreeCompleter;

public class InteractiveCLI {
    String[] _argv;
    Sketch _loadedSketch;
    LineReader _reader;
    Completer _completer = null;
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



            TreeMap<String, Programmer> pl = new TreeMap<String, Programmer>();

            for (String k : Base.programmers.keySet()) {
                Programmer p = Base.programmers.get(k);
                if (p.worksWith(_loadedSketch.getBoard())) {
                    pl.put(k, p);
                }
            }
        
            _completer = new TreeCompleter(
                TreeCompleter.node(
                    new StringsCompleter(new String[] { "vi", "edit" }), 
                        TreeCompleter.node(new StringsCompleter(fileNames))
                ),
                TreeCompleter.node(
                    new StringsCompleter((new String[] { "ls", "quit", "compile", "make", "upload", "info", "purge", "rescan", "pkg-update", "help" }))
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
            updateComplete(); 
            _reader = LineReaderBuilder.builder().completer(_completer).build();

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
                            _loadedSketch.setDevice(args[1]);
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
                            Preferences.setBoolean("compiler.verbose_compile", true);
                        } else if (args[1].equals("off")) {
                            Preferences.setBoolean("compiler.verbose_compile", false);
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
        } catch (EndOfFileException ex) {
            System.out.println("Byebye.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Preferences.save();
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
        if (_loadedSketch.getDevice() == null) {
            System.out.println("Port:       No Port Selected!!!");
        } else {
            System.out.println("Port:       " + _loadedSketch.getDevice().getName());
        }
    }

}

