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

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import java.text.*;
import org.uecide.plugin.*;

import javax.script.*;

import org.uecide.builtin.BuiltinCommand;
import org.uecide.varcmd.VariableCommand;

import java.lang.reflect.*;

import java.util.regex.*;


import java.security.*;

import javax.swing.*;
import javax.imageio.*;

import org.uecide.Compiler;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

import org.reflections.*;
import org.reflections.util.*;
import org.reflections.scanners.*;

import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;

/*! The Base class provides the initial application
 *  startup code, parsing command line options, loading
 *  preferences, themes, etc, then scanning all the boards,
 *  cores, compilers etc.  It also provides a central storage
 *  location for application data, and a selection of useful
 *  helper functions.
 */
public class Base {

    public static HashMap<String, PropertyFile> iconSets = new HashMap<String, PropertyFile>();

    public static String RELEASE = "release";

    public static String overrideSettingsFolder = null;

    public static ArrayList<Process> processes = new ArrayList<Process>();

    public static HashMap<String, JSPlugin> jsplugins;

    static Platform platform;

    public static PropertyFile manualPages;

    static private boolean headless;

    // these are static because they're used by Sketch
    static private File examplesFolder;
    static private File librariesFolder;
    static private File toolsFolder;
    static private File hardwareFolder;
    public static ArrayList<File> MRUList;
    public static HashMap<File,Integer> MCUList;

    static HashSet<File> libraries;

    // maps imported packages to their library folder
    public static TreeMap<String, File> importToLibraryTable;

    // classpath for all known libraries for p5
    // (both those in the p5/libs folder and those with lib subfolders
    // found in the sketchbook)
    public static String librariesClassPath;

    public static Version systemVersion;

    public static TreeMap<String, Compiler> compilers;
    public static TreeMap<String, Board> boards;
    public static TreeMap<String, Core> cores;
    public static TreeMap<String, Programmer> programmers;
    public static TreeMap<String, Tool> tools;
//    public static TreeMap<String, Plugin> plugins;
    public static TreeMap<String, Class<?>> plugins = new TreeMap<String, Class<?>>();
    public static ArrayList<Plugin> pluginInstances;
    static Splash splashScreen;

    public static CommandLine cli = new CommandLine();

    // Location for untitled items
    static File untitledFolder;

    public static PropertyFile preferences;
    public static PropertyFile session = new PropertyFile();

    public static PropertyFile preferencesTree = new PropertyFile();

    public static PropertyFile webLinks;

//    public static HashMap<Object, DiscoveredBoard> discoveredBoards = new HashMap<Object, DiscoveredBoard>();

    public static boolean onlineMode = true;

    public static TreeSet<CommunicationPort> communicationPorts = new TreeSet<CommunicationPort>();

    public static TreeMap<String, Class<?>> lookAndFeels = new TreeMap<String, Class<?>>();

    public static I18N i18n = new I18N("Core");

    static Thread compilerLoaderThread = null;
    static Thread coreLoaderThread = null;
    static Thread boardLoaderThread = null;
    static Thread programmerLoaderThread = null;
    static Thread toolLoaderThread = null;
    static Thread libraryLoaderThread = null;
    static Thread cleanupThread = null;


    /*! Get a Board from the internal boards list by its short name. */
    public static Board getBoard(String name) {
        return boards.get(name);
    }

    /*! Get a Core from the internal cores list by its short name. */
    public static Core getCore(String name) {
        return cores.get(name);
    }

    /*! Get a Compiler from the internal compilers list by its short name. */
    public static Compiler getCompiler(String name) {
        return compilers.get(name);
    }

    /*! The main execution entry function. It just creates an instance of this
     *  object and passes the command line arguments.
     */
    public static void main(String args[]) {
        try {
            new Base(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*! Return a File representing the location of the JAR file the
     *  application was loaded from. */
    public static File getJarLocation() {
        return getJarLocation(Base.class);
    }

    public static File getJarLocation(Class<?> cl) {
        try {
            ProtectionDomain pd = cl.getProtectionDomain();
            if (pd == null) {
                return null;
            }

            CodeSource cs = pd.getCodeSource();
            if (cs == null) {
                return null;
            }

            URL sl = cs.getLocation();
            if (sl == null) {
                return null;
            }
            
            URI ui = sl.toURI();
            return new File(ui);
        } catch(Exception e) {
            Base.error(e);
        }

        return new File("/");
    }


    public static HashMap<String, String> settings = new HashMap<String, String>();


    public static boolean autoCompile = false;
    public static boolean autoProgram = false;
    public static String presetPort = null;
    public static String presetBoard = null;
    public static String presetCompiler = null;
    public static String presetCore = null;
    public static String presetProgrammer = null;

    public static boolean cleanBuild = false;
    public static boolean purgeCache = false;

    public static boolean extraDebug = false;

    static public void cacheSystemFilesFromList(File[] list) {
        for (File f : list) {
            Debug.message("Caching " + f);
            FileCache.add(f);
        }
    }

    static public void cacheSystemFiles() {
        cacheSystemFilesFromList(getCoresFolders());
        cacheSystemFilesFromList(getToolsFolders());
        cacheSystemFilesFromList(getProgrammersFolders());
        cacheSystemFilesFromList(getBoardsFolders());
        cacheSystemFilesFromList(getCompilersFolders());
        cacheSystemFilesFromList(getIconsFolders());
// I'd love to cache the library tree and parse it, but that would make
// categories suck big time.
//        cacheSystemFilesFromList(getLibrariesFolders()); 
    }

    /*! The constructor is the main execution routine. */
    public Base(String[] args) throws IOException {
        cli.addParameter("debug",               "",         Boolean.class,  "cli.help.debug");
        cli.addParameter("verbose",             "",         Boolean.class,  "cli.help.verbose");
        cli.addParameter("exceptions",          "",         Boolean.class,  "cli.help.exceptions");
        cli.addParameter("headless",            "",         Boolean.class,  "cli.help.headless");
        cli.addParameter("datadir",             "location", String.class,   "cli.help.datadir");
        cli.addParameter("last-sketch",         "",         Boolean.class,  "cli.help.last-sketch");
        cli.addParameter("clean",               "",         Boolean.class,  "cli.help.clean");
        cli.addParameter("compile",             "",         Boolean.class,  "cli.help.compile");
        cli.addParameter("upload",              "",         Boolean.class,  "cli.help.upload");
        cli.addParameter("board",               "name",     String.class,   "cli.help.board");
        cli.addParameter("core",                "name",     String.class,   "cli.help.core");
        cli.addParameter("compiler",            "name",     String.class,   "cli.help.compiler");
        cli.addParameter("port",                "name",     String.class,   "cli.help.port");
        cli.addParameter("programmer",          "name",     String.class,   "cli.help.programmer");
        cli.addParameter("purge",               "",         Boolean.class,  "cli.help.purge");
        cli.addParameter("help",                "",         Boolean.class,  "cli.help.help");

        cli.addParameter("update",              "",         Boolean.class,  "cli.help.update");
        cli.addParameter("install",             "package",  String.class,   "cli.help.install");
        cli.addParameter("remove",              "package",  String.class,   "cli.help.remove");
        cli.addParameter("remove-all",          "",         Boolean.class,  "cli.help.remove-all");
        cli.addParameter("upgrade",             "",         Boolean.class,  "cli.help.upgrade");
        cli.addParameter("search",              "term",     String.class,   "cli.help.search");
        cli.addParameter("list",                "",         Boolean.class,  "cli.help.list");
        cli.addParameter("section",             "name",     String.class,   "cli.help.section");
        cli.addParameter("group",               "name",     String.class,   "cli.help.group");
        cli.addParameter("subgroup",            "name",     String.class,   "cli.help.subgroup");
        cli.addParameter("family",              "name",     String.class,   "cli.help.family");
        cli.addParameter("force",               "",         Boolean.class,  "cli.help.force");

        cli.addParameter("mkmf",                "",         Boolean.class,  "cli.help.mkmf");
        cli.addParameter("force-local-build",   "",         Boolean.class,  "cli.help.force-local-build");
        cli.addParameter("force-save-hex",      "",         Boolean.class,  "cli.help.force-save-hex");
        cli.addParameter("force-join-files",    "",         Boolean.class,  "cli.help.force-join-files");
        cli.addParameter("online",              "",         Boolean.class,  "cli.help.online");
        cli.addParameter("offline",             "",         Boolean.class,  "cli.help.offline");

        cli.addParameter("version",             "",         Boolean.class,  "cli.help.version");

        cli.addParameter("cli",                 "",         Boolean.class,  "cli.help.cli");

        cli.addParameter("preferences",         "",         Boolean.class,  "cli.help.preferences");
        cli.addParameter("quiet",               "",         Boolean.class,  "cli.help.quiet");

        cli.addParameter("reset-preferences",   "",         Boolean.class,  "cli.help.reset.prefs");

        cli.addParameter("locale",              "name",     String.class,   "cli.help.locale");

        String[] argv = cli.process(args);

        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                Pattern pat = Pattern.compile(":\\/\\/([a-zA-Z0-9]+):([a-zA-Z0-9]+)@");
                Matcher m = pat.matcher(getRequestingURL().toString());
                if (m.find()) {
                    return new PasswordAuthentication(m.group(1), m.group(2).toCharArray());
                }

                return null;
            }
        });

        i18n = new I18N("Core");

        if (cli.isSet("help")) {
            cli.help();
            System.exit(0);
        }

        headless = cli.isSet("headless");
        boolean loadLastSketch = cli.isSet("last-sketch");

        boolean doExit = false;

        if (cli.isSet("mkmf")) {
            headless = true;
        }

        Debug.setVerbose(cli.isSet("verbose"));
        if (cli.isSet("debug")) {
            Debug.show();
        }

        overrideSettingsFolder = cli.getString("datadir");
        autoCompile = cli.isSet("compile");
        autoProgram = cli.isSet("upload");
        presetPort = cli.getString("port");
        presetBoard = cli.getString("board");
        presetCore = cli.getString("core");
        presetCompiler = cli.getString("compiler");
        presetProgrammer = cli.getString("programmer");
        purgeCache = cli.isSet("purge");
        cleanBuild = cli.isSet("clean");


        if(!cli.isSet("exceptions")) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    Base.broken(t, e);
                }
            });
        }



        if(isUnix()) {
            if((System.getenv("DISPLAY") == null) || (System.getenv("DISPLAY").equals(""))) {
                headless = true;
            }
        }

        PropertyFile versionInfo = new PropertyFile("/org/uecide/version.txt");

        RELEASE = versionInfo.get("Release");
        systemVersion = new Version(versionInfo.get("Version"));

        Debug.message("Version: " + systemVersion);

        if (cli.isSet("version")) {
            System.out.println(i18n.string("msg.version", systemVersion));
            System.exit(0);
        }

        initPlatform();

        if (cli.isSet("reset-preferences")) {
            File prefsFile = getDataFile("preferences.txt");
            try {
                prefsFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(10);
            }
            System.out.println(">>> All preferences reset to default! <<<");
        }

        preferences = new PropertyFile(getDataFile("preferences.txt"), "/org/uecide/config/preferences.txt");
        preferences.setPlatformAutoOverride(true);

        if (preferences.getBoolean("editor.hwaccel")) {
            Properties props = System.getProperties();
            props.setProperty("sun.java2d.opengl", "true");
        } else {
            Properties props = System.getProperties();
            props.setProperty("sun.java2d.opengl", "false");
        }

        platform.setSettingsFolderEnvironmentVariable();

        if (preferences.getBoolean("network.offline")) {
            setOfflineMode();
        }

        if (cli.isSet("online")) {
            setOnlineMode();
        }

        if (cli.isSet("offline")) {
            setOfflineMode();
        }
        
        if (cli.isSet("update")) {
            try {
                PluginManager pm = new PluginManager();
                APT apt = pm.getApt();
                apt.update();
            } catch (Exception ex) { error(ex); }
            doExit = true;
        }

        if (cli.isSet("upgrade")) {
            try {
                PluginManager pm = new PluginManager();
                APT apt = pm.getApt();
                Package[] pl = apt.getUpgradeList();
                for (Package p : pl) {
                    apt.upgradePackage(p);
                }
            } catch (Exception e) {
                error(e);
            }
            doExit = true;
        }

        if (cli.isSet("install")) {
            try {
                PluginManager pm = new PluginManager();
                APT apt = pm.getApt();
                String packageName = cli.getString("install");
                if (packageName == null) {
                    System.err.println(i18n.string("err.selpkginst"));
                } else {
                    Package p = apt.getPackage(packageName);
                    if (p == null) {
                        System.err.println(i18n.string("err.pkgnotfound", packageName));
                        System.err.println(i18n.string("msg.usesearch"));
                    } else {
                        System.out.println(i18n.string("msg.installing", p.getName()));
                        apt.installPackage(p);
                        System.out.println(i18n.string("msg.done"));
                    }
                }
            } catch (Exception ex) { error(ex); }
            doExit = true;
        }

        if (cli.isSet("remove-all")) {
            try {
                if (!cli.isSet("force")) {
                    System.err.println(i18n.string("err.notremove"));
                } else {
                    PluginManager pm = new PluginManager();
                    APT apt = pm.getApt();
                    for (Package p : apt.getInstalledPackages()) {
                        apt.uninstallPackage(p, true);
                    }
                }
            } catch (Exception ex) { error(ex); }
            doExit = true;
        }
            
        if (cli.isSet("remove")) {
            try {
                PluginManager pm = new PluginManager();
                APT apt = pm.getApt();
                String packageName = cli.getString("remove");
                if (packageName == null) {
                    System.err.println(i18n.string("err.selpkguninst"));
                    doExit = true;
                }

                Package p = apt.getPackage(packageName);
                if (p == null) {
                    System.err.println(i18n.string("err.notfound", packageName));
                    System.err.println(i18n.string("msg.usesearch"));
                } else {
                    System.out.println(i18n.string("msg.uninstalling", p.getName()));
                    apt.uninstallPackage(p, cli.isSet("force"));
                }
            } catch (Exception ex) { error(ex); }
            doExit = true;
        }

        if (cli.isSet("list")) {
            try {
                PluginManager pm = new PluginManager();
                APT apt = pm.getApt();
                Package[] pkgs = apt.getPackages();
                String format = "%-50s %10s %10s %s";
                System.out.println(String.format(format, i18n.string("apt.list.package"), i18n.string("apt.list.installed"), i18n.string("apt.list.available"), ""));
                ArrayList<Package> out = new ArrayList<Package>();
                for (Package p : pkgs) {
                    if (cli.isSet("section")) {
                        if (p.get("Section") == null) { continue; }
                        if (!p.get("Section").equals(cli.getString("section"))) { continue; }
                    }
                    if (cli.isSet("family")) {
                        if (p.get("Family") == null) { continue; }
                        if (!p.get("Family").equals(cli.getString("family"))) { continue; }
                    }
                    if (cli.isSet("group")) {
                        if (p.get("Group") == null) { continue; }
                        if (!p.get("Group").equals(cli.getString("group"))) { continue; }
                    }
                    if (cli.isSet("subgroup")) {
                        if (p.get("Subgroup") == null) { continue; }
                        if (!p.get("Subgroup").equals(cli.getString("subgroup"))) { continue; }
                    }

                    String name = p.getName();
                    Package instPack = apt.getInstalledPackage(p.getName());
                    Version avail = p.getVersion();
                    Version inst = null;
                    String msg = "";
                    if (instPack != null) {
                        inst = instPack.getVersion();
                        if (avail.compareTo(inst) > 0) {
                            msg = i18n.string("apt.list.update");
                        }
                    }
                    System.out.println(String.format(format, name, inst == null ? "" : inst.toString(), avail.toString(), msg));
                    System.out.println("  " + p.getDescriptionLineOne());

                }
            } catch (Exception ex) { error(ex); }
            doExit = true;
        }
                
        if (cli.isSet("search")) {
            try {
                PluginManager pm = new PluginManager();
                APT apt = pm.getApt();
                Package[] pkgs = apt.getPackages();
                String format = "%-50s %10s %10s %s";
                System.out.println(String.format(format, i18n.string("apt.list.package"), i18n.string("apt.list.installed"), i18n.string("apt.list.available"), ""));
                ArrayList<Package> out = new ArrayList<Package>();
                String term = cli.getString("search").toLowerCase();
                for (Package p : pkgs) {
                    if (cli.isSet("section")) {
                        if (p.get("Section") == null) { continue; }
                        if (!p.get("Section").equals(cli.getString("section"))) { continue; }
                    }
                    if (cli.isSet("family")) {
                        if (p.get("Family") == null) { continue; }
                        if (!p.get("Family").equals(cli.getString("family"))) { continue; }
                    }
                    if (cli.isSet("group")) {
                        if (p.get("Group") == null) { continue; }
                        if (!p.get("Group").equals(cli.getString("group"))) { continue; }
                    }
                    if (cli.isSet("subgroup")) {
                        if (p.get("Subgroup") == null) { continue; }
                        if (!p.get("Subgroup").equals(cli.getString("subgroup"))) { continue; }
                    }

                    String name = p.getName();
                    Package instPack = apt.getInstalledPackage(p.getName());
                    Version avail = p.getVersion();
                    Version inst = null;
                    String msg = "";
                    if (instPack != null) {
                        inst = instPack.getVersion();
                        if (avail.compareTo(inst) > 0) {
                            msg = i18n.string("apt.list.update");
                        }
                    }
                    String comp = p.getName() + " " + p.getDescription();
                    if (comp.toLowerCase().contains(term)) {
                        System.out.println(String.format(format, name, inst == null ? "" : inst.toString(), avail.toString(), msg));
                        System.out.println("  " + p.getDescriptionLineOne());
                    }

                }
            } catch (Exception ex) { error(ex); }
            doExit = true;
        }

        if (doExit) {
            System.exit(0);
        }


        if (cli.isSet("cli")) {
            headless = true;
            platform.init(this);
            compilers = new TreeMap<String, Compiler>();
            cores = new TreeMap<String, Core>();
            tools = new TreeMap<String, Tool>();
            boards = new TreeMap<String, Board>();
            programmers = new TreeMap<String, Programmer>();
            plugins = new TreeMap<String, Class<?>>();
            lookAndFeels = new TreeMap<String, Class<?>>();
            pluginInstances = new ArrayList<Plugin>();
            
            Serial.updatePortList();

            System.out.print(i18n.string("msg.loading.assets"));
            loadAssets();
            System.out.println(i18n.string("msg.loading.done"));

            buildPreferencesTree();

            runInitScripts();

            InteractiveCLI icli = new InteractiveCLI(argv);
            icli.run();
            System.exit(0);
        }

        Debug.setLocation(new Point(preferences.getInteger("debug.window.x"), preferences.getInteger("debug.window.y")));
        Debug.setSize(new Dimension(preferences.getInteger("debug.window.width"), preferences.getInteger("debug.window.height")));

        if(!headless) {
            splashScreen = new Splash();
            splashScreen.setMessage("Loading UECIDE...", 10);
        }

        if (!headless) splashScreen.setMessage(i18n.string("splash.msg.packagemanager"), 15);
        initPackageManager();

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        // Create a location for untitled sketches
        untitledFolder = createTempFolder("untitled");
        untitledFolder.deleteOnExit();

        Debug.message("Application section start");
        if(!headless) splashScreen.setMessage(i18n.string("splash.msg.application"), 20);

        Debug.message("Platform init");
        platform.init(this);

        // Get paths for the libraries and examples in the Processing folder
        //String workingDirectory = System.getProperty("user.dir");

        Debug.message("Get system folders");
        examplesFolder = getContentFile("examples");
        toolsFolder = getContentFile("tools");

        // Get the sketchbook path, and make sure it's set properly
        String sketchbookPath = preferences.get("locations.sketchbook");

        // If no path is set, get the default sketchbook folder for this platform
        if(sketchbookPath == null) {
            File defaultFolder = getDefaultSketchbookFolder();
            preferences.set("locations.sketchbook", defaultFolder.getAbsolutePath());
            sketchbookPath = defaultFolder.getAbsolutePath();
        }

        File sketchbookFolder = new File(sketchbookPath);

        if(!sketchbookFolder.exists()) {
            if (!sketchbookFolder.mkdirs()) { error("Unable to make sketchbook folder " + sketchbookFolder.getAbsolutePath()); }
        }

        Debug.message("Create trees");

        webLinks = new PropertyFile("/org/uecide/config/links.txt");

        compilers = new TreeMap<String, Compiler>();
        cores = new TreeMap<String, Core>();
        tools = new TreeMap<String, Tool>();
        boards = new TreeMap<String, Board>();
        programmers = new TreeMap<String, Programmer>();
        plugins = new TreeMap<String, Class<?>>();
        lookAndFeels = new TreeMap<String, Class<?>>();
        pluginInstances = new ArrayList<Plugin>();

        Debug.message("Scan ports");
        Thread t = new Thread() {
            public void run() {
                Serial.updatePortList();
                Serial.fillExtraPorts();
            }
        };
        t.start();

        Debug.message("Application section done");

        if(!headless) splashScreen.setMessage(i18n.string("splash.msg.assets"), 40);


        loadAssets();

        buildPreferencesTree();

        if (cli.isSet("mkmf")) {
            for(int i = 0; i < argv.length; i++) {
                String path = argv[i];
                if (path.equals(".")) {
                    path = System.getProperty("user.dir");
                }
                Sketch s = null;
                try {
                    s = new Sketch(path);
                } catch (IOException ex) {
                    error(ex);
                    return;
                }
                s.loadConfig();
                if(presetPort != null) {
                    s.setDevice(presetPort);
                }

                if(presetBoard != null) {
                    s.setBoard(presetBoard);
                }

                if(presetCore != null) {
                    s.setCore(presetCore);
                }

                if(presetCompiler != null) {
                    s.setCompiler(presetCompiler);
                }

                if(presetProgrammer != null) {
                    s.setProgrammer(presetProgrammer);
                }

                if (purgeCache) {
                    s.purgeCache();
                }

                s.generateMakefile();
            }
            System.exit(0);
        }


        runInitScripts();

        initMRU();


        if(!headless) splashScreen.setMessage(i18n.string("splash.msg.editor"), 80);

        if (!headless) setLookAndFeel();

        if (cli.isSet("preferences")) {
            if(!headless) splashScreen.setMessage(i18n.string("splash.msg.complete"), 100);
            splashScreen.dispose();
            new Preferences(null);
            preferences.save();
            System.exit(0);
        }

        boolean opened = false;

        // Check if any files were passed in on the command line
        for(int i = 0; i < argv.length; i++) {
            String path = argv[i];

            // Fix a problem with systems that use a non-ASCII languages. Paths are
            // being passed in with 8.3 syntax, which makes the sketch loader code
            // unhappy, since the sketch folder naming doesn't match up correctly.
            // http://dev.processing.org/bugs/show_bug.cgi?id=1089
            if(isWindows()) {
                try {
                    File file = new File(argv[i]);
                    path = file.getCanonicalPath();
                } catch(IOException e) {
                    error(e);
                }
            }

            if (path.equals(".")) {
                path = System.getProperty("user.dir");
            }

            File p = new File(path);
            Debug.message("Loading sketch " + p);
            if (p.exists()) {
                try {
                    opened = doOpenThings(p);
                } catch (IOException ex) {
                    error(ex);
                    return;
                }
            }
        }

        if(loadLastSketch || Preferences.getBoolean("editor.save.loadlast")) {
            File lastFile = MRUList.get(0);

            if(lastFile != null) {
                if (!headless && !opened)
                try {
                    opened = doOpenThings(lastFile);
                } catch (IOException ex) {
                    error(ex);
                    return;
                }
            }
        }

        // Create a new empty window (will be replaced with any files to be opened)
        if(!opened && !headless) {
            try {
                handleNew();
            } catch (IOException ex) {
                error(ex);
                return;
            }
        }

        if(!headless) {
            if(!headless) splashScreen.setMessage(i18n.string("splash.msg.complete"), 100);
            splashScreen.dispose();

            if (preferences.get("changelog.hide") != null) {
                Version vhidden = new Version(preferences.get("changelog.hide"));
                if (systemVersion.compareTo(vhidden) > 0) {
                    Changelog c = new Changelog(Editor.editorList.get(0));
                }
            } else {
                Changelog c = new Changelog(Editor.editorList.get(0));
            }

            try {

                synchronized (Editor.editorList) {
                    if(boards.size() == 0) {

                        showWarning(
                            i18n.string("err.noboards.title"),
                            i18n.string("err.noboards.body"),
                            null
                        );
                        PluginManager pm = new PluginManager();
                        pm.openWindow(Editor.editorList.get(0), true);
                    } else if(cores.size() == 0) {
                        showWarning(
                            i18n.string("err.nocores.title"),
                            i18n.string("err.nocores.body"),
                            null
                        );
                        PluginManager pm = new PluginManager();
                        pm.openWindow(Editor.editorList.get(0), true);
                    } else if(compilers.size() == 0) {
                        showWarning(
                            i18n.string("err.nocompilers.title"),
                            i18n.string("err.nocompilers.body"),
                            null
                        );
                        PluginManager pm = new PluginManager();
                        pm.openWindow(Editor.editorList.get(0), true);
                    }
                }
            } catch (Exception ex) { error(ex); }
        }

        if(isTimeToCheckVersion()) {
            if(isNewVersionAvailable()) {
                if(headless) {
                    System.err.println(i18n.string("msg.version.available"));
                    System.err.println(i18n.string("msg.version.download", "https://uecide.org/download"));
                } else {
                    String[] options = {"Yes", "No"};
                    int n = JOptionPane.showOptionDialog(
                        null, 
                        i18n.string("msg.version.available.body"),
                        i18n.string("msg.version.available"),
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.QUESTION_MESSAGE, 
                        null, 
                        options, 
                        options[0]
                    );

                    if(n == 0) {
                        Utils.browse("https://uecide.org/download");
                    }
                }
            }
        }

        session.set("os.version", getOSVersion());

        if(headless) {
            System.exit(0);
        }

        ServiceManager.addService(new UsbDiscoveryService());
        ServiceManager.addService(new BackgroundLibraryCompileService());
        ServiceManager.addService(new ChangedFileService());
        ServiceManager.addService(new NetworkDiscoveryService());
        ServiceManager.addService(new TreeUpdaterService());        
    }

    public static void addJarFile(File f) {
        try {
            File find = f;
            if (!find.exists()) {
                File[] pfs = getPluginsFolders();
                for (File pf : pfs) {
                    find = new File(pf, f.getPath());
                    if (find.exists()) {
                        break;
                    }
                }
            }
            if (!find.exists()) {
                error("File not found: " + f);
                return;
            }
            Base.addURL(f.toURI().toURL());
        } catch (Exception e) {
            error(e);
        }
    }
    
    /*! Attempt to add a jar file as a URL to the system class loader */
    @SuppressWarnings("unchecked")
    public static void addURL(URL u) {
        final Class[] parameters = new Class[]{URL.class}; 
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{u});
        } catch (Exception ex) {
            ex.printStackTrace();
            Base.error(ex);
        }
    }

    /*! Open a sketch in a new Editor (if not running headless) and set up any preset values
     *  specified on the command line.
     */
    static boolean doOpenThings(File sketch) throws IOException {
        Sketch s = null;
        Editor e = null;

        if(!headless) {
            e = createNewEditor(sketch.getAbsolutePath());

            if(e == null) {
                return false;
            }


            s = e.getSketch();
        } else {
            s = new Sketch(sketch);
            s.loadConfig();
        }

        if(presetPort != null) {
            s.setDevice(presetPort);
        }

        if(presetBoard != null) {
            s.setBoard(presetBoard);
        }

        if(presetCore != null) {
            s.setCore(presetCore);
        }

        if(presetCompiler != null) {
            s.setCompiler(presetCompiler);
        }

        if(presetProgrammer != null) {
            s.setProgrammer(presetProgrammer);
        }

        if (purgeCache) {
            s.purgeCache();
        }

        if (cleanBuild) {
            s.purgeBuildFiles();
        }

        if(e == null) {
            if(autoProgram) {
                if(!s.build()) {
                    System.exit(10);
                }
                if (!s.upload()) {
                    System.exit(10);
                }
            } else if(autoCompile) {
                if (!s.build()) {
                    System.exit(10);
                }
            }
        } else {
            if(autoProgram) {
                e.program();
            } else if(autoCompile) {
                e.compile();
            }
        }

        return true;
    }

    /*! Initialize any platform specific settings */
    static protected void initPlatform() {
        try {
//            Class<?> platformClass = Class.forName("org.uecide.Platform");

            if(Base.isMacOS()) {
                platform = new org.uecide.macosx.Platform();
//                platformClass = Class.forName("org.uecide.macosx.Platform");
            } else if(Base.isWindows()) {
                platform = new org.uecide.windows.Platform();
//                platformClass = Class.forName("org.uecide.windows.Platform");
            } else if(Base.isUnix()) {
                platform = new org.uecide.unix.Platform();
//                platformClass = Class.forName("org.uecide.unix.Platform");
            }

//            platform = (Platform) platformClass.newInstance();
        } catch(Exception e) {
            Base.showError("Problem Setting the Platform",
                           "An unknown error occurred while trying to load\n" +
                           "platform-specific code for your machine.", e);
        }
    }

    /* Initialize the internal MRU list from the preferences set */
    public static void initMRU() {
        MRUList = new ArrayList<File>();
        MCUList = new HashMap<File,Integer>();

        for(int i = 0; i < 10; i++) {
            if(preferences.get("sketch.mru." + i) != null) {
                File f = new File(preferences.get("sketch.mru." + i));

                if(f.exists()) {
                    if(MRUList.indexOf(f) == -1) {
                        MRUList.add(f);
                    }
                }
            }
        }

        for(int i = 0; i < 10; i++) {
            if(preferences.get("sketch.mcu." + i) != null) {
                String[] mcuEntry = preferences.getArray("sketch.mcu." + i);

                int hits = 0;
                try {
                    hits = Integer.parseInt(mcuEntry[1]);
                } catch (Exception e) {
                }

                File f = new File(mcuEntry[0]);

                if(f.exists()) {
                    MCUList.put(f, hits);
                }
            }
        }
    }

    /*! Update the internal MRU list with a new File */
    public static void updateMRU(File f) {
        if (f == null) {
            return;
        }

        if(!f.isDirectory()) {
            f = f.getParentFile();
        }

        if ((MRUList == null) || (MCUList == null)) {
            initMRU();
        }

        MRUList.remove(f);
        MRUList.add(0, f);

        while(MRUList.size() > 10) {
            MRUList.remove(10);
        }

        int hits = 0;
        try {
            hits = MCUList.get(f);
        } catch (Exception e) {
        }
        hits++;
        MCUList.put(f, hits);

        for(int i = 0; i < 10; i++) {
            if(i < MRUList.size()) {
                preferences.set("sketch.mru." + i, MRUList.get(i).getAbsolutePath());
            } else {
                preferences.unset("sketch.mru." + i);
            }
        }

        while (MCUList.size() > 10) {
            int minHits = Integer.MAX_VALUE;

            for (Integer i : MCUList.values()) {
                if (i < minHits) {
                    minHits = i;
                }
            }

            for (Map.Entry<File,Integer> i : MCUList.entrySet()) {
                if (i.getValue().equals(minHits)) {
                    MCUList.remove(i.getKey());
                    break;
                }
            }
        }

        int z = 0;
        for (Map.Entry<File,Integer> i : MCUList.entrySet()) {
            preferences.set("sketch.mcu." + z, i.getKey().getAbsolutePath() + "::" + i.getValue());
            z++;
        }


        preferences.saveDelay();
    }

    /*! Get the default board to use if no current board can be found */
    public static Board getDefaultBoard() {
        Board tb;
        String prefsBoard = preferences.get("board");
        String[] entries;

        if(boards.size() == 0) {
            return null;
        }

        tb = boards.get(prefsBoard);

        if(tb != null) {
            return tb;
        }

        entries = (String[]) boards.keySet().toArray(new String[0]);
        tb = boards.get(entries[0]);

        if(tb != null) {
            return tb;
        }

        return null;
    }

    public static void loadCompilers() {
        compilers.clear();
//        compilerLoaderThread = new Thread() {
//            public void run() {
                ArrayList<File> compilerFiles = FileCache.getFilesByName("compiler.txt");
                for (File cfile : compilerFiles) {
                    if(cfile.exists()) {
                        Debug.message("    Loading compiler " + cfile.getAbsolutePath());
                        Compiler newCompiler = new Compiler(cfile.getParentFile());

                        if(newCompiler.isValid()) {
                            compilers.put(newCompiler.getName(), newCompiler);
                        } else {
                            Debug.message("    ==> IS NOT VALID!!!");
                        }
                    }
                }
//            }
//        };
//        compilerLoaderThread.start();
    }

    public static void loadCores() {
        cores.clear();
//        coreLoaderThread = new Thread() {
//            public void run() {

                ArrayList<File> coreFiles = FileCache.getFilesByName("core.txt");
                for (File cfile : coreFiles) {
                    if(cfile.exists()) {
                        Debug.message("    Loading core " + cfile.getAbsolutePath());
                        Core newCore = new Core(cfile.getParentFile());

                        if(newCore.isValid()) {
                            cores.put(newCore.getName(), newCore);
                        } else {
                            Debug.message("    ==> IS NOT VALID!!!");
                        }
                    }
                }
//            }
//        };
//        coreLoaderThread.start();
    }

    public static void loadBoards() {
        boards.clear();
//        boardLoaderThread = new Thread() {
//            public void run() {

                ArrayList<File> boardFiles = FileCache.getFilesByName("board.txt");
                for (File bfile : boardFiles) {
                    if(bfile.exists()) {
                        Debug.message("    Loading board " + bfile.getAbsolutePath());
                        Board newBoard = new Board(bfile.getParentFile());

                        if(newBoard.isValid()) {
                            boards.put(newBoard.getName(), newBoard);
                        } else {
                            Debug.message("    ==> IS NOT VALID!!!");
                        }
                    }
                }
//            }
//        };
//        boardLoaderThread.start();
    }

    public static void loadProgrammers() {
        programmers.clear();
//        programmerLoaderThread = new Thread() {
//            public void run() {

                ArrayList<File> programmerFiles = FileCache.getFilesByName("programmer.txt");
                for (File pfile : programmerFiles) {
                    if(pfile.exists()) {
                        Debug.message("    Loading programmer " + pfile.getAbsolutePath());
                        Programmer newProgrammer = new Programmer(pfile.getParentFile());

                        if(newProgrammer.isValid()) {
                            programmers.put(newProgrammer.getName(), newProgrammer);
                        } else {
                            Debug.message("    ==> IS NOT VALID!!!");
                        }
                    }
                }
//            }
//        };
//        programmerLoaderThread.start();
    }

    public static void loadTools() {
        tools.clear();
//        toolLoaderThread = new Thread() {
//            public void run() {

                ArrayList<File> toolFiles = FileCache.getFilesByName("tool.txt");
                for (File tfile : toolFiles) {
                    if(tfile.exists()) {
                        Debug.message("    Loading tool " + tfile.getAbsolutePath());
                        Tool newTool = new Tool(tfile.getParentFile());

                        if(newTool.isValid()) {
                            tools.put(newTool.getName(), newTool);
                        } else {
                            Debug.message("    ==> IS NOT VALID!!!");
                        }
                    }
                }
//            }
//        };
//        toolLoaderThread.start();
    }


//    boolean breakTime = false;
//    String[] months = {
//        "jan", "feb", "mar", "apr", "may", "jun",
//        "jul", "aug", "sep", "oct", "nov", "dec"
//    };

    /*! Create a new untitled document in a new sketch window.  */
    public static void handleNew() throws IOException {
        createNewEditor(null);
    }


    /*! Determine if the provided folder is a sketch folder or not */
    public static boolean isSketchFolder(File folder) {
        if(folder.isDirectory()) {
            File testFile = new File(folder, folder.getName() + ".ino");

            if(testFile.exists()) {
                return true;
            }

            testFile = new File(folder, folder.getName() + ".pde");

            if(testFile.exists()) {
                return true;
            }
        }

        return false;
    }

    /*! Find if a folder has at least one sketch underneath it */
    public static boolean pathContainsSketchSomewhere(File root) {
        if(!root.isDirectory()) {
            return false;
        }

        if(isSketchFolder(root)) {
            return true;
        }

        File[] files = root.listFiles();

        for(File f : files) {
            if(f.isDirectory()) {
                if(pathContainsSketchSomewhere(f)) {
                    return true;
                }
            }
        }

        return false;
    }

    /*! Opens a sketch given by *path* in a new Editor window */
    public static Editor createNewEditor(String path) throws IOException {
        Sketch s;

        if(path == null) {
            s = new Sketch((File)null);
        } else {
            s = new Sketch(path);
        }

        Editor editor = new Editor(s);
        editor.setVisible(true);

        if(path != null) {
            updateMRU(new File(path));
        }

        editor.getSketch().loadConfig();

        return editor;
    }

// .................................................................


    /*! Load all the libraries in the system */
    public static void gatherLibraries() {
//        libraryLoaderThread = new Thread() {
//            public void run() {
                Library.loadLibraries();
//            }
//        };
//        libraryLoaderThread.start();
    }

    public static Platform getPlatform() {
        return platform;
    }

    public static String getOSArch() {
        return System.getProperty("os.arch");
    }

    public static String getOSVersion() {
        return platform.getVersion();
    }

    public static String getOSFlavour() {
        return platform.getFlavour();
    }

    public static String getOSFullName() {
        return getOSName() + "_" + getOSArch();
    }

    public static String getOSName() {
        String osname = System.getProperty("os.name");

        if(osname.indexOf("Mac") != -1) {
            return "macosx";
        } else if(osname.indexOf("Windows") != -1) {
            return "windows";
        } else {
            return osname.toLowerCase();
        }
    }

    /**
    * returns true if Processing is running on a Mac OS X machine.
    */
    public static boolean isMacOS() {
        return getOSName().equals("macosx");
    }


    /**
    * returns true if running on windows.
    */
    public static boolean isWindows() {
        return getOSName().equals("windows");
    }


    /**
    * true if running on linux.
    */
    public static boolean isLinux() {
        return getOSName().equals("linux");
    }

    public static boolean isFreeBSD() {
        return getOSName().equals("freebsd");
    }

    public static boolean isUnix() {
        return isLinux() || isFreeBSD();
    }

    public static boolean isPosix() {
        return isLinux() || isFreeBSD() || isMacOS();
    }

    /**
    * Get the path to the platform's temporary folder, by creating
    * a temporary temporary file and getting its parent folder.
    * <br/>
    * Modified for revision 0094 to actually make the folder randomized
    * to avoid conflicts in multi-user environments. (Bug 177)
    */
    public static File createTempFolder(String name) {
        try {
            File folder = File.createTempFile(name, null);
            //String tempPath = ignored.getParent();
            //return new File(tempPath);
            tryDelete(folder);
            if (!folder.mkdirs()) { error("Unable to create temp folder " + folder.getAbsolutePath()); }
            return folder;

        } catch(Exception e) {
            error(e);
        }

        return null;
    }


    public static Set<File> getLibraries() {
        return libraries;
    }


    public static File getExamplesFolder() {
        return examplesFolder;
    }

    public static String getExamplesPath() {
        return examplesFolder.getAbsolutePath();
    }


    public static File getToolsFolder() {
        return toolsFolder;
    }


    public static String getToolsPath() {
        return toolsFolder.getAbsolutePath();
    }


    public static File getHardwareFolder() {
        // calculate on the fly because it's needed by Preferences.init() to find
        // the boards.txt and programmers.txt preferences files (which happens
        // before the other folders / paths get cached).
        return getContentFile("hardware");
    }

    public static File getSketchbookFolder() {
        String sbPath = preferences.get("locations.sketchbook");
        if (sbPath == null) {
            preferences.setFile("locations.sketchbook", platform.getDefaultSketchbookFolder());
            return platform.getDefaultSketchbookFolder();
        }
        return new File(preferences.get("locations.sketchbook"));
    }

    protected File getDefaultSketchbookFolder() {
        File sketchbookFolder = null;

        try {
            sketchbookFolder = platform.getDefaultSketchbookFolder();
        } catch(Exception e) { return null; }

        // create the folder if it doesn't exist already
        if(!sketchbookFolder.exists()) {
            if (!sketchbookFolder.mkdirs()) { error("Unable to make sketchbook folder " + sketchbookFolder.getAbsolutePath()); }
        }

        return sketchbookFolder;
    }

    /**
    * Used to determine whether to disable the "Show Sketch Folder" option.
    * @return true If a means of opening a folder is known to be available.
    */
    static protected boolean openFolderAvailable() {
        return platform.openFolderAvailable();
    }


    /**
    * Implements the other cross-platform headache of opening
    * a folder in the machine's native file browser.
    */
    public static void openFolder(File file) {
        try {
            platform.openFolder(file);

        } catch(Exception e) {
            showWarning(
                i18n.string("err.badfolder.title"),
                i18n.string("err.badfolder.body", file),
                e
            );
        }
    }


    // .................................................................


    /**
    * Prompt for a fodler and return it as a File object (or null).
    * Implementation for choosing directories that handles both the
    * Mac OS X hack to allow the native AWT file dialog, or uses
    * the JFileChooser on other platforms. Mac AWT trick obtained from
    * <A HREF="http://lists.apple.com/archives/java-dev/2003/Jul/msg00243.html">this post</A>
    * on the OS X Java dev archive which explains the cryptic note in
    * Apple's Java 1.4 release docs about the special System property.
    */
    public static File selectFolder(String prompt, File folder, Frame frame) {
        if(Base.isMacOS()) {
            if(frame == null) frame = new Frame();  //.pack();

            FileDialog fd = new FileDialog(frame, prompt, FileDialog.LOAD);

            if(folder != null) {
                fd.setDirectory(folder.getParent());
                //fd.setFile(folder.getName());
            }

            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fd.setVisible(true);
            System.setProperty("apple.awt.fileDialogForDirectories", "false");

            if(fd.getFile() == null) {
                return null;
            }

            return new File(fd.getDirectory(), fd.getFile());

        } else {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle(prompt);

            if(folder != null) {
                fc.setSelectedFile(folder);
            }

            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int returned = fc.showOpenDialog(new JDialog());

            if(returned == JFileChooser.APPROVE_OPTION) {
                return fc.getSelectedFile();
            }
        }

        return null;
    }


    // .................................................................


    /**
    * Give this Frame a Processing icon.
    */
    public static void setIcon(Frame frame) {
        try {
            frame.setIconImage(loadImageFromResource("icons/icon.png"));
        } catch(Exception e) {
            error(e);
        }
    }


    /**
    * Registers key events for a Ctrl-W and ESC with an ActionListener
    * that will take care of disposing the window.
    */
    public static void registerWindowCloseKeys(JRootPane root, ActionListener disposer) {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        root.registerKeyboardAction(disposer, stroke,
                                    JComponent.WHEN_IN_FOCUSED_WINDOW);

        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        stroke = KeyStroke.getKeyStroke('W', modifiers);
        root.registerKeyboardAction(disposer, stroke,
                                    JComponent.WHEN_IN_FOCUSED_WINDOW);
    }


    // .................................................................


    /**
    * "No cookie for you" type messages. Nothing fatal or all that
    * much of a bummer, but something to notify the user about.
    */
    public static void showMessage(String title, String message) {
        if(title == null) title = i18n.string("alert.message");

        if(headless) {
            System.out.println(title + ": " + message);

        } else {
            JOptionPane.showMessageDialog(new Frame(), message, title,
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }


    /**
    * Non-fatal error message with optional stack trace side dish.
    */
    public static void showWarning(String title, String message, Exception e) {
        if(title == null) title = i18n.string("alert.warning");

        if(headless) {
            System.out.println(title + ": " + message);

        } else {
            System.out.println(title + ": " + message);
            JOptionPane.showMessageDialog(new Frame(), message, title,
                                          JOptionPane.WARNING_MESSAGE);
        }

        if(e != null) error(e);
    }


    /**
    * Show an error message that's actually fatal to the program.
    * This is an error that can't be recovered. Use showWarning()
    * for errors that allow P5 to continue running.
    */
    public static void showError(String title, String message, Throwable e) {
        if(title == null) title = i18n.string("alert.error");

        if(headless) {
            System.err.println(title + ": " + message);

        } else {
            JOptionPane.showMessageDialog(new Frame(), message, title,
                                          JOptionPane.ERROR_MESSAGE);
        }

        if(e != null) error(e);

        System.exit(1);
    }


    // ...................................................................

    public static File getContentFile(String name) {
        String path = System.getProperty("user.dir");

        // Get a path to somewhere inside the .app folder
        if(Base.isMacOS()) {
            //      <key>javaroot</key>
            //      <string>$JAVAROOT</string>
            String javaroot = System.getProperty("javaroot");

            if(javaroot != null) {
                path = javaroot;
            }
        }

        File working = new File(path);

        if(name == null) {
            return working;
        }

        return new File(working, name);
    }

    public static void copyFile(File sourceFile, File targetFile) {
        try {
            Files.copy(sourceFile.toPath(), targetFile.toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            error(e);
        }
    }

    /**
    * Copy a folder from one place to another. This ignores all dot files and
    * folders found in the source directory, to avoid copying silly .DS_Store
    * files and potentially troublesome .svn folders.
    */
    public static void copyDir(File sourceDir, File targetDir) {
        try {
            if (!targetDir.exists()) {
                if (!targetDir.mkdirs()) {
                    error("Unable to make target folder " + targetDir.getAbsolutePath());
                    return;
                }
            }
            String files[] = sourceDir.list();

            for(int i = 0; i < files.length; i++) {
                // Ignore dot files (.DS_Store), dot folders (.svn) while copying
                if(files[i].charAt(0) == '.') continue;

                //if (files[i].equals(".") || files[i].equals("..")) continue;
                File source = new File(sourceDir, files[i]);
                File target = new File(targetDir, files[i]);

                if(source.isDirectory()) {
                    //target.mkdirs();
                    copyDir(source, target);
                    target.setLastModified(source.lastModified());
                } else {
                    copyFile(source, target);
                }
            }
        } catch(Exception e) {
            error(e);
        }
    }


    /**
    * Remove all files in a directory and the directory itself.
    */
    public static void removeDir(File dir) {
        if(dir.exists()) {
            Debug.message("Deleting folder " + dir.getAbsolutePath());
            removeDescendants(dir);
            tryDelete(dir);
        }
    }


    /**
    * Recursively remove all files within a directory,
    * used with removeDir(), or when the contents of a dir
    * should be removed, but not the directory itself.
    * (i.e. when cleaning temp files from lib/build)
    */
    public static void removeDescendants(File dir) {
        if(!dir.exists()) return;

        String files[] = dir.list();

        for(int i = 0; i < files.length; i++) {
            if(files[i].equals(".") || files[i].equals("..")) continue;

            File dead = new File(dir, files[i]);

            if(!dead.isDirectory()) {
                Base.tryDelete(dead);
            } else {
                removeDir(dead);
            }
        }
    }

    public static void loadPlugins() {

        PropertyFile availablePlugins = new PropertyFile("/org/uecide/config/plugins.txt");

        for (String k : availablePlugins.childKeys()) {
            String pluginClass = availablePlugins.get(k + ".class");
            String pluginName = availablePlugins.get(k + ".name");
            boolean pluginDefaultEnabled = availablePlugins.getBoolean(k + ".enabled");
            boolean pluginIsEnabled = Preferences.getBoolean("plugins.enable." + k, pluginDefaultEnabled);

            if (pluginIsEnabled) {
                try {
                    Debug.message("Enabling plugin " + pluginName);
                    Class cl = Class.forName(pluginClass);
                    plugins.put(cl.getName(), cl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        loadJSPlugins();
    }

    public static Version getPluginVersion(String plugin) {

        Class<?> pluginClass = plugins.get(plugin);
        if (pluginClass == null) {
            return new Version(null);
        }
        File f = Base.getJarLocation(pluginClass);

        try {
            JarFile myself = new JarFile(f);
            Manifest manifest = myself.getManifest();
            myself.close();
            Attributes manifestContents = manifest.getMainAttributes();

            return new Version(manifestContents.getValue("Version"));
        } catch (IOException ex) {
        }
        return new Version(null);
    }

    public static String findClassInZipFile(File file) {
        String base = file.getName();

        if(!base.endsWith(".jar")) {
            return null;
        }

        base = base.substring(0, base.length() - 4);

        String classFileName = "/" + base + ".class";


        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<?> entries = zipFile.entries();

            while(entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if(!entry.isDirectory()) {
                    String name = entry.getName();

                    if(name.endsWith(classFileName)) {
                        // Remove .class and convert slashes to periods.
                        zipFile.close();
                        return name.substring(0, name.length() - 6).replace('/', '.');
                    }
                }
            }
            zipFile.close();
        } catch(IOException e) {
            error(e);
        }

        return null;
    }

    public static File getTmpDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public static void applyPreferences() {
        setLookAndFeel();
        Editor.updateLookAndFeel();
    }

    public static void reloadPlugins() {
        loadPlugins();
        Editor.updateAllEditors();
    }

    static String openLauncher;
    static public void open(String filename) {
        open(new String[] { filename });
    }

    static public Process open(String argv[]) {
        String[] params = null;

        if(isWindows()) {
            params = new String[] { "cmd", "/c" };

        } else if(isMacOS()) {
            params = new String[] { "open" };

        } else if(isUnix()) {
            if(openLauncher == null) {
                try {
                    Process p = Runtime.getRuntime().exec(new String[] { "gnome-open" });
                    /*int result =*/ p.waitFor();
                    openLauncher = "gnome-open";
                } catch(Exception e) { }
            }

            if(openLauncher == null) {
                // Attempt with kde-open
                try {
                    Process p = Runtime.getRuntime().exec(new String[] { "kde-open" });
                    /*int result =*/ p.waitFor();
                    openLauncher = "kde-open";
                } catch(Exception e) { }
            }

            if(openLauncher == null) {
                System.err.println("Could not find gnome-open or kde-open, " +
                                   "the open() command may not work.");
            }

            if(openLauncher != null) {
                params = new String[] { openLauncher };
            }
        }

        if(params != null) {
            if(params[0].equals(argv[0])) {
                return exec(argv);
            } else {
                params = concat(params, argv);
                return exec(params);
            }
        } else {
            return exec(argv);
        }
    }

    static public Process exec(String[] argv) {
        try {
            return Runtime.getRuntime().exec(argv);
        } catch(Exception e) {
            error(e);
            return null;
        }
    }

    static public String[] concat(String a[], String b[]) {
        String c[] = new String[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    static public File getDataFolder() {
        File out = null;
        if (overrideSettingsFolder != null) {
            out = new File(overrideSettingsFolder);
        } else {
            if ((preferences != null) && (preferences.getFile("locations.data") != null)) {
                out = preferences.getFile("locations.data");
            } else {
                File portable = new File(getJarLocation().getParentFile(), "portable");
                if (portable.exists() && portable.isDirectory()) {
                    out = portable;
                } else {
                    out = platform.getSettingsFolder();
                }
            }
        }
        if (!out.exists()) {
            out.mkdirs();
        }
        return out;
    }
    
    static public File getDataFolder(String dir) {
        File out = new File(getDataFolder(), dir);
        if (!out.exists()) {
            out.mkdirs();
        }
        return out;
    }

    static public File getDataFile(String file) {
        return new File(getDataFolder(), file);
    }
    
    static public File getCacheFolder() { return getDataFolder("cache"); }

    static public File[] getAnyFolders(String type) {
        ArrayList<File> locs = new ArrayList<File>();
        locs.add(getDataFolder(type));
        locs.add(getDataFolder("usr/share/uecide/" + type));
        if (isPosix()) {
            locs.add(new File("/usr/share/uecide/" + type));
        }

        locs.add(new File(getSketchbookFolder(), type));
        return locs.toArray(new File[0]);
    }

    static public File[] getCoresFolders() { return getAnyFolders("cores"); }
    static public File[] getToolsFolders() { return getAnyFolders("tools"); }
    static public File[] getProgrammersFolders() { return getAnyFolders("programmers"); }
    static public File[] getBoardsFolders() { return getAnyFolders("boards"); }
    static public File[] getPluginsFolders() { return getAnyFolders("plugins"); }
    static public File[] getCompilersFolders() { return getAnyFolders("compilers"); }
    static public File[] getLibrariesFolders() { return getAnyFolders("libraries"); }
    static public File[] getIconsFolders() { return getAnyFolders("icons"); }

    static public void errorReport(Thread t, Throwable e) {
        showError("Uncaught Exception", "An uncaught exception occurred in thread " + t.getName() + " (" + t.getId() + ")\n" +
                  "The cause is: " + e.getCause() + "\n" +
                  "The message is: " + e.getMessage() + "\n", e);
    }

    static public void broken(Thread t, Throwable e) {
        if (headless) {
            e.printStackTrace();
            return;
        }
        try {
            e.printStackTrace();
            if(e.getCause() == null) {
                return;
            }


            Debug.message("");
            Debug.message("******************** EXCEPTION ********************");
            Debug.message("An uncaught exception occurred in thread " + t.getName() + " (" + t.getId() + ")");
            Debug.message("    The cause is: " + e.getCause());
            Debug.message("    The message is: " + e.getMessage());
            Debug.message("");

            for(StackTraceElement element : e.getStackTrace()) {
                if(element != null) {
                    Debug.message("        " + element);
                }
            }

            Debug.message("******************** EXCEPTION ********************");
            Debug.message("");
        } catch(Exception ee) {
            ee.printStackTrace();
        }
    }

    public static byte[] loadBytesRaw(File file) throws FileNotFoundException, IOException {
        FileInputStream input = new FileInputStream(file);

        int size = (int) file.length();
        byte buffer[] = new byte[size];
        int offset = 0;
        int bytesRead;

        while((bytesRead = input.read(buffer, offset, size - offset)) != -1) {
            offset += bytesRead;
            if(bytesRead == 0) break;
        }

        input.close();

        return buffer;
    }


    public static void error(String e) {
        Editor.broadcastError(e);
        System.err.println(e);
        Debug.message(e);
    }

    public static void error(Throwable e) {

        Editor.broadcastError(e.getMessage());

        try {
            Debug.message("");
            Debug.message("******************** EXCEPTION ********************");
            Debug.message("An uncaught exception occurred:");
            Debug.message("    The cause is: " + e.getCause());
            Debug.message("    The message is: " + e.getMessage());
            Debug.message("");

            for(StackTraceElement element : e.getStackTrace()) {
                if(element != null) {
                    Debug.message("        " + element);
                }
            }

            Debug.message("******************** EXCEPTION ********************");
            Debug.message("");
        } catch(Exception ee) {
            ee.printStackTrace();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        System.err.println(sw.toString());
    }

    // This handy little function will rebuild the whole of the internals of
    // UECIDE - that is, all the boards, cores, compilers and libraries etc.
    public static void cleanAndScanAllSettings() throws IOException {
        try {

            SwingWorker<String, Object> worker = new SwingWorker<String, Object>() {
                @Override
                protected String doInBackground() throws Exception {
                    cacheSystemFiles();
                    Editor.lockAll();
                    Editor.bulletAll(i18n.string("msg.loading.serial"));
                    Serial.updatePortList();
                    Serial.fillExtraPorts();
                    Editor.bulletAll(i18n.string("msg.loading.assets"));

                    IconManager.loadIconSets();
                    rescanCompilers();
                    rescanCores();
                    rescanBoards();
                    rescanProgrammers();
                    rescanTools();
                    rescanPlugins();
                    rescanLibraries();

                    waitForAssetLoading();

                    buildPreferencesTree();

                    IconManager.setIconFamily(Preferences.get("theme.icons"));

                    return "OK";
                }

                @Override
                protected void done() {
                    Editor.unlockAll();
                    Editor.bulletAll(i18n.string("msg.loading.updating"));
                    try { Editor.updateAllEditors(); } catch (Exception e) {}
                    try { Editor.selectAllEditorBoards(); } catch (Exception e) {}
                    try { Editor.refreshAllEditors(); } catch (Exception e) {}
                    Editor.bulletAll(i18n.string("msg.done"));
                }
            };
            worker.execute();

            //Editor.unlockAll();
        } catch (Exception e) {
        }
    }

    public static void rescanPlugins() {
//        plugins = new TreeMap<String, Class<?>>();
//        lookAndFeels = new TreeMap<String, Class<?>>();
//        pluginInstances = new ArrayList<Plugin>();
        loadPlugins();
    }

    public static void rescanCompilers() {
        compilers = new TreeMap<String, Compiler>();
        loadCompilers();
    }

    public static void rescanTools() {
        tools = new TreeMap<String, Tool>();
        loadTools();
    }

    public static void rescanCores() {
        cores = new TreeMap<String, Core>();
        loadCores();
    }

    public static void rescanProgrammers() {
        try {
            programmers = new TreeMap<String, Programmer>();
            loadProgrammers();
            Editor.updateAllEditors();
            Editor.selectAllEditorProgrammers();
        } catch(Exception e) {
            error(e);
        }
    }

    public static void rescanBoards() {
        try {
            boards = new TreeMap<String, Board>();
            loadBoards();
            Editor.updateAllEditors();
            Editor.selectAllEditorBoards();
        } catch(Exception e) {
            error(e);
        }
    }

    public static void rescanLibraries() {
        gatherLibraries();
    }

    public static void updateLookAndFeel() {
        Editor.updateLookAndFeel();
    }

    public static BufferedImage loadImageFromResource(String res) {
        if(!res.startsWith("/")) {
            res = "/org/uecide/" + res;
        }

        URL loc = Base.class.getResource(res);

        if(loc == null) {
            loc = Base.class.getResource("/org/uecide/icons/unknown.png");
        }

        try {
            BufferedImage im = ImageIO.read(loc);
            return im;
        } catch(Exception e) {
            error(e);
        }

        return null;
    }

    public static boolean copyResourceToFile(String res, File dest) {
        try {
            InputStream from = Base.class.getResourceAsStream(res);
            OutputStream to =
                new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buffer = new byte[16 * 1024];
            int bytesRead;

            while((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }

            to.flush();
            from.close();
            to.close();
        } catch(Exception e) {
            error(e);
            return false;
        }

        return true;
    }

    public Version getLatestVersion() {
        try {
            HttpRequest req = new HttpRequest("https://uecide.org/version.txt");
            String ver = req.getText();
            return new Version(ver);
/*
            URL url = new URL("https://uecide.org/version.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String data = in.readLine();
            in.close();
            return new Version(data);
*/
        } catch(Exception e) {
            // Unable to get new version details - return nothing.
            // Also switch to offline mode since there was an error.
            onlineMode = false;
        }

        return null;
    }

    public static boolean isOnline() {
        return onlineMode;
    }

    public static void setOnlineMode() {
        onlineMode = true;
    }

    public static void setOfflineMode() {
        onlineMode = false;
    }

    public boolean isNewVersionAvailable() {
        if (!isOnline()) {
            return false;
        }
        int time = (int)(System.currentTimeMillis() / 1000L);
        Preferences.setInteger("version.lastcheck", time);

        if(Preferences.getBoolean("editor.version_check")) {
            Version newVersion = getLatestVersion();

            if(newVersion == null) {
                return false;
            }

            if(newVersion.compareTo(systemVersion) > 0) {
                return true;
            }

            return false;
        }

        return false;
    }

    // Is it time to check the version?  Was the last version check
    // more than 3 hours ago?
    public boolean isTimeToCheckVersion() {
        int lastCheck = Preferences.getInteger("version.lastcheck");
        int time = (int)(System.currentTimeMillis() / 1000L);

        if(time > (lastCheck + (3 * 60 * 60))) {
            return true;
        }

        return false;
    }

    public static void debug(String msg) {
        if(!extraDebug) return;

        if(msg == null) {
            msg = "(null)";
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        Thread t = Thread.currentThread();
        StackTraceElement[] st = t.getStackTrace();
        StackTraceElement caller = st[2];
        System.err.print(caller.getFileName() + " " + caller.getLineNumber() + " (" + caller.getMethodName() + "): " + msg);
    }

    public static String getFileAsString(File f) {
        if (f == null) {
            return "";
        }
        if (!f.exists()) {
            return "";
        }
        if (f.isDirectory()) {
            return "";
        }
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(f));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
        return "";
    }

    public static boolean yesno(String title, String question) {
        if (!headless) {
            return JOptionPane.showOptionDialog(null, question, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null) == JOptionPane.YES_OPTION;
        }
        return false;
    }

    public static void buildPreferencesTree() {
        preferencesTree = new PropertyFile();


        for(Programmer c : programmers.values()) {
            PropertyFile prefs = c.getProperties().getChildren("prefs");
            for (String k : prefs.keySet()) {
                prefs.setSource(k, "programmer:" + c.getName());
            }
            preferencesTree.mergeData(prefs);
        }

        for(Compiler c : compilers.values()) {
            PropertyFile prefs = c.getProperties().getChildren("prefs");
            for (String k : prefs.keySet()) {
                prefs.setSource(k, "compiler:" + c.getName());
            }
            preferencesTree.mergeData(prefs);
        }

        for(Core c : cores.values()) {
            PropertyFile prefs = c.getProperties().getChildren("prefs");
            for (String k : prefs.keySet()) {
                prefs.setSource(k, "core:" + c.getName());
            }
            preferencesTree.mergeData(prefs);
        }

        for(Board c : boards.values()) {
            PropertyFile prefs = c.getProperties().getChildren("prefs");
            for (String k : prefs.keySet()) {
                prefs.setSource(k, "board:" + c.getName());
            }
            preferencesTree.mergeData(prefs);
        }

        for (Map.Entry<String, Class<?>> p : plugins.entrySet()) {
            try {
                Class<?> plg = p.getValue();
                Method m = plg.getMethod("getPreferencesTree");
                Object[] foo = null;
                PropertyFile prefs = (PropertyFile)m.invoke(null, foo);
                for (String k : prefs.keySet()) {
                    prefs.setSource(k, "plugin:" + p.getKey());
                }
                preferencesTree.mergeData(prefs);
            } catch (Exception e) {
            }
        }

        loadPreferencesTree("/org/uecide/config/prefs.txt");
        Context ctx = new Context();

        for (JSPlugin p : jsplugins.values()) {
            PropertyFile pf = (PropertyFile)p.call("getPreferencesTree", ctx, null);
            if (pf != null) {
                Base.preferencesTree.mergeData(pf);
            }
            p.onBoot();
        }

    }

    public static void registerPreference(String key, String type, String name, String def) {
        registerPreference(key, type, name, def, null);
    }
        
    public static void registerPreference(String key, String type, String name, String def, String plat) {
        preferencesTree.set(key + ".type", type);
        preferencesTree.set(key + ".name", name);
        if (plat == null) {
            preferencesTree.set(key + ".default", def);
        } else {
            preferencesTree.set(key + ".default." + plat, def);
        }
    }

    public static void loadPreferencesTree(String res) {
        PropertyFile pf = new PropertyFile(res);
        preferencesTree.mergeData(pf);
    }

    public static Object executeJavaScript(String resource, String function, Object[] args) {
        Object ret = null;
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");

            String script = getResourceAsString(resource);

            if (script == null) { return null; }
            if (script.equals("")) { return null; }

            engine.eval(script);

            Invocable inv = (Invocable)engine;
            ret = inv.invokeFunction(function, args);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static String getResourceAsString(String resource) {
        String out = "";
        try {
            InputStream from = Base.class.getResourceAsStream(resource);
            int bytesRead;

            StringBuilder sb = new StringBuilder();

            String line = null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(from));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            out = sb.toString();

            reader.close();
            from.close();
        } catch(Exception e) {
            error(e);
        }
        return out;
    }

    // This little routine works through each and every board, core and compiler and
    // runs any "init.script.*" lines.
    public static void runInitScripts() {
        for (Board b : boards.values()) {
            if (b.get("init.script.0") != null) {
                Context ctx = new Context();
                ctx.setBoard(b);
                ctx.executeKey("init.script");
            }
        }
        for (Core c : cores.values()) {
            if (c.get("init.script.0") != null) {
                Context ctx = new Context();
                ctx.setCore(c);
                ctx.executeKey("init.script");
            }
        }
        for (Compiler c : compilers.values()) {
            if (c.get("init.script.0") != null) {
                Context ctx = new Context();
                ctx.setCompiler(c);
                ctx.executeKey("init.script");
            }
        }
        for (Programmer c : programmers.values()) {
            if (c.get("init.script.0") != null) {
                Context ctx = new Context();
                ctx.setProgrammer(c);
                ctx.executeKey("init.script");
            }
        }
    }

    // If the package manager hasn't been configured then 
    // configure it, do an update, and then install the base packages.

    public static void initPackageManager() {
        try {
            File aptFolder = getDataFolder("apt");
            if (!aptFolder.exists()) {
                aptFolder.mkdirs();
            }
            File cacheFolder = new File(aptFolder, "cache");
            if (!cacheFolder.exists()) {
                cacheFolder.mkdirs();
            }
            File dbFolder = new File(aptFolder, "db");
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }
            File packagesFolder = new File(dbFolder, "packages");
            if (!packagesFolder.exists()) {
                packagesFolder.mkdirs();
            }

            File sourcesDir = new File(dbFolder, "sources.d");

            if (!sourcesDir.exists()) {
                sourcesDir.mkdirs();
            }
            File sourcesFile = new File(sourcesDir, "internal.db");
            if (!sourcesFile.exists()) {
                PrintWriter pw = new PrintWriter(sourcesFile);
                pw.println("deb res://org/uecide/dist uecide main");
                pw.close();
            }

            PluginManager reqpm = new PluginManager();
            APT reqapt = reqpm.getApt();
            reqapt.update(true, true);
            Package[] reqpkgs = reqapt.getPackages();
            for (Package p : reqpkgs) {
                if (reqapt.isInstalled(p)) {
                    if (reqapt.isUpgradable(p)) {
                        reqapt.upgradePackage(p);
                    }
                } else {
                    reqapt.installPackage(p);
                }
            }
//            reqapt.save();

        } catch (Exception e) {
            error(e);
        }

    }

    static String jTattooTheme(String name) {
        String fontData = Preferences.get("theme.jtattoo.aafont");
        String colourData = Preferences.get("theme.jtattoo.themes." + name);

        if (fontData == null) fontData = "Default";
        if (colourData == null) colourData = "Default";

        String full = colourData + "-" + fontData;

        full = full.replace("Default-", "");
        full = full.replace("-Default", "");

        return full;
    }

    public static void setLookAndFeel() {
        String lafname = Preferences.get("theme.laf");

        LookAndFeel laf = null;

        try {

            if (lafname.equals("gnome")) { 
                new GnomeLAF().applyLAF(); 
            } else if (lafname.equals("acryl")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.acryl.AcrylLookAndFeel.setTheme(jTattooTheme("acryl"));
                com.jtattoo.plaf.acryl.AcrylLookAndFeel.setTheme(p);
                new JTattooAcrylLAF().applyLAF(); 
            } else if (lafname.equals("aero")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.aero.AeroLookAndFeel.setTheme(jTattooTheme("aero"));
                com.jtattoo.plaf.aero.AeroLookAndFeel.setTheme(p);
                new JTattooAeroLAF().applyLAF();
            } else if (lafname.equals("aluminium")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.aluminium.AluminiumLookAndFeel.setTheme(jTattooTheme("aluminium"));
                com.jtattoo.plaf.aluminium.AluminiumLookAndFeel.setTheme(p);
                new JTattooAluminiumLAF().applyLAF(); 
            } else if (lafname.equals("bernstein")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.bernstein.BernsteinLookAndFeel.setTheme(jTattooTheme("bernstein"));
                com.jtattoo.plaf.bernstein.BernsteinLookAndFeel.setTheme(p);
                new JTattooBernsteinLAF().applyLAF(); 
            } else if (lafname.equals("fast")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.fast.FastLookAndFeel.setTheme(jTattooTheme("fast"));
                com.jtattoo.plaf.fast.FastLookAndFeel.setTheme(p);
                new JTattooFastLAF().applyLAF(); 
            } else if (lafname.equals("graphite")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.graphite.GraphiteLookAndFeel.setTheme(jTattooTheme("graphite"));
                com.jtattoo.plaf.graphite.GraphiteLookAndFeel.setTheme(p);
                new JTattooGraphiteLAF().applyLAF(); 
            } else if (lafname.equals("hifi")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.hifi.HiFiLookAndFeel.setTheme(jTattooTheme("hifi"));
                com.jtattoo.plaf.hifi.HiFiLookAndFeel.setTheme(p);
                new JTattooHiFiLAF().applyLAF(); 
            } else if (lafname.equals("luna")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.luna.LunaLookAndFeel.setTheme(jTattooTheme("luna"));
                com.jtattoo.plaf.luna.LunaLookAndFeel.setTheme(p);
                new JTattooLunaLAF().applyLAF(); 
            } else if (lafname.equals("mcwin")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.mcwin.McWinLookAndFeel.setTheme(jTattooTheme("mcwin"));
                com.jtattoo.plaf.mcwin.McWinLookAndFeel.setTheme(p);
                new JTattooMcWinLAF().applyLAF(); 
            } else if (lafname.equals("mint")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.mint.MintLookAndFeel.setTheme(jTattooTheme("mint"));
                com.jtattoo.plaf.mint.MintLookAndFeel.setTheme(p);
                new JTattooMintLAF().applyLAF(); 
            } else if (lafname.equals("noire")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.noire.NoireLookAndFeel.setTheme(jTattooTheme("noire"));
                com.jtattoo.plaf.noire.NoireLookAndFeel.setTheme(p);
                new JTattooNoireLAF().applyLAF(); 
            } else if (lafname.equals("smart")) { 
                Properties p = new Properties();
                p.put("windowDecoration", Preferences.getBoolean("theme.jtattoo.customdec") ? "on" : "off");
                p.put("macStyleWindowDecoration", Preferences.getBoolean("theme.jtattoo.macdec") ? "on" : "off");
                p.put("logoString", "UECIDE");
                p.put("textAntiAliasing", "on");
                com.jtattoo.plaf.smart.SmartLookAndFeel.setTheme(jTattooTheme("smart"));
                com.jtattoo.plaf.smart.SmartLookAndFeel.setTheme(p);
                new JTattooSmartLAF().applyLAF(); 
            }
            else if (lafname.equals("liquid")) { new LiquidLAF().applyLAF(); }
            else if (lafname.equals("metal")) { new MetalLAF().applyLAF(); }
            else if (lafname.equals("motif")) { new MotifLAF().applyLAF(); }
            else if (lafname.equals("nimbus")) { new NimbusLAF().applyLAF(); }
            else if (lafname.equals("office2003")) { new Office2003LAF().applyLAF(); }
            else if (lafname.equals("officexp")) { new OfficeXPLAF().applyLAF(); }
            else if (lafname.equals("systemdefault")) { new SystemDefaultLAF().applyLAF(); }
            else if (lafname.equals("tinyforest")) { new TinyForestLAF().applyLAF(); }
            else if (lafname.equals("tinygolden")) { new TinyGoldenLAF().applyLAF(); }
            else if (lafname.equals("tinynightly")) { new TinyNightlyLAF().applyLAF(); }
            else if (lafname.equals("tinyplastic")) { new TinyPlasticLAF().applyLAF(); }
            else if (lafname.equals("tinysilver")) { new TinySilverLAF().applyLAF(); }
            else if (lafname.equals("tinyunicode")) { new TinyUnicodeLAF().applyLAF(); }
            else if (lafname.equals("vs2005")) { new VisualStudio2005LAF().applyLAF(); }
            else if (lafname.equals("material")) { new MaterialLAF().applyLAF(); }
            else if (lafname.equals("arduino")) { new ArduinoLAF().applyLAF(); }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isQuiet() {
        return cli.isSet("quiet");
    }

    public static Locale getLocale() {
        if (cli != null) {
            if (cli.isSet("locale")) {
                String[] bits = cli.getString("locale").split("_");
                if (bits.length == 2) {
                    return new Locale(bits[0], bits[1]);
                }
            }
        }
        return Locale.getDefault();
    }

    public static void loadJSPlugins() {
        jsplugins = new HashMap<String, JSPlugin>();

        File[] pfs = getPluginsFolders();
        for (File pf : pfs) {
            loadJSPluginsFromFolder(pf);
        }

        for (JSPlugin p : jsplugins.values()) {
            p.onBoot();
        }
    }

    public static void loadJSPluginsFromFolder(File f) {
        if (!f.exists()) return;
        if (!f.isDirectory()) return;
        File[] files = f.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".jpl")) {
                Debug.message("Loading javascript plugin " + file.getAbsolutePath());
                JSPlugin p = new JSPlugin(file);
                JSPlugin op = jsplugins.get(file.getName());
                if (op != null) {
                    Version vold = new Version(op.getVersion());
                    Version vnew = new Version(p.getVersion());
                    if (vold.compareTo(vnew) < 0) {
                        jsplugins.put(file.getName(), p);
                    }
                } else {
                    jsplugins.put(file.getName(), p);
                }
            }
        }
    }

    public static void tryDelete(File file) {
        // If it's not there, do nothing.
        if (!file.exists()) {
            return;
        }

        // Try and delete it
        try {
            file.delete();
        } catch (Exception e) {
        }

        // If it deleted then return
        if (!file.exists()) return;
    }

    public static void waitForAssetLoading() {
//        while (coreLoaderThread != null) {
//            try {
//                coreLoaderThread.join();
//                coreLoaderThread = null;
//            } catch (Exception e) {
//            }
//        }
//
//        while (compilerLoaderThread != null) {
//            try {
//                compilerLoaderThread.join();
//                compilerLoaderThread = null;
//            } catch (Exception e) {
//            }
//        }
//
//        while (boardLoaderThread != null) {
//            try {
//                boardLoaderThread.join();
//                boardLoaderThread = null;
//            } catch (Exception e) {
//            }
//        }
//       
//        while (programmerLoaderThread != null) {
//            try {
//                programmerLoaderThread.join();
//                programmerLoaderThread = null;
//            } catch (Exception e) {
//            }
//        }
//       
//        while (toolLoaderThread != null) {
//            try {
//                toolLoaderThread.join();
//                toolLoaderThread = null;
//            } catch (Exception e) {
//            }
//        }
//       
//        while (libraryLoaderThread != null) {
//            try {
//                libraryLoaderThread.join();
//                libraryLoaderThread = null;
//            } catch (Exception e) {
//            }
//        }
//
//        while (cleanupThread != null) {
//            try {
//                cleanupThread.join();
//                cleanupThread = null;
//            } catch (Exception e) {
//            }
//        }
    }

    public static String getFileExtension(File f) {
        String fileName = f.getName();
        String extension = "";

        int i = fileName.lastIndexOf('.');
        if (i >= 0) {
            extension = fileName.substring(i+1);
        }
        return extension;
    }

    public static Tool getTool(String name) {
        return tools.get(name);
    }

    public static void loadAssets() throws IOException {
        Debug.message("Loading assets");
        Debug.message("Caching system files");
        cacheSystemFiles();

        loadManualPages();

        Debug.message("Loading cores");
        loadCores();
//        waitForAssetLoading();

        Debug.message("Loading compilers");
        loadCompilers();

        Debug.message("Loading boards");
        loadBoards();

        Debug.message("Loading programmers");
        loadProgrammers();

        Debug.message("Loading tools");
        loadTools();

        Debug.message("Loading plugins");
        loadPlugins();

        Debug.message("Loading icon sets");
        IconManager.loadIconSets();


        Debug.message("Loading libraries");
        gatherLibraries();

//        waitForAssetLoading();

        Debug.message("Loading assets done");
        IconManager.setIconFamily(Preferences.get("theme.icons"));
    }

    public static boolean isHeadless() {
        return headless;
    }

    public static void loadManualPages() {
        manualPages = new PropertyFile();
        PropertyFile manualIndex = new PropertyFile("/org/uecide/manual.txt");
        for (String k : manualIndex.keySet()) {
            String filename = manualIndex.get(k);
            PropertyFile partFile = new PropertyFile("/org/uecide/manual/" + filename + ".txt");
            if (k.equals("Global")) {
                manualPages.mergeData(partFile);
            } else {
                manualPages.mergeData(partFile, k);
            }
        }
    }
}

