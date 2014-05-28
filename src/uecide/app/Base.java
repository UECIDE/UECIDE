/*
 * Copyright (c) 2014, Majenko Technologies
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

package uecide.app;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import uecide.plugin.*;

import java.lang.reflect.Method;



import javax.swing.*;
import javax.imageio.*;

import uecide.app.Compiler;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jtattoo.plaf.*;

/**
 * The base class for the main uecide.application.
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading
 * files and images, etc) that comes from that.
 */
public class Base {
	
    public static int REVISION = 23;
    /** This might be replaced by main() if there's a lib/version.txt file. */
    /** Set true if this a proper release rather than a numbered revision. */
    public static boolean RELEASE = false;
    public static int BUILDNO = 0;
    public static String BUILDER = "";

    public static String overrideSettingsFolder = null;

    public static ArrayList<Process> processes = new ArrayList<Process>();
  
    static Platform platform;

    static private boolean headless;

    // A single instance of the preferences window
    static Preferences preferencesFrame;

    // these are static because they're used by Sketch
    static private File examplesFolder;
    static private File librariesFolder;
    static private File toolsFolder;
    static private File hardwareFolder;
    public static ArrayList<File> MRUList;

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
//    public static TreeMap<String, Plugin> plugins;
    public static TreeMap<String, Class<?>> plugins = new TreeMap<String, Class<?>>();
    public static ArrayList<Plugin> pluginInstances;
    static Splash splashScreen;

    public static TreeMap<String, String>libraryCategoryNames;
    public static TreeMap<String, File>libraryCategoryPaths;

    // Location for untitled items
    static File untitledFolder;

    public static PropertyFile preferences;
    public static Theme theme;

    public static Board getBoard(String name) { return boards.get(name); }
    public static Core getCore(String name) { return cores.get(name); }
    public static Compiler getCompiler(String name) { return compilers.get(name); }

    public static void main(String args[]) {
        new Base(args);
    }

    public static File getJarLocation() {
        try {
            return new File(Base.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            Base.error(e);
        }
        return new File("/");
    }

    public Base(String[] args) {

        boolean redirectExceptions = true;

        headless = false;
        boolean loadLastSketch = false;

        for (int i = 0; i < args.length; i++) {
            String path = args[i];
            if (path.equals("--verbose")) {
                Debug.setVerbose(true);
            }
            if (path.equals("--debug")) {
                Debug.show();
                break;
            }
            if (path.equals("--exceptions")) {
                redirectExceptions = false;
            }
            if (path.equals("--headless")) {
                headless = true;
            }
            if (path.startsWith("--datadir=")) {
                overrideSettingsFolder = path.substring(10);
            }
            if (path.equals("--last-sketch")) {
                loadLastSketch = true;
            }
        }

        if (redirectExceptions) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    Base.broken(t, e);
                }
            });
        }



        if (isUnix()) {
            if ((System.getenv("DISPLAY") == null) || (System.getenv("DISPLAY").equals(""))) {
                headless = true;
            }
        }

        try {
            File f = getJarLocation();

            Debug.message("Getting version information from " + f.getAbsolutePath());
            JarFile myself = new JarFile(f);
            Manifest manifest = myself.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();

            systemVersion = new Version(manifestContents.getValue("Version"));
            REVISION = Integer.parseInt(manifestContents.getValue("Compiled"));
            BUILDNO = Integer.parseInt(manifestContents.getValue("Build"));
            BUILDER = manifestContents.getValue("Built-By");

            RELEASE = true;

            Debug.message("Version: "+ systemVersion);
        } catch (Exception e) {
            error(e);
        }

        // Get the initial basic theme data
        theme = new Theme("/uecide/app/config/theme.txt");
        theme.setPlatformAutoOverride(true);

        System.err.println("Loading " + theme.get("product") + "...");

        initPlatform();



        preferences = new PropertyFile(getSettingsFile("preferences.txt"), "/uecide/app/config/preferences.txt");
        preferences.setPlatformAutoOverride(true);

        platform.setSettingsFolderEnvironmentVariable();

        Debug.setLocation(new Point(preferences.getInteger("debug.window.x"), preferences.getInteger("debug.window.y")));
        Debug.setSize(new Dimension(preferences.getInteger("debug.window.width"), preferences.getInteger("debug.window.height")));

        ArrayList<String> bundledPlugins = getResourcesFromJarFile(getJarLocation(), "uecide/app/bundles/plugins/", ".jar");
        File upf = getUserPluginsFolder();
        for (String s : bundledPlugins) {
            String fn = s.substring(s.lastIndexOf("/") + 1);
            File dest = new File(upf, fn);
            if (!dest.exists()) {
                System.err.println("Installing " + fn);
                copyResourceToFile("/" + s, dest);
                continue;
            } 

            try {
                JarFile jf = new JarFile(dest);
                Manifest manifest = jf.getManifest();
                Attributes manifestContents = manifest.getMainAttributes();
                Version oldVersion = new Version(manifestContents.getValue("Version"));
                String bv = getBundleVersion("/" + s);
                Version newVersion = new Version(bv);


                if (newVersion.compareTo(oldVersion) > 0) {
                    System.err.println("Upgrading your version of " + fn + " to " + bv);
                    copyResourceToFile("/" + s, dest);
                }
            } catch (Exception e) {
                error(e);
            }
        }

        // Now we reload the theme data with user overrides
        // (we didn't know where they were before) 
        theme = new Theme(getSettingsFile("theme.txt"), "/uecide/app/config/theme.txt");
        theme.setPlatformAutoOverride(true);

        if (!headless) {
            splashScreen = new Splash();
            splashScreen.setMessage("Loading " + theme.get("product.cap") + "...", 10);
        }

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        try {
            if (!headless) {
                if (isUnix()) {
                    Toolkit xToolkit = Toolkit.getDefaultToolkit();
                    java.lang.reflect.Field awtAppClassNameField =
                        xToolkit.getClass().getDeclaredField("awtAppClassName");
                    awtAppClassNameField.setAccessible(true);
                    awtAppClassNameField.set(xToolkit, Base.theme.get("product.cap"));
                }

                String laf = Base.preferences.get("editor.laf");
                if (laf == null) {
                    laf = Base.preferences.getPlatformSpecific("editor.laf.default");
                    if (laf != null) {
                        Base.preferences.set("editor.laf", laf);
                    }
                }
                if (laf != null) {
                    if (laf.startsWith("com.jtattoo.plaf.")) {
                        Properties props = new Properties();
                        props.put("windowDecoration", Base.preferences.getBoolean("editor.laf.decorator") ? "off" : "on");
                        props.put("logoString", "UECIDE");
                        props.put("textAntiAliasing", "on");

                        Class<?> cls = Class.forName(laf);
                        Class[] cArg = new Class[1];
                        cArg[0] = Properties.class;
                        Method mth = cls.getMethod("setCurrentTheme", cArg);
                        mth.invoke(cls, props);
                    }
                    UIManager.setLookAndFeel(laf);
                }
            }

        } catch (Exception e) {
            error(e);
        }

        // Create a location for untitled sketches
        untitledFolder = createTempFolder("untitled");
        untitledFolder.deleteOnExit();

        if (!headless) splashScreen.setMessage("Loading Application...", 20);
        platform.init(this);

        // Get paths for the libraries and examples in the Processing folder
        //String workingDirectory = System.getProperty("user.dir");

        examplesFolder = getContentFile("examples");
        toolsFolder = getContentFile("tools");

        // Get the sketchbook path, and make sure it's set properly
        String sketchbookPath = preferences.get("sketchbook.path");

//        Translate.load("swedish");

        // If no path is set, get the default sketchbook folder for this platform
        if (sketchbookPath == null) {
            File defaultFolder = getDefaultSketchbookFolder();
            preferences.set("sketchbook.path", defaultFolder.getAbsolutePath());
            sketchbookPath = defaultFolder.getAbsolutePath();
        }

        File sketchbookFolder = new File(sketchbookPath);
        if (!sketchbookFolder.exists()) {
            sketchbookFolder.mkdirs();
        }
    
        compilers = new TreeMap<String, Compiler>();
        cores = new TreeMap<String, Core>();
        boards = new TreeMap<String, Board>();
        plugins = new TreeMap<String, Class<?>>();
        pluginInstances = new ArrayList<Plugin>();

        Serial.updatePortList();
        Serial.fillExtraPorts();

        if (!headless) splashScreen.setMessage("Loading Compilers...", 30);
        loadCompilers();
        if (!headless) splashScreen.setMessage("Loading Cores...", 40);
        loadCores();
        if (!headless) splashScreen.setMessage("Loading Boards...", 50);
        loadBoards();
        if (!headless) splashScreen.setMessage("Loading Plugins...", 60);
        loadPlugins();
        if (!headless) splashScreen.setMessage("Loading Libraries...", 70);
        gatherLibraries();
        initMRU();

        if (headless) {
            error("Unable to open editor window - no graphics environment found.");
            System.exit(10);
        }

        if (!headless) splashScreen.setMessage("Opening Editor...", 80);
        boolean opened = false;
        // Check if any files were passed in on the command line
        for (int i = 0; i < args.length; i++) {
            String path = args[i];
            if (path.startsWith("--")) {
                continue;
            }
            // Fix a problem with systems that use a non-ASCII languages. Paths are
            // being passed in with 8.3 syntax, which makes the sketch loader code
            // unhappy, since the sketch folder naming doesn't match up correctly.
            // http://dev.processing.org/bugs/show_bug.cgi?id=1089
            if (isWindows()) {
                try {
                    File file = new File(args[i]);
                    path = file.getCanonicalPath();
                } catch (IOException e) {
                    error(e);
                }
            }
            if (createNewEditor(path) != null) {
                opened = true;
            }
        }

        if (loadLastSketch) {
            File lastFile = MRUList.get(0);
            if (lastFile != null) {
                if (createNewEditor(lastFile.getAbsolutePath()) != null) {
                    opened = true;
                }
            }
        }

        // Create a new empty window (will be replaced with any files to be opened)
        if (!opened) {
            handleNew();
        }
        if (!headless) {
            splashScreen.setMessage("Complete", 100);
            splashScreen.dispose();
            if (boards.size() == 0) {
                System.err.println(plugins.keySet());
                showWarning(Translate.t("No boards installed"), Translate.w("You have no boards installed.  I will now open the plugin manager so you can install the boards, cores and compilers you need to use %1.", 40, "\n", theme.get("product.cap")), null);
                Editor.editorList.get(0).launchPlugin(plugins.get("uecide.plugin.PluginManager"));
            } else if (cores.size() == 0) {
                showWarning(Translate.t("No cores installed"), Translate.w("You have no cores installed.  I will now open the plugin manager so you can install the boards, cores and compilers you need to use %1.", 40, "\n", theme.get("product.cap")), null);
                Editor.editorList.get(0).launchPlugin(plugins.get("uecide.plugin.PluginManager"));
            } else if (compilers.size() == 0) {
                showWarning(Translate.t("No compilers installed"), Translate.w("You have no compilers installed.  I will now open the plugin manager so you can install the boards, cores and compilers you need to use %1.", 40, "\n", theme.get("product.cap")), null);
                Editor.editorList.get(0).launchPlugin(plugins.get("uecide.plugin.PluginManager"));
            } 
        }
            
    }

    static protected void initPlatform() {
        try {
            Class<?> platformClass = Class.forName("uecide.app.Platform");
            if (Base.isMacOS()) {
                platformClass = Class.forName("uecide.app.macosx.Platform");
            } else if (Base.isWindows()) {
                platformClass = Class.forName("uecide.app.windows.Platform");
            } else if (Base.isUnix()) {
                platformClass = Class.forName("uecide.app.unix.Platform");
            }
            platform = (Platform) platformClass.newInstance();
        } catch (Exception e) {
            Base.showError("Problem Setting the Platform",
                            "An unknown error occurred while trying to load\n" +
                            "platform-specific code for your machine.", e);
        }
    }


    static protected void initRequirements() {
        try {
            Class.forName("com.sun.jdi.VirtualMachine");
        } catch (ClassNotFoundException cnfe) {
            Base.showPlatforms();
            Base.showError("Please install JDK 1.5 or later",
                            theme.get("product.cap") + " requires a full JDK (not just a JRE)\n" +
                            "to run. Please install JDK 1.5 or later.\n" +
                            "More information can be found in the reference.", cnfe);
        }
    }


    public static void initMRU()
    {
        MRUList = new ArrayList<File>();
        for (int i = 0; i < 10; i++) {
            if (preferences.get("sketch.mru." + i) != null) {
                File f = new File(preferences.get("sketch.mru." + i));
                if (f.exists()) {
                    if (MRUList.indexOf(f) == -1) {
                        MRUList.add(f);
                    }
                }
            }
        }
    }

    public static void updateMRU(File f)
    {
        if (!f.isDirectory()) {
            f = f.getParentFile();
        }
        MRUList.remove(f);
        MRUList.add(0,f);
        while (MRUList.size() > 10) {
            MRUList.remove(10);
        }
        for (int i = 0; i < 10; i++) {
            if (i < MRUList.size()) {
                preferences.set("sketch.mru." + i, MRUList.get(i).getAbsolutePath());
            } else {
                preferences.unset("sketch.mru." + i);
            }
        }
        preferences.save();
    }

    public static Board getDefaultBoard() {
        Board tb;
        String prefsBoard = preferences.get("board");
        String[] entries;

        if (boards.size() == 0) {
            return null;
        }

        tb = boards.get(prefsBoard);
        if (tb != null) {
            return tb;
        }

        entries = (String[]) boards.keySet().toArray(new String[0]);
        tb = boards.get(entries[0]);
        if (tb != null) {
            return tb;
        }
        return null;
    }

    public static void loadCompilers() {
        compilers.clear();
        loadCompilersFromFolder(getSystemCompilersFolder());
        if (getUserCompilersFolder() != getSystemCompilersFolder()) {
            loadCompilersFromFolder(getUserCompilersFolder());
        }
    }

    public static void loadCompilersFromFolder(File folder) {
        if (folder == null) {
            return;
        }
        if (!folder.exists()) {
            return;
        }
        if (!folder.isDirectory()) 
            return;
        String cl[] = folder.list();

        Debug.message("Loading compilers from " + folder.getAbsolutePath());
 
        for (int i = 0; i < cl.length; i++) {
            if (cl[i].charAt(0) == '.')
                continue;
            File cdir = new File(folder, cl[i]);
            if (cdir.isDirectory()) {
                File cfile = new File(cdir, "compiler.txt");
                if (cfile.exists()) {
                    Debug.message("    Loading core " + cfile.getAbsolutePath());
                    Compiler newCompiler = new Compiler(cdir);
                    if (newCompiler.isValid()) {
                        compilers.put(newCompiler.getName(), newCompiler);
                    }
                }
            }
        }
    }


    public static void loadCores() {
        cores.clear();
        loadCoresFromFolder(getSystemCoresFolder());
        if (getUserCoresFolder() != getSystemCoresFolder()) {
            loadCoresFromFolder(getUserCoresFolder());
        }
    }

    public static void loadCoresFromFolder(File folder) {
        if (!folder.isDirectory()) 
            return;
        String cl[] = folder.list();
        Debug.message("Loading cores from " + folder.getAbsolutePath());
 
        for (int i = 0; i < cl.length; i++) {
            if (cl[i].charAt(0) == '.')
                continue;
            File cdir = new File(folder, cl[i]);
            if (cdir.isDirectory()) {
                File cfile = new File(cdir, "core.txt");
                if (cfile.exists()) {
                    Debug.message("    Loading core " + cfile.getAbsolutePath());
                    Core newCore = new Core(cdir);
                    if (newCore.isValid()) {
                        cores.put(newCore.getName(), newCore);
                    }
                }
            }
        }
    }


    public static void loadBoards() {
        boards.clear();
        loadBoardsFromFolder(getSystemBoardsFolder());
        if (getUserBoardsFolder() != getSystemBoardsFolder()) {
            loadBoardsFromFolder(getUserBoardsFolder());
        }
    }

    public static void loadBoardsFromFolder(File folder) {
        String bl[] = folder.list();

        if (bl == null) {
            return;
        }

        Debug.message("Loading boards from " + folder.getAbsolutePath());

        for (int i = 0; i < bl.length; i++) {
            if (bl[i].charAt(0) == '.')
                continue;
            File bdir = new File(folder, bl[i]);
            if (bdir.isDirectory()) {
                File bfile = new File(bdir, "board.txt");
                if (bfile.exists()) {
                    Debug.message("    Loading board " + bfile.getAbsolutePath());
                    Board newBoard = new Board(bdir);
                    if (newBoard.isValid()) {
                        boards.put(newBoard.getName(), newBoard);
                    } 
                } else {
                    loadBoardsFromFolder(bdir);
                }
            }
        }
    }

    boolean breakTime = false;
    String[] months = {
        "jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dec"
    };

    /**
     * Create a new untitled document in a new sketch window.
     */
    public static void handleNew() {
        createNewEditor(null);
    }


    /**
     * Prompt for a sketch to open, and open it in a new window.
     */

    public static boolean isSketchFolder(File folder) {
        if (folder.isDirectory()) {
            File testFile = new File(folder, folder.getName() + ".ino");
            if (testFile.exists()) {
                return true;
            }
            testFile = new File(folder, folder.getName() + ".pde");
            if (testFile.exists()) {
                return true;
            }
        }
        return false;
    }

    public static boolean pathContainsSketchSomewhere(File root) {
        if (!root.isDirectory()) {
            return false;
        }
        if (isSketchFolder(root)) {
            return true;
        }
        File[] files = root.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                if (pathContainsSketchSomewhere(f)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Open a sketch in a new window.
     * @param path Path to the pde file for the sketch in question
     * @return the Editor object, so that properties (like 'untitled')
     *         can be set by the caller
     */

    public static Editor createNewEditor(String path) {
        Sketch s;

        if (path == null) {
            s = new Sketch((File)null);
        } else {
            s = new Sketch(path);
        }
            
        Editor editor = new Editor(s);
        editor.setVisible(true);
        if (path != null) {
            updateMRU(new File(path));
        }
        return editor;
    }

// .................................................................


    /**
    * Asynchronous version of menu rebuild to be used on save and rename
    * to prevent the interface from locking up until the menus are done.
    */

    public static ArrayList<String> getLibraryCollectionNames() {
        ArrayList<String> out = new ArrayList<String>();
        for (String k : libraryCollections.keySet()) {
            out.add(k);
        }
        return out;
    }

    public static TreeMap<String, Library> getLibraryCollection(String name, String corename) {
        TreeMap<String, Library>out = new TreeMap<String, Library>();
        TreeMap<String, Library>coll = libraryCollections.get(name);
        if (coll == null) {
            return out;
        }
        for (String k : coll.keySet()) {
            Library v = coll.get(k);
            if (v.worksWith(corename)) {
                out.put(k, v);
            }
        }
        return out;
        //return libraryCollections.get(name);
    }

    public static TreeMap<String, TreeMap<String, Library>> libraryCollections;

    public static void gatherLibraries() {
        libraryCategoryNames = new TreeMap<String, String>();
        libraryCategoryPaths = new TreeMap<String, File>();

        for (String k : preferences.childKeysOf("library")) {
            String cName = preferences.get("library." + k + ".name");
            String cPath = preferences.get("library." + k + ".path");
            if (cName != null && cPath != null) {
                File f = new File(cPath);
                if (f.exists() && f.isDirectory()) {    
                    libraryCategoryNames.put(k, cName);
                    libraryCategoryPaths.put(k, f);
                }
            }
        }

        // No library locations defined at the moment - let's define
        // a default one that is like the old Arduino one.

        if(libraryCategoryPaths.size() == 0) {
            File cdir = new File(getSketchbookFolder(), "libraries");
            libraryCategoryPaths.put("contributed", cdir);
            libraryCategoryNames.put("contributed", "Contributed");
            preferences.set("library.contributed.name", "Contributed");
            preferences.setFile("library.contributed.path", cdir);
        }

        libraryCollections = new TreeMap<String, TreeMap<String, Library>>();

        String[] corelist = (String[]) cores.keySet().toArray(new String[0]);

        for (String core : corelist) {
            libraryCollections.put("core:" + core, loadLibrariesFromFolder(cores.get(core).getLibrariesFolder(), "core", core)); // Core libraries
        }

        for (String key : libraryCategoryPaths.keySet()) {
            libraryCollections.put("cat:" + key, loadLibrariesFromFolder(libraryCategoryPaths.get(key),"contributed"));
        }
    }

    public static TreeMap<String, Library> loadLibrariesFromFolder(File folder, String type) {
        return loadLibrariesFromFolder(folder, type, "all");
    }

    public static TreeMap<String, Library> loadLibrariesFromFolder(File folder, String type, String cr) {
        TreeMap<String, Library> theseLibraries = new TreeMap<String, Library>();
        if (!folder.exists()) {
            return theseLibraries;
        }
        File[] list = folder.listFiles();
        Debug.message("Loading libraries from " + folder.getAbsolutePath());
        for (File f : list) {
            if (f.isDirectory()) {
                if (cr.equals("all")) {
                    boolean sub = false;
                    for (String c : cores.keySet()) {
                        if (f.getName().equals(c)) {
                            Debug.message("  Found sub-library core group " + f);
                            theseLibraries.putAll(loadLibrariesFromFolder(f, type, c));
                            sub = true;
                            break;
                        }
                    }
                    if (sub) continue;
                }
                File files[] = f.listFiles();
                for (File sf : files) {
                    if ((sf.getName().equals(f.getName() + ".h") || (sf.getName().startsWith(f.getName() + "_") && sf.getName().endsWith(".h")))) {
                        Library newLibrary = new Library(sf, type, cr);
                        if (newLibrary.isValid()) {
                            theseLibraries.put(newLibrary.getName(), newLibrary);
                            Debug.message("    Adding new library " + newLibrary.getName() + " from " + f.getAbsolutePath());
                        } else {
                            Debug.message("    Skipping invalid library " + f.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return theseLibraries;
    }

    /**
    * Show the Preferences window.
    */
    public static void handlePrefs() {
        if (preferencesFrame == null) preferencesFrame = new Preferences();
        preferencesFrame.showFrame();
    }


    public static Platform getPlatform() {
        return platform;
    }

    public static String getOSArch() {
        return System.getProperty("os.arch");
    }


    public static String getOSFullName() {
        return getOSName() + "_" + getOSArch();
    }
    
    public static String getOSName() {
        String osname = System.getProperty("os.name");

        if (osname.indexOf("Mac") != -1) {
            return "macosx";
        } else if (osname.indexOf("Windows") != -1) {
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

    // .................................................................

    public static File getSettingsFolder() {
        File settingsFolder = null;

        if (overrideSettingsFolder != null) {
            settingsFolder = new File(overrideSettingsFolder);
            if (!settingsFolder.exists()) {
                settingsFolder.mkdirs();
            }
            return settingsFolder;
        }

        try {
            settingsFolder = platform.getSettingsFolder();
        } catch (Exception e) {
            showError(Translate.t("Problem getting data folder"),
            Translate.t("Error getting the data folder."), e);
            error(e);
            return null;
        }

        if (!settingsFolder.exists()) {
            if (!settingsFolder.mkdirs()) {
                showError(Translate.t("Settings issues"),
                Translate.t("Cannot run because I could not create a folder to store your settings."), null);
                return null;
            }
        }
        return settingsFolder;
    }


    /**
    * Convenience method to get a File object for the specified filename inside
    * the settings folder.
    * For now, only used by Preferences to get the preferences.txt file.
    * @param filename A file inside the settings folder.
    * @return filename wrapped as a File object inside the settings folder
    */
    public static File getSettingsFile(String filename) {
        return new File(getSettingsFolder(), filename);
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
            folder.delete();
            folder.mkdirs();
            return folder;

        } catch (Exception e) {
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

    public static File getSystemLibrariesFolder() {
        return getContentFile("libraries");
    }

    public static String getHardwarePath() {
        return getHardwareFolder().getAbsolutePath();
    }

    public static File getSketchbookFolder() {
        return new File(preferences.get("sketchbook.path"));
    }

    public static File getSketchbookHardwareFolder() {
        return new File(getSketchbookFolder(), "hardware");
    }

    protected File getDefaultSketchbookFolder() {
        File sketchbookFolder = null;
        try {
            sketchbookFolder = platform.getDefaultSketchbookFolder();
        } catch (Exception e) { }

        if (sketchbookFolder == null) {
            sketchbookFolder = promptSketchbookLocation();
        }

        // create the folder if it doesn't exist already
        boolean result = true;
        if (!sketchbookFolder.exists()) {
            result = sketchbookFolder.mkdirs();
        }

        if (!result) {
            showError(Translate.t("You forgot your sketchbook"),
            Translate.t("I cannot run because I could not create a folder to store your sketchbook."), null);
        }

        return sketchbookFolder;
    }


    /**
    * Check for a new sketchbook location.
    */
    static protected File promptSketchbookLocation() {
        File folder = null;

        folder = new File(System.getProperty("user.home"), "sketchbook");
        if (!folder.exists()) {
            folder.mkdirs();
            return folder;
        }

        String prompt = Translate.t("Select (or create new) folder for sketches...");
        folder = Base.selectFolder(prompt, null, null);
        if (folder == null) {
            System.exit(0);
        }
        return folder;
    }


    // .................................................................


    /**
    * Implements the cross-platform headache of opening URLs
    * TODO This code should be replaced by PApplet.link(),
    * however that's not a static method (because it requires
    * an AppletContext when used as an applet), so it's mildly
    * trickier than just removing this method.
    */
    public static void openURL(String url) {
        try {
            platform.openURL(url);

        } catch (Exception e) {
            showWarning(Translate.t("Problem Opening URL"),
            Translate.t("Could not open the URL") + "\n" + url, e);
        }
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

        } catch (Exception e) {
            showWarning(Translate.t("Problem Opening Folder"),
            Translate.t("Could not open the folder") + "\n" + file.getAbsolutePath(), e);
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
        if (Base.isMacOS()) {
            if (frame == null) frame = new Frame(); //.pack();
            FileDialog fd = new FileDialog(frame, prompt, FileDialog.LOAD);
            if (folder != null) {
                fd.setDirectory(folder.getParent());
                //fd.setFile(folder.getName());
            }
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fd.setVisible(true);
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
            if (fd.getFile() == null) {
                return null;
            }
            return new File(fd.getDirectory(), fd.getFile());

        } else {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle(prompt);
            if (folder != null) {
                fc.setSelectedFile(folder);
            }
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int returned = fc.showOpenDialog(new JDialog());
            if (returned == JFileChooser.APPROVE_OPTION) {
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
        } catch (Exception e) {
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


    public static void showReference(String filename) {
        File referenceFolder = Base.getContentFile("reference");
        File referenceFile = new File(referenceFolder, filename);
        openURL(referenceFile.getAbsolutePath());
    }

    public static void showGettingStarted() {
        if (Base.isMacOS()) {
            Base.showReference("Guide_MacOSX.html");
        } else if (Base.isWindows()) {
            Base.showReference("Guide_Windows.html");
        } else {
            Base.openURL("http://www.arduino.cc/playground/Learning/Linux");
        }
    }

    public static void showReference() {
        showReference("index.html");
    }


    public static void showEnvironment() {
        showReference("Guide_Environment.html");
    }


    public static void showPlatforms() {
        showReference("environment" + File.separator + "platforms.html");
    }


    public static void showTroubleshooting() {
        showReference("Guide_Troubleshooting.html");
    }


    public static void showFAQ() {
        showReference("FAQ.html");
    }


    // .................................................................


    /**
    * "No cookie for you" type messages. Nothing fatal or all that
    * much of a bummer, but something to notify the user about.
    */
    public static void showMessage(String title, String message) {
        if (title == null) title = Translate.t("Message");

        if (headless) {
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
        if (title == null) title = Translate.t("Warning");

        if (headless) {
            System.out.println(title + ": " + message);

        } else {
            System.out.println(title + ": " + message);
            JOptionPane.showMessageDialog(new Frame(), message, title,
            JOptionPane.WARNING_MESSAGE);
        }
        if (e != null) error(e);
    }


    /**
    * Show an error message that's actually fatal to the program.
    * This is an error that can't be recovered. Use showWarning()
    * for errors that allow P5 to continue running.
    */
    public static void showError(String title, String message, Throwable e) {
        if (title == null) title = Translate.t("Error");

        if (headless) {
            System.err.println(title + ": " + message);

        } else {
            JOptionPane.showMessageDialog(new Frame(), message, title,
            JOptionPane.ERROR_MESSAGE);
        }
        if (e != null) error(e);
        System.exit(1);
    }


    // ...................................................................

    // incomplete
    public static int showYesNoCancelQuestion(Editor editor, String title, String primary, String secondary) {
        if (!Base.isMacOS()) {
            int result =
                JOptionPane.showConfirmDialog(null, primary + "\n" + secondary, title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            return result;
        } else {
            // Pane formatting adapted from the Quaqua guide
            // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
            JOptionPane pane =
                new JOptionPane("<html> " +
                    "<head> <style type=\"text/css\">"+
                    "b { font: 13pt \"Lucida Grande\" }"+
                    "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                    "</style> </head>" +
                    "<b>" + Translate.t("Do you want to save changes to this sketch before closing?") + "</b>" +
                    "<p>" + Translate.t("If you don't save, your changes will be lost."),
                    JOptionPane.QUESTION_MESSAGE);

            String[] options = new String[] {
                Translate.t("Save"), Translate.t("Cancel"), Translate.t("Don't Save")
            };
            pane.setOptions(options);

            // highlight the safest option ala apple hig
            pane.setInitialValue(options[0]);

            // on macosx, setting the destructive property places this option
            // away from the others at the lefthand side
            pane.putClientProperty("Quaqua.OptionPane.destructiveOption", new Integer(2));

            JDialog dialog = pane.createDialog(editor, null);
            dialog.setVisible(true);

            Object result = pane.getValue();
            if (result == options[0]) {
                return JOptionPane.YES_OPTION;
            } else if (result == options[1]) {
                return JOptionPane.CANCEL_OPTION;
            } else if (result == options[2]) {
                return JOptionPane.NO_OPTION;
            } else {
                return JOptionPane.CLOSED_OPTION;
            }
        }
    }

    public static int showYesNoQuestion(Frame editor, String title, String primary, String secondary) {
        if (!Base.isMacOS()) {
            return JOptionPane.showConfirmDialog(editor,
                "<html><body>" +
                "<b>" + primary + "</b>" +
                "<br>" + secondary, title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        } else {
            // Pane formatting adapted from the Quaqua guide
            // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
            JOptionPane pane =
                new JOptionPane("<html> " +
                    "<head> <style type=\"text/css\">"+
                    "b { font: 13pt \"Lucida Grande\" }"+
                    "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                    "</style> </head>" +
                    "<b>" + primary + "</b>" +
                    "<p>" + secondary + "</p>",
                    JOptionPane.QUESTION_MESSAGE);

            String[] options = new String[] {
                Translate.t("Yes"), Translate.t("No")
            };
            pane.setOptions(options);

            // highlight the safest option ala apple hig
            pane.setInitialValue(options[0]);

            JDialog dialog = pane.createDialog(editor, null);
            dialog.setVisible(true);

            Object result = pane.getValue();
            if (result == options[0]) {
                return JOptionPane.YES_OPTION;
            } else if (result == options[1]) {
                return JOptionPane.NO_OPTION;
            } else {
                return JOptionPane.CLOSED_OPTION;
            }
        }
    }


    public static File getContentFile(String name) {
        String path = System.getProperty("user.dir");

        // Get a path to somewhere inside the .app folder
        if (Base.isMacOS()) {
            //      <key>javaroot</key>
            //      <string>$JAVAROOT</string>
            String javaroot = System.getProperty("javaroot");
            if (javaroot != null) {
                path = javaroot;
            }
        }
        File working = new File(path);
        if (name == null) {
            return working;
        }
        return new File(working, name);
    }


    /**
    * Get an image associated with the current color theme.
    */
    public static Image getThemeImage(String name, Component who) {
        return getLibImage("theme/" + name, who);
    }


    /**
    * Return an Image object from inside the Processing lib folder.
    */
    public static Image getLibImage(String name, Component who) {
        Image image = null;
        Toolkit tk = Toolkit.getDefaultToolkit();

        File imageLocation = new File(getContentFile("lib"), name);
        image = tk.getImage(imageLocation.getAbsolutePath());
        MediaTracker tracker = new MediaTracker(who);
        tracker.addImage(image, 0);
        try {
            tracker.waitForAll();
        } catch (InterruptedException e) { }
        return image;
    }

    public static BufferedImage getLibBufferedImage(String name) {
        try {
            BufferedImage image = ImageIO.read(new File(getContentFile("lib"), name));
            return image;
        } catch (Exception e){
            return null;
        }
    }


    /**
    * Return an InputStream for a file inside the Processing lib folder.
    */
    public static InputStream getLibStream(String filename) {
        try {
            return new FileInputStream(new File(getContentFile("lib"), filename));
        } catch (Exception e) {
            System.err.println("Unable to find " + filename + " in lib");
            return null;
        }
    }


    // ...................................................................


    /**
    * Get the number of lines in a file by counting the number of newline
    * characters inside a String (and adding 1).
    */
    public static int countLines(String what) {
        int count = 1;
        for (char c : what.toCharArray()) {
            if (c == '\n') count++;
        }
        return count;
    }

    public static void copyFile(File sourceFile, File targetFile) {
        try {
            InputStream from =
                new BufferedInputStream(new FileInputStream(sourceFile));
            OutputStream to =
                new BufferedOutputStream(new FileOutputStream(targetFile));
            byte[] buffer = new byte[16 * 1024];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }
            to.flush();
            from.close(); // ??
            from = null;
            to.close(); // ??
            to = null;

            targetFile.setLastModified(sourceFile.lastModified());
        } catch (Exception e) {
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
            targetDir.mkdirs();
            String files[] = sourceDir.list();
            for (int i = 0; i < files.length; i++) {
                // Ignore dot files (.DS_Store), dot folders (.svn) while copying
                if (files[i].charAt(0) == '.') continue;
                //if (files[i].equals(".") || files[i].equals("..")) continue;
                File source = new File(sourceDir, files[i]);
                File target = new File(targetDir, files[i]);
                if (source.isDirectory()) {
                    //target.mkdirs();
                    copyDir(source, target);
                    target.setLastModified(source.lastModified());
                } else {
                    copyFile(source, target);
                }
            }
        } catch (Exception e) {
            error(e);
        }
    }


    /**
    * Remove all files in a directory and the directory itself.
    */
    public static void removeDir(File dir) {
        if (dir.exists()) {
            Debug.message("Deleting folder " + dir.getAbsolutePath());
            removeDescendants(dir);
            if (!dir.delete()) {
                System.err.println(Translate.t("Could not delete %1", dir.getName()));
            }
        }
    }


    /**
    * Recursively remove all files within a directory,
    * used with removeDir(), or when the contents of a dir
    * should be removed, but not the directory itself.
    * (i.e. when cleaning temp files from lib/build)
    */
    public static void removeDescendants(File dir) {
        if (!dir.exists()) return;

        String files[] = dir.list();
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(".") || files[i].equals("..")) continue;
            File dead = new File(dir, files[i]);
            if (!dead.isDirectory()) {
                if (!preferences.getBoolean("compiler.save_build_files")) {
                    if (!dead.delete()) {
                        // temporarily disabled
                        System.err.println(Translate.t("Could not delete %1", dead.getName()));
                    }
                }
            } else {
                removeDir(dead);
                //dead.delete();
            }
        }
    }

    /**
     * Calculate the size of the contents of a folder.
     * Used to determine whether sketches are empty or not.
     * Note that the function calls itself recursively.
     */
    public static int calcFolderSize(File folder) {
        int size = 0;

        String files[] = folder.list();
        // null if folder doesn't exist, happens when deleting sketch
        if (files == null) return -1;

        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(".") || (files[i].equals("..")) ||
                files[i].equals(".DS_Store")) continue;
                File fella = new File(folder, files[i]);
            if (fella.isDirectory()) {
                size += calcFolderSize(fella);
            } else {
                size += (int) fella.length();
            }
        }
        return size;
    }

    /**
     * Recursively creates a list of all files within the specified folder,
     * and returns a list of their relative paths.
     * Ignores any files/folders prefixed with a dot.
     */
    public static String[] listFiles(String path, boolean relative) {
        return listFiles(new File(path), relative);
    }

    public static String[] listFiles(File folder, boolean relative) {
        String path = folder.getAbsolutePath();
        Vector<String> vector = new Vector<String>();
        listFiles(relative ? (path + File.separator) : "", path, vector);
        String outgoing[] = new String[vector.size()];
        vector.copyInto(outgoing);
        return outgoing;
    }

    static protected void listFiles(String basePath, String path, Vector<String> vector) {
        File folder = new File(path);
        String list[] = folder.list();
        if (list == null) return;

        for (int i = 0; i < list.length; i++) {
            if (list[i].charAt(0) == '.') continue;

            File file = new File(path, list[i]);
            String newPath = file.getAbsolutePath();
            if (newPath.startsWith(basePath)) {
                newPath = newPath.substring(basePath.length());
            }
            vector.add(newPath);
            if (file.isDirectory()) {
                listFiles(basePath, newPath, vector);
            }
        }
    }

    public static void handleSystemInfo() {
        Editor.broadcast(Translate.t("Version: ") + systemVersion + "\n");
        Editor.broadcast(Translate.t("Build Number: ") + BUILDNO + "\n");
        Editor.broadcast(Translate.t("Built By: ") + BUILDER + "\n");

        Editor.broadcast(Translate.t("Installed plugins") + ":\n");

        for (String plugin : plugins.keySet()) {
            Version v = getPluginVersion(plugin);
            Editor.broadcast("  " + plugin + " - " + v.toString() + "\n");
        }

        Editor.broadcast("\n" + Translate.t("Processes") + ":\n");
        for (Process p : processes) {
            Editor.broadcast("  " + p + "\n");
        }

        Editor.broadcast("\n" + Translate.t("Threads") + ":\n");
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread t : threads) {
            Editor.broadcast("  " + t.getName() + "\n");
        }
    }

    public static void loadPlugins()
    {
        plugins.clear();

        File pf = getSystemPluginsFolder();;

        if (pf != null) loadPluginsFromFolder(pf);

        String[] entries = (String[]) cores.keySet().toArray(new String[0]);

        for (int i = 0; i < entries.length; i++) {
            Core c = cores.get(entries[i]);
            pf = new File(c.getFolder(), c.get("library.plugins", "plugins"));
            if (pf != null) loadPluginsFromFolder(pf);
        }

        pf = getUserPluginsFolder();
        if (pf != getSystemPluginsFolder()) {
            if (pf != null) loadPluginsFromFolder(pf);
        }
    }

    public static void loadPluginsFromFolder(File f)
    {
        File[] contents = f.listFiles();
        if (contents == null) return;

        Debug.message("Loading plugins from " + f.getAbsolutePath());

        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isDirectory()) {
                loadPluginsFromFolder(contents[i]);
            } else if (contents[i].getName().toLowerCase().endsWith(".jar")) {
                loadPlugin(contents[i]);
            }
        }
    }

    public static String getPluginInfo(String plugin, String item) {
        try {
            Class<?> pluginClass = plugins.get(plugin);
            if (pluginClass == null) {
                return null;
            }

            Method getInfo = pluginClass.getMethod("getInfo", String.class);
            String val = (String)(getInfo.invoke(null, "version"));
            return val;
        } catch (Exception e) {
            error(e);
        }
        return null;
    }
    
    public static Version getPluginVersion(String plugin) {
        return new Version(getPluginInfo(plugin, "version"));
    }

    public static void loadPlugin(File jar)
    {
        try {
            URL[] urlList = new URL[1];
            urlList[0]  = jar.toURI().toURL();

            URLClassLoader loader = new URLClassLoader(urlList);

            String className = null;

            Debug.message("    Loading plugin " + jar.getAbsolutePath());

            JarFile jf = new JarFile(jar);
            Manifest manifest = jf.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();
            Version minimumVersion = new Version(manifestContents.getValue("Minimum-Version"));

            if (minimumVersion.compareTo(systemVersion) > 0) {
                Debug.message("        Requires newer version of UECIDE - skipping");
                return;
            }

            className = manifestContents.getValue("Main-Class");
            Debug.message("        Class: " + className);
            if (className == null) {
                className = findClassInZipFile(jar);

                if (className == null) {
                    return;
                }
            }

            Version oldVersion = getPluginVersion(className);
            Version newVersion = new Version(manifestContents.getValue("Version"));
            int diff = oldVersion.compareTo(newVersion);

            if (newVersion.compareTo(oldVersion) > 0) {
                HashMap<String, String> pluginInfo = new HashMap<String, String>();
                pluginInfo.put("version", manifestContents.getValue("Version"));
                pluginInfo.put("compiled", manifestContents.getValue("Compiled"));
                pluginInfo.put("jarfile", jar.getAbsolutePath());
                pluginInfo.put("shortcut", manifestContents.getValue("Shortcut"));
                pluginInfo.put("modifier", manifestContents.getValue("Modifier"));

                Class<?> pluginClass = Class.forName(className, true, loader);
                Method setLoader = pluginClass.getMethod("setLoader", URLClassLoader.class);
                Method setInfo = pluginClass.getMethod("setInfo", HashMap.class);
                setLoader.invoke(null, loader);
                setInfo.invoke(null, pluginInfo);

                plugins.put(className, pluginClass);
            }

        } catch (Exception e) {
            error(e);
        }
    }

    public static String findClassInZipFile(File file) {
        String base = file.getName();
        if (!base.endsWith(".jar")) {
            return null;
        }

        base = base.substring(0, base.length()-4);

        String classFileName = "/" + base + ".class";


        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (!entry.isDirectory()) {
                    String name = entry.getName();

                    if (name.endsWith(classFileName)) {
                        // Remove .class and convert slashes to periods.
                        return name.substring(0, name.length() - 6).replace('/', '.');
                    }
                }
            }
        } catch (IOException e) {
            error(e);
        }
        return null;
    }

    public static File getTmpDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public static void applyPreferences() {
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

    if (isWindows()) {
      params = new String[] { "cmd", "/c" };

    } else if (isMacOS()) {
      params = new String[] { "open" };

    } else if (isUnix()) {
      if (openLauncher == null) {
        try {
          Process p = Runtime.getRuntime().exec(new String[] { "gnome-open" });
          /*int result =*/ p.waitFor();
          openLauncher = "gnome-open";
        } catch (Exception e) { }
      }
      if (openLauncher == null) {
        // Attempt with kde-open
        try {
          Process p = Runtime.getRuntime().exec(new String[] { "kde-open" });
          /*int result =*/ p.waitFor();
          openLauncher = "kde-open";
        } catch (Exception e) { }
      }
      if (openLauncher == null) {
        System.err.println("Could not find gnome-open or kde-open, " +
                           "the open() command may not work.");
      }
      if (openLauncher != null) {
        params = new String[] { openLauncher };
      }
    }
    if (params != null) {
      if (params[0].equals(argv[0])) {
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
    } catch (Exception e) {
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



  static public int[] expand(int list[]) {
    return expand(list, list.length << 1);
  }

  static public int[] expand(int list[], int newSize) {
    int temp[] = new int[newSize];
    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
    return temp;
  }

    static public File getUserCacheFolder() {
        File uf = preferences.getFile("location.cache");
        if (uf != null) {
            if (!uf.exists()) {
                uf.mkdirs();
            }
            return uf;
        }
        File f = getSettingsFile("cache");
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }


    static public File getUserCoresFolder() {
        File uf = preferences.getFile("location.cores");
        if (uf != null) {
            if (!uf.exists()) {
                uf.mkdirs();
            }
            return uf;
        }
        File f = getSettingsFile("cores");
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    static public File getSystemCoresFolder() {
        return new File(getHardwareFolder(),"cores");
    }

    static public File getUserBoardsFolder() {
        File uf = preferences.getFile("location.boards");
        if (uf != null) {
            if (!uf.exists()) {
                uf.mkdirs();
            }
            return uf;
        }
        File f = getSettingsFile("boards");
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    static public File getSystemBoardsFolder() {
        return new File(getHardwareFolder(),"boards");
    }

    static public File getSystemThemesFolder() {
        return new File(getHardwareFolder(), "themes");
    }

    static public File getUserThemesFolder() {
        File tf = preferences.getFile("location.themes");
        if (tf != null) {
            if (!tf.exists()) {
                tf.mkdirs();
            }
            return tf;
        }
        File f = getSettingsFile("themes");
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    static public File getUserPluginsFolder() {
        File uf = preferences.getFile("location.plugins");
        if (uf != null) {
            if (!uf.exists()) {
                uf.mkdirs();
            }
            return uf;
        }
        File f = getSettingsFile("plugins");
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    static public File getSystemPluginsFolder() {
        return getContentFile("plugins");
    }

    static public File getUserCompilersFolder() {
        File uf = preferences.getFile("location.compilers");
        if (uf != null) {
            if (!uf.exists()) {
                uf.mkdirs();
            }
            return uf;
        }
        File f = getSettingsFile("compilers");
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    static public File getSystemCompilersFolder() {
        return new File(getHardwareFolder(),"compilers");
    }

    static public void errorReport(Thread t, Throwable e) {
        showError("Uncaught Exception", "An uncaught exception occurred in thread " + t.getName() + " (" + t.getId() + ")\n" +
                                        "The cause is: " + e.getCause() + "\n" +
                                        "The message is: " + e.getMessage() + "\n", e);
    }

    static public void broken(Thread t, Throwable e) {
        try {
            if (e.getCause() == null) { 
                return;
            }
            Debug.message("");
            Debug.message("******************** EXCEPTION ********************");
            Debug.message("An uncaught exception occurred in thread " + t.getName() + " (" + t.getId() + ")");
            Debug.message("    The cause is: " + e.getCause());
            Debug.message("    The message is: " + e.getMessage());
            Debug.message("");
            for (StackTraceElement element : e.getStackTrace()) {
                if (element != null) {
                    Debug.message("        " + element);
                }
            }
            Debug.message("******************** EXCEPTION ********************");
            Debug.message("");
        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    public static byte[] loadBytesRaw(File file) {
        try {
            int size = (int) file.length();
            FileInputStream input = new FileInputStream(file);
            byte buffer[] = new byte[size];
            int offset = 0;
            int bytesRead;
            while ((bytesRead = input.read(buffer, offset, size-offset)) != -1) {
                offset += bytesRead;
                if (bytesRead == 0) break;
            }
            input.close();  // weren't properly being closed
            input = null;
            return buffer;
    } catch (Exception e) {
        error(e);
        return null;
    }
    }


    public static void error(String e) {
        Editor.broadcast(e);
        System.err.println(e);
        Debug.message(e);
    }

    public static void error(Throwable e) {

        Editor.broadcast(e.getMessage());
        try {
            Debug.message("");
            Debug.message("******************** EXCEPTION ********************");
            Debug.message("An uncaught exception occurred:");
            Debug.message("    The cause is: " + e.getCause());
            Debug.message("    The message is: " + e.getMessage());
            Debug.message("");
            for (StackTraceElement element : e.getStackTrace()) {
                if (element != null) {
                    Debug.message("        " + element);
                }
            }
            Debug.message("******************** EXCEPTION ********************");
            Debug.message("");
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        System.err.println(sw.toString());
    }

    // This handy little function will rebuild the whole of the internals of
    // UECIDE - that is, all the boards, cores, compilers and libraries etc.
    public static void cleanAndScanAllSettings() {
        compilers = new TreeMap<String, Compiler>();
        cores = new TreeMap<String, Core>();
        boards = new TreeMap<String, Board>();
        plugins = new TreeMap<String, Class<?>>();
        pluginInstances = new ArrayList<Plugin>();

        Editor.broadcast(Translate.t("Updating serial ports..."));

        Serial.updatePortList();
        Serial.fillExtraPorts();

        Editor.broadcast(Translate.t("Scanning compilers..."));
        loadCompilers();
        Editor.broadcast(Translate.t("Scanning cores..."));
        loadCores();
        Editor.broadcast(Translate.t("Scanning boards..."));
        loadBoards();
        Editor.broadcast(Translate.t("Scanning plugins..."));
        loadPlugins();
        Editor.broadcast(Translate.t("Scanning libraries..."));
        gatherLibraries();
        Editor.broadcast(Translate.t("Update complete"));
        Editor.updateAllEditors();
        Editor.selectAllEditorBoards();
    }

    public static void updateLookAndFeel() {
        Editor.updateLookAndFeel();
    }

    public static ImageIcon loadIconFromResource(String res, URLClassLoader loader) {
        URL loc = loader.getResource(res);

        if (loc == null) {
            loc = Base.class.getResource("/uecide/app/icons/unknown.png");
        }
        return new ImageIcon(loc);
    }

    public static BufferedImage loadImageFromResource(String res) {
        if (!res.startsWith("/")) {
            res = "/uecide/app/" + res;
        }
        URL loc = Base.class.getResource(res);

        if (loc == null) {
            loc = Base.class.getResource("/uecide/app/icons/unknown.png");
        }
        try {
            BufferedImage im = ImageIO.read(loc);
            return im;
        } catch (Exception e) {
            error(e);
        }
        return null;
    }

    public static ImageIcon loadIconFromResource(String res) {
        if (!res.startsWith("/")) {
            res = "/uecide/app/icons/" + res;
        }
        URL loc = Base.class.getResource(res);

        if (loc == null) {
            loc = Base.class.getResource("/uecide/app/icons/unknown.png");
        }
        return new ImageIcon(loc);
    }

    public boolean copyResourceToFile(String res, File dest) {
        System.err.println("RES: " + res);
        try {
            InputStream from = Base.class.getResourceAsStream(res);
            OutputStream to =
                new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buffer = new byte[16 * 1024];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }
            to.flush();
            from.close();
            from = null;
            to.close();
            to = null;
        } catch (Exception e) {
            error(e);
            return false;
        }
        return true;
    }

    private static ArrayList<String> getResourcesFromJarFile(File file, String root, String extension) {
        ArrayList<String> retval = new ArrayList<String>();
        ZipFile zf;
        try{
            zf = new ZipFile(file);
            final Enumeration e = zf.entries();
            while(e.hasMoreElements()){
                final ZipEntry ze = (ZipEntry) e.nextElement();
                final String fileName = ze.getName();
                if (fileName.startsWith(root) && fileName.endsWith(extension)) {
                    retval.add(fileName);
                }
            }
            zf.close();
        } catch(Exception e){
            error(e);
        }
        return retval;
    }

    private String getBundleVersion(String path) {
        try {
            InputStream instr = Base.class.getResourceAsStream(path);
            if (instr == null) {
                return "0.0.0a";
            }
            JarInputStream jis = new JarInputStream(instr);
            Manifest manifest = jis.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();
            return manifestContents.getValue("Version");
        } catch (Exception e) {
            error(e);
        }
        return "unknown";
    }
}


