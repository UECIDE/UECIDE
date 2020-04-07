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

import org.uecide.gui.Gui;
import org.uecide.gui.swing.SwingGui;
import org.uecide.gui.cli.CliGui;
import org.uecide.gui.action.ActionGui;
import org.uecide.gui.none.NoneGui;
import org.uecide.gui.html.HTMLGui;

import org.uecide.actions.Action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.PasswordAuthentication;
import java.net.Authenticator;
import java.net.URI;
import java.net.URL;

import java.security.ProtectionDomain;
import java.security.CodeSource;

import java.awt.GraphicsEnvironment;

public class UECIDE {

    class ActionSpec {
        String action;
        Object[] args;
        public ActionSpec(String action, Object... args) {
            this.action = action; 
            this.args = args;
        }

        public String getAction() {
            return action;
        }
        public Object[] getArgs() {
            return args;
        }
    }

    public static HashMap<String, PropertyFile> iconSets = new HashMap<String, PropertyFile>();

    public static String RELEASE = "release";

    public static String overrideSettingsFolder = null;

    public static ArrayList<Process> processes = new ArrayList<Process>();

    static Platform platform;

    static private boolean headless;

    // these are static because they're used by Sketch
    static private File examplesFolder;
    static private File librariesFolder;
    static private File toolsFolder;
    static private File hardwareFolder;
    public static ArrayList<File> MRUList;
    public static HashMap<File,Integer> MCUList;
    public static String gui;

    // maps imported packages to their library folder
    public static TreeMap<String, File> importToLibraryTable;

    // classpath for all known libraries for p5
    // (both those in the p5/libs folder and those with lib subfolders
    // found in the sketchbook)
    public static String librariesClassPath;

    public static Version systemVersion;

    public static Context systemContext;

    public static ArrayList<Context> sessions = new ArrayList<Context>();

    public static CommandLine cli = new CommandLine();

    public static PropertyFile session = new PropertyFile();

    public static PropertyFile webLinks;

//    public static HashMap<Object, DiscoveredBoard> discoveredBoards = new HashMap<Object, DiscoveredBoard>();

    public static boolean onlineMode = true;

    public static TreeSet<CommunicationPort> communicationPorts = new TreeSet<CommunicationPort>();

    public static I18N i18n = new I18N("Core");

    public static void main(String args[]) {
//        replaceSystemClassLoader();
        try {
            new UECIDE(args);
        } catch (Exception e) {
            Debug.exception(e);
        }
    }

    /*! Return a File representing the location of the JAR file the
     *  application was loaded from. */
    public static File getJarLocation() {
        return getJarLocation(UECIDE.class);
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
            Debug.exception(e);
            UECIDE.error(e);
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
    public UECIDE(String[] args) throws IOException {
        Action.initActions();
//        cli.addParameter("debug",               "",         Boolean.class,  "cli.help.debug");
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

        cli.addParameter("log",                 "file",     String.class,   "cli.help.log");

        cli.addParameter("update",              "",         Boolean.class,  "cli.help.update");
        cli.addParameter("install",             "package",  String.class,   "cli.help.install");
        cli.addParameter("remove",              "package",  String.class,   "cli.help.remove");
        cli.addParameter("remove-all",          "",         Boolean.class,  "cli.help.remove-all");
        cli.addParameter("upgrade-all",         "",         Boolean.class,  "cli.help.upgrade");
        cli.addParameter("upgrade",             "package",  String.class,   "cli.help.upgrade");
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
        cli.addParameter("set",                 "key=val",  String.class,   "cli.help.set");
        cli.addParameter("reset",               "key",      String.class,   "cli.help.reset");
        cli.addParameter("reset-preferences",   "",         Boolean.class,  "cli.help.reset.prefs");
        cli.addParameter("quiet",               "",         Boolean.class,  "cli.help.quiet");


        cli.addParameter("locale",              "name",     String.class,   "cli.help.locale");

        cli.addParameter("gui",                 "name",     String.class,   "Select a GUI to run (cli / swing / none)");
        cli.addParameter("laf",                 "name",     String.class,   "Select a LookAndFeel for the Swing GUI");

        cli.addParameter("action",              "string",   String.class,   "Execute an action (parameters separated by ::)");


        String[] argv = cli.process(args);

        if (cli.isSet("log")) {
            File f = new File(cli.getString("log")[0]);
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);
            System.out.println("Logging to " + f.getAbsolutePath());
            System.setErr(ps);
            System.setOut(ps);
        }

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

        boolean doExit = false;



        // Early debugging flags. Must be the first to process

        if (cli.isSet("verbose")) {
            Debug.setVerbose(cli.isSet("verbose"));
        }

        if (cli.isSet("datadir")) {
            overrideSettingsFolder = cli.getString("datadir")[0];
        }

        Context bootContext = createContext(null, "none", false);
        bootContext.setSystemContext(true);

        PropertyFile versionInfo = new PropertyFile("/org/uecide/version.txt");
        RELEASE = versionInfo.get("Release");
        systemVersion = new Version(versionInfo.get("Version"));

        // One-off single-shot commands that enforce a "none" GUI, or commands that
        // want to run in a "none" GUI regardless of which GUI is selected.

        if (cli.isSet("version")) {
            bootContext.message("UECIDE Version " + systemVersion);
            bootContext.message("(c) 2020 Majenko Technologies");
            bootContext.action("CloseSession");
        }




        initPlatform();


        if (cli.isSet("reset-preferences")) {
            
            File prefsFile = getDataFile("preferences.txt");
            try {
                prefsFile.delete();
            } catch (Exception e) {
                Debug.exception(e);
                System.exit(10);
            }
            System.out.println(">>> All preferences reset to default! <<<");
        }

        Preferences.init();

        if (Preferences.getBoolean("editor.hwaccel")) {
            Properties props = System.getProperties();
            props.setProperty("sun.java2d.opengl", "true");
        } else {
            Properties props = System.getProperties();
            props.setProperty("sun.java2d.opengl", "false");
        }

        platform.setSettingsFolderEnvironmentVariable();

        if (Preferences.getBoolean("network.offline")) {
            setOfflineMode();
        }

        if (cli.isSet("online")) {
            setOnlineMode();
        }

        if (cli.isSet("offline")) {
            setOfflineMode();
        }
        
        if (cli.isSet("update")) {
            bootContext.action("AptUpdate");
            doExit = true;
        }

        if (cli.isSet("upgrade-all")) {
            bootContext.action("AptUpgrade");
            doExit = true;
        }
    
        if (cli.isSet("upgrade")) {
            for (String p : cli.getString("upgrade")) {
                bootContext.action("AptUpgrade", p);
            }
            doExit = true;
        }

        if (cli.isSet("install")) {
            for (String packageName : cli.getString("install")) {
                bootContext.action("AptInstall", packageName);
            }
            doExit = true;
        }

        if (cli.isSet("remove")) {
            for (String packageName : cli.getString("remove")) {
                bootContext.action("AptRemove", packageName);
            }
            doExit = true;
        }

        if (cli.isSet("search")) {
            for (String packageName : cli.getString("search")) {
                bootContext.action("AptSearch", packageName);
            }
            doExit = true;
        }

//        if (cli.isSet("remove-all")) {
//            try {
//                if (!cli.isSet("force")) {
//                    System.err.println(i18n.string("err.notremove"));
//                } else {
//                    APT apt = APT.factory();
//                    for (Package p : apt.getInstalledPackages()) {
//                        apt.uninstallPackage(p, true);
//                    }
//                }
//            } catch (Exception ex) { error(ex); }
//            doExit = true;
//        }

//        if (cli.isSet("list")) {
//            try {
//                APT apt = APT.factory();
//                Package[] pkgs = apt.getPackages();
//                String format = "%-50s %10s %10s %s";
//                System.out.println(String.format(format, i18n.string("apt.list.package"), i18n.string("apt.list.installed"), i18n.string("apt.list.available"), ""));
//                ArrayList<Package> out = new ArrayList<Package>();
//                for (Package p : pkgs) {
//                    if (cli.isSet("section")) {
//                        if (p.get("Section") == null) { continue; }
//                        if (!p.get("Section").equals(cli.getString("section"))) { continue; }
//                    }
//                    if (cli.isSet("family")) {
//                        if (p.get("Family") == null) { continue; }
//                        if (!p.get("Family").equals(cli.getString("family"))) { continue; }
//                    }
//                    if (cli.isSet("group")) {
//                        if (p.get("Group") == null) { continue; }
//                        if (!p.get("Group").equals(cli.getString("group"))) { continue; }
//                    }
//                    if (cli.isSet("subgroup")) {
//                        if (p.get("Subgroup") == null) { continue; }
//                        if (!p.get("Subgroup").equals(cli.getString("subgroup"))) { continue; }
//                    }
//
//                    String name = p.getName();
//                    Package instPack = apt.getInstalledPackage(p.getName());
//                    Version avail = p.getVersion();
//                    Version inst = null;
//                    String msg = "";
//                    if (instPack != null) {
//                        inst = instPack.getVersion();
//                        if (avail.compareTo(inst) > 0) {
//                            msg = i18n.string("apt.list.update");
//                        }
//                    }
//                    System.out.println(String.format(format, name, inst == null ? "" : inst.toString(), avail.toString(), msg));
//                    System.out.println("  " + p.getDescriptionLineOne());
//
//                }
//            } catch (Exception ex) { error(ex); }
//            doExit = true;
//        }
//                
//        if (cli.isSet("search")) {
//            try {
//                APT apt = APT.factory();
//                Package[] pkgs = apt.getPackages();
//                String format = "%-50s %10s %10s %s";
//                System.out.println(String.format(format, i18n.string("apt.list.package"), i18n.string("apt.list.installed"), i18n.string("apt.list.available"), ""));
//                ArrayList<Package> out = new ArrayList<Package>();
//                String term = cli.getString("search").toLowerCase();
//                for (Package p : pkgs) {
//                    if (cli.isSet("section")) {
//                        if (p.get("Section") == null) { continue; }
//                        if (!p.get("Section").equals(cli.getString("section"))) { continue; }
//                    }
//                    if (cli.isSet("family")) {
//                        if (p.get("Family") == null) { continue; }
//                        if (!p.get("Family").equals(cli.getString("family"))) { continue; }
//                    }
//                    if (cli.isSet("group")) {
//                        if (p.get("Group") == null) { continue; }
//                        if (!p.get("Group").equals(cli.getString("group"))) { continue; }
//                    }
//                    if (cli.isSet("subgroup")) {
//                        if (p.get("Subgroup") == null) { continue; }
//                        if (!p.get("Subgroup").equals(cli.getString("subgroup"))) { continue; }
//                    }
//
//                    String name = p.getName();
//                    Package instPack = apt.getInstalledPackage(p.getName());
//                    Version avail = p.getVersion();
//                    Version inst = null;
//                    String msg = "";
//                    if (instPack != null) {
//                        inst = instPack.getVersion();
//                        if (avail.compareTo(inst) > 0) {
//                            msg = i18n.string("apt.list.update");
//                        }
//                    }
//                    String comp = p.getName() + " " + p.getDescription();
//                    if (comp.toLowerCase().contains(term)) {
//                        System.out.println(String.format(format, name, inst == null ? "" : inst.toString(), avail.toString(), msg));
//                        System.out.println("  " + p.getDescriptionLineOne());
//                    }
//
//                }
//            } catch (Exception ex) { error(ex); }
//            doExit = true;
//        }

        if (doExit) {
            System.exit(0);
        }













        // From here on in we create a new context, load a sketch into it if needed, then start our GUI, whatever that may be.

        // Our first task is to load all the settings.


        if (cli.isSet("cli")) {
            bootContext.warning("Warning: --cli is deprecated. Use --gui=cli instead");
            cli.set("gui", new String[] {"cli"});
        }

        if (cli.isSet("headless")) {
            bootContext.warning("Warning: --headless is deprecated. Use --gui=none instead");
            cli.set("gui", new String[] {"none"});
        }

        if (!cli.isSet("gui")) {
            cli.set("gui", new String[] {"swing"});
        }

        if (GraphicsEnvironment.isHeadless()) {
            if (cli.getString("gui")[0].equals("swing")) {
                cli.set("gui", new String[] {"none"});
            }
        }

        gui = cli.getString("gui")[0];

        switch (gui) {
            case "cli": CliGui.init(); break;
            case "swing": SwingGui.init(); break;
            case "action": ActionGui.init(); break;
            case "none": NoneGui.init(); break;
            case "html": HTMLGui.init(); break;
            default:
                bootContext.error("Unknown GUI specified. Cannot continue.");
                bootContext.action("CloseSession");
        }

        systemContext = createContext(null, gui, false);
        systemContext.setSystemContext(true);

        systemContext.getGui().openSplash();
        systemContext.getGui().splashMessage("Loading UECIDE...", 10);
        systemContext.getGui().splashMessage(i18n.string("splash.msg.packagemanager"), 15);
        initPackageManager();
        systemContext.getGui().splashMessage(i18n.string("splash.msg.application"), 20);
        platform.init(this);

        Thread t = new Thread() {
            public void run() {
                Serial.updatePortList();
                Serial.fillExtraPorts();
            }
        };
        t.start();

        systemContext.getGui().splashMessage(i18n.string("splash.msg.assets"), 40);
        loadAssets();
        Preferences.buildPreferencesTree();

  //      ctx.runInitScripts();
        initMRU();
        systemContext.getGui().splashMessage(i18n.string("splash.msg.complete"), 100);

        ServiceManager.addService(new UsbDiscoveryService());
        ServiceManager.addService(new NetworkDiscoveryService());
//        ServiceManager.addService(new TreeUpdaterService());        
        ServiceManager.addService(new PortListUpdaterService());

        switch (gui) {
            case "cli": CliGui.endinit(); break;
            case "swing": SwingGui.endinit(); break;
            case "action": ActionGui.endinit(); break;
            case "none": NoneGui.endinit(); break;
            case "html": HTMLGui.endinit(); break;
            default:
                bootContext.error("Unknown GUI specified. Cannot continue.");
                bootContext.action("CloseSession");
        }
        

        systemContext.getGui().closeSplash();

        ArrayList<ActionSpec> startupActions = new ArrayList<ActionSpec>();

        if (cli.isSet("last-sketch")) {
            if (MRUList.size() > 0) {
                startupActions.add(new ActionSpec("OpenSketch", MRUList.get(0)));
            }
        }

        for (String arg : argv) { startupActions.add(new ActionSpec("OpenSketch", new File(arg))); }

        for (ActionSpec action : startupActions) {
            systemContext.action(action.getAction(), action.getArgs());
        }

        if (sessions.size() == 0) { // Only a system context, no GUI has been opened
            createContext(null);
        }

        startupActions.clear();

        if (cli.isSet("set")) { 
            for (String pref : cli.getString("set")) {
                String[] prefset = pref.split("=");
                startupActions.add(new ActionSpec("SetPref", prefset[0], prefset[1]));
            }
        }

        if (cli.isSet("board")) { startupActions.add(new ActionSpec("SetBoard", cli.getString("board")[0])); }
        if (cli.isSet("core")) { startupActions.add(new ActionSpec("SetCore", cli.getString("core")[0])); }
        if (cli.isSet("compiler")) { startupActions.add(new ActionSpec("SetCompiler", cli.getString("compiler")[0])); }
        if (cli.isSet("programmer")) { startupActions.add(new ActionSpec("SetProgrammer", cli.getString("programmer")[0])); }
        if (cli.isSet("port")) { startupActions.add(new ActionSpec("SetDevice", cli.getString("port")[0])); }

        if (cli.isSet("clean")) { startupActions.add(new ActionSpec("Purge")); }
        if (cli.isSet("purge")) { startupActions.add(new ActionSpec("Purge")); }
        if (cli.isSet("compile")) { startupActions.add(new ActionSpec("Build")); }
        if (cli.isSet("upload")) { startupActions.add(new ActionSpec("BuildAndUpload")); }

        if (cli.isSet("action")) {
            for (String action : cli.getString("action")) {
                String[] bits = action.split("::");
                String actionName = bits[0];
                if (bits.length > 1) {
                    String[] actionParameters = new String[bits.length-1];
                    for (int i = 0; i < bits.length-1; i++) {
                        actionParameters[i] = bits[i+1];
                    }
                    startupActions.add(new ActionSpec(actionName, (Object[])actionParameters));
                } else {
                    startupActions.add(new ActionSpec(actionName));
                }
            }
        }

        if (systemContext.getGui().isEphemeral()) {
            startupActions.add(new ActionSpec("CloseSession"));
        }

        for (Context session : sessions) {
            if (session.isSystemContext()) continue;
            for (ActionSpec action : startupActions) {
                session.action(action.getAction(), action.getArgs());
            }
        }
    }

    static protected void initPlatform() {
        try {

            if(UECIDE.isMacOS()) {
                platform = new org.uecide.macosx.Platform();
            } else if(UECIDE.isWindows()) {
                platform = new org.uecide.windows.Platform();
            } else if(UECIDE.isUnix()) {
                platform = new org.uecide.unix.Platform();
            }

        } catch(Exception e) {
            Debug.exception(e);
            error("An unknown error occurred while trying to load platform-specific code for your machine.");
        }
    }

    /* Initialize the internal MRU list from the preferences set */
    public static void initMRU() {
        MRUList = new ArrayList<File>();
        MCUList = new HashMap<File,Integer>();

        for(int i = 0; i < 10; i++) {
            if(Preferences.get("sketch.mru." + i) != null) {
                File f = new File(Preferences.get("sketch.mru." + i));

                if(f.exists()) {
                    if(MRUList.indexOf(f) == -1) {
                        MRUList.add(f);
                    }
                }
            }
        }

        for(int i = 0; i < 10; i++) {
            if(Preferences.get("sketch.mcu." + i) != null) {
                String[] mcuEntry = Preferences.getArray("sketch.mcu." + i);

                int hits = 0;
                try {
                    hits = Integer.parseInt(mcuEntry[1]);
                } catch (Exception e) {
                    Debug.exception(e);
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

        try {
            f = new File(f.getCanonicalPath());
        } catch (Exception err) {
            Debug.exception(err);
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
            Debug.exception(e);
        }
        hits++;
        MCUList.put(f, hits);

        for(int i = 0; i < 10; i++) {
            if(i < MRUList.size()) {
                Preferences.set("sketch.mru." + i, MRUList.get(i).getAbsolutePath());
            } else {
                Preferences.unset("sketch.mru." + i);
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
            Preferences.set("sketch.mcu." + z, i.getKey().getAbsolutePath() + "::" + i.getValue());
            z++;
        }
    }

    /*! Get the default board to use if no current board can be found */
/*
    public static Board getDefaultBoard() {
        Board tb;
        String prefsBoard = Preferences.get("board");
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
*/

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

    /*! Load all the libraries in the system */
    public static void gatherLibraries() {
        Library.loadLibraries();
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
            Debug.exception(e);
            error(e);
        }

        return null;
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
        String sbPath = Preferences.get("locations.sketchbook");
        if (sbPath == null) {
            Preferences.setFile("locations.sketchbook", platform.getDefaultSketchbookFolder());
            return platform.getDefaultSketchbookFolder();
        }
        return new File(Preferences.get("locations.sketchbook"));
    }

    protected File getDefaultSketchbookFolder() {
        File sketchbookFolder = null;

        try {
            sketchbookFolder = platform.getDefaultSketchbookFolder();
        } catch(Exception e) { 
            Debug.exception(e);
            return null; 
        }

        // create the folder if it doesn't exist already
        if(!sketchbookFolder.exists()) {
            if (!sketchbookFolder.mkdirs()) { error("Unable to make sketchbook folder " + sketchbookFolder.getAbsolutePath()); }
        }

        return sketchbookFolder;
    }

    public static File getContentFile(String name) {
        String path = System.getProperty("user.dir");

        // Get a path to somewhere inside the .app folder
        if(UECIDE.isMacOS()) {
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
                UECIDE.tryDelete(dead);
            } else {
                removeDir(dead);
            }
        }
    }

    public static File getTmpDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    static public File getDataFolder() {
        File out = null;
        if (overrideSettingsFolder != null) {
            out = new File(overrideSettingsFolder);
        } else {
            if (Preferences.getFile("locations.data") != null) {
                out = Preferences.getFile("locations.data");
                if (out.getName().equals("")) {
                    System.err.println("Warning: invalid data location detected in preferences file.");
                    System.err.println("         Ignoring and using system default instead.");
                    out = platform.getSettingsFolder();
                    Preferences.unset("locations.data");
                }
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
    static public File[] getCompilersFolders() { return getAnyFolders("compilers"); }
    static public File[] getLibrariesFolders() { return getAnyFolders("libraries"); }
    static public File[] getIconsFolders() { return getAnyFolders("icons"); }

    public static void error(String e) {
        System.err.println(e);
        Debug.message(e);
    }

    public static void error(Throwable e) {

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
            Debug.exception(ee);
        }

        e.printStackTrace();
    }

    // This handy little function will rebuild the whole of the internals of
    // UECIDE - that is, all the boards, cores, compilers and libraries etc.
    public static void cleanAndScanAllSettings() throws IOException {
        cacheSystemFiles();
        Serial.updatePortList();
        Serial.fillExtraPorts();
        rescanCompilers();
        rescanCores();
        rescanBoards();
        rescanProgrammers();
        rescanTools();
        rescanLibraries();
        Preferences.buildPreferencesTree();
    }

    public static void rescanCompilers() {
        Compiler.load();
    }

    public static void rescanTools() {
        Tool.load();
    }

    public static void rescanCores() {
        Core.load();
    }

    public static void rescanProgrammers() {
        Programmer.load();
    }

    public static void rescanBoards() {
        Board.load();
    }

    public static void rescanLibraries() {
        gatherLibraries();
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
            Debug.exception(e);
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

            APT reqapt = APT.factory(systemContext);
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
            Debug.exception(e);
            error(e);
        }

    }

    public static boolean isQuiet() {
        return cli.isSet("quiet");
    }

    public static Locale getLocale() {
        if (cli != null) {
            if (cli.isSet("locale")) {
                String[] bits = cli.getString("locale")[0].split("_");
                if (bits.length == 2) {
                    return new Locale(bits[0], bits[1]);
                }
            }
        }
        return Locale.getDefault();
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
            Debug.exception(e);
        }

        // If it deleted then return
        if (!file.exists()) return;
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

    public static void loadAssets() throws IOException {
        cacheSystemFiles();
        Core.load();
        Compiler.load();
        Board.load();
        Programmer.load();
        Tool.load();
        gatherLibraries();
    }

    public static boolean isHeadless() {
        return headless;
    }

    public void setDisplayScaling() {
        int scale = Preferences.getInteger("theme.scale");
        Properties props = System.getProperties();
        props.setProperty("sun.java2d.uiScale", String.format("%d", scale));
    }

    public static Context createContext(File sf) {
        String gui = cli.getString("gui")[0];
        return createContext(sf, gui, true);
    }

    public static Context createContext(File sf, String gui) {
        return createContext(sf, gui, true);
    }

    public static Context createContext(File sf, String gui, boolean startSession) {
        try {

            Gui guiObject = null;
            Context ctx = new Context();
            switch (gui) {
                case "cli": guiObject = new CliGui(ctx); break;
                case "swing": guiObject = new SwingGui(ctx); break;
                case "action": guiObject = new ActionGui(ctx); break;
                case "none": guiObject = new NoneGui(ctx); break;
                case "html": guiObject = new HTMLGui(ctx); break;
                default:
                    System.err.println("Unknown GUI specified. Cannot continue.");
                    System.exit(10);
            }

            ctx.setGui(guiObject);

            if (startSession) {
                sessions.add(ctx);
                ctx.action("openSketch", sf);
                if (presetPort != null) { ctx.action("SetDevice", presetPort); }
                if (presetBoard != null) { ctx.action("SetBoard", presetBoard); }
                if (presetCore != null) { ctx.action("SetCore", presetCore); }
                if (presetCompiler != null) { ctx.action("SetCompiler", presetCompiler); }
                if (presetProgrammer != null) { ctx.action("SetProgrammer", presetProgrammer); }
                if (purgeCache) { ctx.action("Purge"); }
                if (cleanBuild) { ctx.action("Purge"); }
                if (autoProgram) { ctx.action("Upload", ctx.getSketch().getName()); }

                guiObject.open();
                if (guiObject.shouldAutoOpen()) {
                    ctx.action("openSketchFile", ctx.getSketch().getMainFile());
                }
            }
            return ctx;
        } catch (Exception e) {
            Debug.exception(e);
            error(e);
        }
        return null;
    }

    public static void cleanupSession(Context ctx) {
        ctx.dispose();
        sessions.remove(ctx);
        if (sessions.size() == 0) {
            System.err.println("Last session exited - quitting");
            System.exit(0);
        }
    }
}

