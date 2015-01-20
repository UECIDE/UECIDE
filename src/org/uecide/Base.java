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

package org.uecide;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import org.uecide.plugin.*;

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

import com.jtattoo.plaf.*;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

import org.reflections.*;
import org.reflections.util.*;
import org.reflections.scanners.*;

/*! The Base class provides the initial application
 *  startup code, parsing command line options, loading
 *  preferences, themes, etc, then scanning all the boards,
 *  cores, compilers etc.  It also provides a central storage
 *  location for application data, and a selection of useful
 *  helper functions.
 */
public class Base {

    public static HashMap<String, PropertyFile> iconSets = new HashMap<String, PropertyFile>();

    public static final String defaultIconSet = "Gnomic";

    public static int REVISION = 23;
    public static String RELEASE = "release";
    public static int BUILDNO = 0;
    public static String BUILDER = "";

    public static String iconSet = defaultIconSet;

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

    public static CommandLine cli = new CommandLine();

    // Location for untitled items
    static File untitledFolder;

    public static PropertyFile preferences;
    public static PropertyFile session = new PropertyFile();
    public static Theme theme;

    public static HashMap<Object, DiscoveredBoard> discoveredBoards = new HashMap<Object, DiscoveredBoard>();

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
        new Base(args);
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

    /*! The constructor is the main execution routine. */
    public Base(String[] args) {

        cli.addParameter("debug", "", Boolean.class, "Enable the debug window");
        cli.addParameter("verbose", "", Boolean.class, "Output debug log to stdout");
        cli.addParameter("exceptions", "", Boolean.class, "Output exceptions to stderr");
        cli.addParameter("headless", "", Boolean.class, "Enable headless operation");
        cli.addParameter("datadir", "location", String.class, "Specify location for plugins and data");
        cli.addParameter("last-sketch", "", Boolean.class, "Automatically load last used sketch");
        cli.addParameter("clean", "", Boolean.class, "Clean the build folder");
        cli.addParameter("compile", "", Boolean.class, "Immediately compile loaded sketch");
        cli.addParameter("upload", "", Boolean.class, "Immediately compile and upload loaded sketch");
        cli.addParameter("board", "name", String.class, "Select specific board");
        cli.addParameter("core", "name", String.class, "Select specific core");
        cli.addParameter("compiler", "name", String.class, "Select specific compiler");
        cli.addParameter("port", "name", String.class, "Select specific serial port");
        cli.addParameter("programmer", "name", String.class, "Select specific programmer");
        cli.addParameter("purge", "", Boolean.class, "Purge the cache files");
        cli.addParameter("help", "", Boolean.class, "This help text");

        cli.process(args);

        headless = cli.isSet("headless");
        boolean loadLastSketch = cli.isSet("last-sketch");

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

            RELEASE = manifestContents.getValue("Release");;

            Debug.message("Version: " + systemVersion);
        } catch(Exception e) {
            error(e);
        }

        // Get the initial basic theme data
        theme = new Theme("/org/uecide/config/theme.txt");
        theme.setPlatformAutoOverride(true);

        initPlatform();

        preferences = new PropertyFile(getSettingsFile("preferences.txt"), "/org/uecide/config/preferences.txt");
        preferences.setPlatformAutoOverride(true);

        platform.setSettingsFolderEnvironmentVariable();

        Debug.setLocation(new Point(preferences.getInteger("debug.window.x"), preferences.getInteger("debug.window.y")));
        Debug.setSize(new Dimension(preferences.getInteger("debug.window.width"), preferences.getInteger("debug.window.height")));

        ArrayList<String> bundledPlugins = getResourcesFromJarFile(getJarLocation(), "org/uecide/bundles/plugins/", ".jar");
        File upf = getUserPluginsFolder();

        for(String s : bundledPlugins) {
            String fn = s.substring(s.lastIndexOf("/") + 1);
            File dest = new File(upf, fn);

            if(!dest.exists()) {
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


                if(newVersion.compareTo(oldVersion) > 0) {
                    System.err.println("Upgrading your version of " + fn + " to " + bv);
                    copyResourceToFile("/" + s, dest);
                }
            } catch(Exception e) {
                error(e);
            }
        }

        // Now we reload the theme data with user overrides
        // (we didn't know where they were before)
        theme = new Theme(getSettingsFile("theme.txt"), "/org/uecide/config/theme.txt");
        theme.setPlatformAutoOverride(true);

        if(!headless) {
            splashScreen = new Splash();

            if(RELEASE.equals("beta")) {
                splashScreen.setBetaMessage("** BETA VERSION **");
            }

            splashScreen.setMessage("Loading " + theme.get("product.cap") + "...", 10);
        }

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        try {
            if(!headless) {
                if(isUnix()) {
                    Toolkit xToolkit = Toolkit.getDefaultToolkit();
                    java.lang.reflect.Field awtAppClassNameField =
                        xToolkit.getClass().getDeclaredField("awtAppClassName");
                    awtAppClassNameField.setAccessible(true);
                    awtAppClassNameField.set(xToolkit, Base.theme.get("product.cap"));
                }

                String laf = Base.preferences.get("editor.laf");

                UIManager.setLookAndFeel(laf);

                if(laf == null) {
                    laf = Base.preferences.getPlatformSpecific("editor.laf.default");

                    if(laf != null) {
                        Base.preferences.set("editor.laf", laf);
                    }
                }

                if(laf != null) {
                    String lafTheme = "";

                    if(laf.indexOf(";") > -1) {
                        lafTheme = laf.substring(laf.lastIndexOf(";") + 1);
                        laf = laf.substring(0, laf.lastIndexOf(";"));
                    }

                    if(laf.startsWith("de.muntjak.tinylookandfeel.")) {
                        de.muntjak.tinylookandfeel.ThemeDescription[] tinyThemes = de.muntjak.tinylookandfeel.Theme.getAvailableThemes();
                        URI themeURI = null;

                        for(de.muntjak.tinylookandfeel.ThemeDescription td : tinyThemes) {
                            if(td.getName().equals(lafTheme)) {
                                de.muntjak.tinylookandfeel.Theme.loadTheme(td);
                                break;
                            }
                        }
                    }


                    if(laf.startsWith("com.jtattoo.plaf.")) {
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

                }
            }

        } catch(Exception e) {
            error(e);
        }

        // Create a location for untitled sketches
        untitledFolder = createTempFolder("untitled");
        untitledFolder.deleteOnExit();

        if(!headless) splashScreen.setMessage("Loading Application...", 20);

        platform.init(this);

        // Get paths for the libraries and examples in the Processing folder
        //String workingDirectory = System.getProperty("user.dir");

        examplesFolder = getContentFile("examples");
        toolsFolder = getContentFile("tools");

        // Get the sketchbook path, and make sure it's set properly
        String sketchbookPath = preferences.get("sketchbook.path");

//        Translate.load("swedish");

        // If no path is set, get the default sketchbook folder for this platform
        if(sketchbookPath == null) {
            File defaultFolder = getDefaultSketchbookFolder();
            preferences.set("sketchbook.path", defaultFolder.getAbsolutePath());
            sketchbookPath = defaultFolder.getAbsolutePath();
        }

        File sketchbookFolder = new File(sketchbookPath);

        if(!sketchbookFolder.exists()) {
            sketchbookFolder.mkdirs();
        }

        compilers = new TreeMap<String, Compiler>();
        cores = new TreeMap<String, Core>();
        boards = new TreeMap<String, Board>();
        plugins = new TreeMap<String, Class<?>>();
        pluginInstances = new ArrayList<Plugin>();

        Serial.updatePortList();
        Serial.fillExtraPorts();

        if(!headless) splashScreen.setMessage("Loading Themes...", 25);

        loadThemes();
        theme.fullyParseFile();

        if(!headless) splashScreen.setMessage("Loading Compilers...", 30);

        loadCompilers();

        if(!headless) splashScreen.setMessage("Loading Cores...", 40);

        loadCores();

        if(!headless) splashScreen.setMessage("Loading Boards...", 50);

        loadBoards();

        if(!headless) splashScreen.setMessage("Loading Plugins...", 60);

        loadPlugins();

        loadIconSets();

        if (preferences.get("editor.icons") != null) {
            if (iconSets.get(preferences.get("editor.icons")) == null) {
                iconSet = defaultIconSet;
            } else {
                iconSet = preferences.get("editor.icons");
            }
        }

        if(!headless) splashScreen.setMessage("Loading Libraries...", 70);

        gatherLibraries();

        initMRU();

        if(!headless) splashScreen.setMessage("Opening Editor...", 80);

        boolean opened = false;

        // Check if any files were passed in on the command line
        for(int i = 0; i < args.length; i++) {
            String path = args[i];

            if(path.startsWith("--")) {
                continue;
            }

            // Fix a problem with systems that use a non-ASCII languages. Paths are
            // being passed in with 8.3 syntax, which makes the sketch loader code
            // unhappy, since the sketch folder naming doesn't match up correctly.
            // http://dev.processing.org/bugs/show_bug.cgi?id=1089
            if(isWindows()) {
                try {
                    File file = new File(args[i]);
                    path = file.getCanonicalPath();
                } catch(IOException e) {
                    error(e);
                }
            }

            File p = new File(path);
            Debug.message("Loading sketch " + p);
            if (p.exists()) {
                opened = doOpenThings(p);
            }
        }

        if(loadLastSketch) {
            File lastFile = MRUList.get(0);

            if(lastFile != null) {
                opened = doOpenThings(lastFile);
            }
        }

        // Create a new empty window (will be replaced with any files to be opened)
        if(!opened && !headless) {
            handleNew();
        }

        if(!headless) {
            splashScreen.setMessage("Complete", 100);
            splashScreen.dispose();

            synchronized (Editor.editorList) {
                if(boards.size() == 0) {
                    showWarning(Translate.t("No boards installed"), Translate.w("You have no boards installed.  I will now open the plugin manager so you can install the boards, cores and compilers you need to use %1.", 40, "\n", theme.get("product.cap")), null);
                        Editor.editorList.get(0).launchPlugin(plugins.get("org.uecide.plugin.PluginManager"));
                } else if(cores.size() == 0) {
                    showWarning(Translate.t("No cores installed"), Translate.w("You have no cores installed.  I will now open the plugin manager so you can install the boards, cores and compilers you need to use %1.", 40, "\n", theme.get("product.cap")), null);
                    Editor.editorList.get(0).launchPlugin(plugins.get("org.uecide.plugin.PluginManager"));
                } else if(compilers.size() == 0) {
                    showWarning(Translate.t("No compilers installed"), Translate.w("You have no compilers installed.  I will now open the plugin manager so you can install the boards, cores and compilers you need to use %1.", 40, "\n", theme.get("product.cap")), null);
                    Editor.editorList.get(0).launchPlugin(plugins.get("org.uecide.plugin.PluginManager"));
                }
            }
        }

        if(isTimeToCheckVersion()) {
            if(isNewVersionAvailable()) {
                if(headless) {
                    System.err.println("A new version is available!");
                    System.err.println("Download it from: " + Base.theme.get("version." + RELEASE + ".download"));
                } else {
                    String[] options = {"Yes", "No"};
                    int n = JOptionPane.showOptionDialog(null, "A newer version is available.\nWould you like to download it now?", "Newer version available", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                    if(n == 0) {
                        openURL(Base.theme.get("version." + RELEASE + ".download"));
                    }
                }
            }
        }

        session.set("os.version", getOSVersion());

        if(headless) {
            System.exit(0);
        }

        Reflections mainReflections = new Reflections("");
        Set<Class<? extends Service>> serviceClasses = mainReflections.getSubTypesOf(Service.class);
        for (Class<? extends Service> c : serviceClasses) {
            try {
                Constructor<?> ctor = c.getConstructor();
                Service s = (Service)(ctor.newInstance());
                ServiceManager.addService(s);
            } catch (NoSuchMethodException ex) {
            } catch (InstantiationException ex) {
            } catch (IllegalAccessException ex) {
            } catch (InvocationTargetException ex) {
            }
        }
    }
    
    /*! Attempt to add a jar file as a URL to the system class loader */
    public static void addURL(URL u) {
        final Class[] parameters = new Class[]{URL.class}; 
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        try {
            Debug.message("...1");
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            Debug.message("...2");
            method.setAccessible(true);
            Debug.message("...3");
            method.invoke(sysloader, new Object[]{u});
            Debug.message("...4");
        } catch (Throwable ex) {
            Base.error(ex);
        }
    }

    /*! Open a sketch in a new Editor (if not running headless) and set up any preset values
     *  specified on the command line.
     */
    static boolean doOpenThings(File sketch) {
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
        }

        if(presetPort != null) {
            s.setSerialPort(presetPort);
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
            Class<?> platformClass = Class.forName("org.uecide.Platform");

            if(Base.isMacOS()) {
                platformClass = Class.forName("org.uecide.macosx.Platform");
            } else if(Base.isWindows()) {
                platformClass = Class.forName("org.uecide.windows.Platform");
            } else if(Base.isUnix()) {
                platformClass = Class.forName("org.uecide.unix.Platform");
            }

            platform = (Platform) platformClass.newInstance();
        } catch(Exception e) {
            Base.showError("Problem Setting the Platform",
                           "An unknown error occurred while trying to load\n" +
                           "platform-specific code for your machine.", e);
        }
    }


    /*! Check that the Java installation is new enough for us to run */
    static protected void initRequirements() {
        try {
            Class.forName("com.sun.jdi.VirtualMachine");
        } catch(ClassNotFoundException cnfe) {
            Base.showPlatforms();
            Base.showError("Please install JDK 1.5 or later",
                           theme.get("product.cap") + " requires a full JDK (not just a JRE)\n" +
                           "to run. Please install JDK 1.5 or later.\n" +
                           "More information can be found in the reference.", cnfe);
        }
    }


    /* Initialize the internal MRU list from the preferences set */
    public static void initMRU() {
        MRUList = new ArrayList<File>();

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
    }

    /*! Update the internal MRU list with a new File */
    public static void updateMRU(File f) {
        if (f == null) {
            return;
        }

        if(!f.isDirectory()) {
            f = f.getParentFile();
        }

        if (MRUList == null) {
            initMRU();
        }

        MRUList.remove(f);
        MRUList.add(0, f);

        while(MRUList.size() > 10) {
            MRUList.remove(10);
        }

        for(int i = 0; i < 10; i++) {
            if(i < MRUList.size()) {
                preferences.set("sketch.mru." + i, MRUList.get(i).getAbsolutePath());
            } else {
                preferences.unset("sketch.mru." + i);
            }
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

    /*! Load all the compilers into the main compilers list */
    public static void loadCompilers() {
        compilers.clear();
        loadCompilersFromFolder(getSystemCompilersFolder());

        if(getUserCompilersFolder() != getSystemCompilersFolder()) {
            loadCompilersFromFolder(getUserCompilersFolder());
        }
    }

    /*! Load any compilers found in the specified folder */
    public static void loadCompilersFromFolder(File folder) {
        if(folder == null) {
            return;
        }

        if(!folder.exists()) {
            return;
        }

        if(!folder.isDirectory())
            return;

        String cl[] = folder.list();

        Debug.message("Loading compilers from " + folder.getAbsolutePath());

        for(int i = 0; i < cl.length; i++) {
            if(cl[i].charAt(0) == '.')
                continue;

            File cdir = new File(folder, cl[i]);

            if(cdir.isDirectory()) {
                File cfile = new File(cdir, "compiler.txt");

                if(cfile.exists()) {
                    Debug.message("    Loading core " + cfile.getAbsolutePath());
                    Compiler newCompiler = new Compiler(cdir);

                    if(newCompiler.isValid()) {
                        compilers.put(newCompiler.getName(), newCompiler);
                    }
                }
            }
        }
    }

    /*! Load all the cores into the main cores list */
    public static void loadCores() {
        cores.clear();
        loadCoresFromFolder(getSystemCoresFolder());

        if(getUserCoresFolder() != getSystemCoresFolder()) {
            loadCoresFromFolder(getUserCoresFolder());
        }
    }

    /*! Load any cores found in the specified folder */
    public static void loadCoresFromFolder(File folder) {
        if(!folder.isDirectory())
            return;

        String cl[] = folder.list();
        Debug.message("Loading cores from " + folder.getAbsolutePath());

        for(int i = 0; i < cl.length; i++) {
            if(cl[i].charAt(0) == '.')
                continue;

            File cdir = new File(folder, cl[i]);

            if(cdir.isDirectory()) {
                File cfile = new File(cdir, "core.txt");

                if(cfile.exists()) {
                    Debug.message("    Loading core " + cfile.getAbsolutePath());
                    Core newCore = new Core(cdir);

                    if(newCore.isValid()) {
                        cores.put(newCore.getName(), newCore);
                    }
                }
            }
        }
    }

    /*! Load all the boards into the main boards list */
    public static void loadBoards() {
        boards.clear();
        loadBoardsFromFolder(getSystemBoardsFolder());

        if(getUserBoardsFolder() != getSystemBoardsFolder()) {
            loadBoardsFromFolder(getUserBoardsFolder());
        }
    }

    /*! Load any boards found in the specified folder */
    public static void loadBoardsFromFolder(File folder) {
        String bl[] = folder.list();

        if(bl == null) {
            return;
        }

        Debug.message("Loading boards from " + folder.getAbsolutePath());

        for(int i = 0; i < bl.length; i++) {
            if(bl[i].charAt(0) == '.')
                continue;

            File bdir = new File(folder, bl[i]);

            if(bdir.isDirectory()) {
                File bfile = new File(bdir, "board.txt");

                if(bfile.exists()) {
                    Debug.message("    Loading board " + bfile.getAbsolutePath());
                    Board newBoard = new Board(bdir);

                    if(newBoard.isValid()) {
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

    /*! Create a new untitled document in a new sketch window.  */
    public static void handleNew() {
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
    public static Editor createNewEditor(String path) {
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

        return editor;
    }

// .................................................................


    /*! Load all the libraries in the system */
    public static void gatherLibraries() {
        Library.loadLibraries();
    }

    /**
    * Show the Preferences window.
    */
    public static void handlePrefs() {
        if(preferencesFrame == null) preferencesFrame = new Preferences(null);

        preferencesFrame.showFrame();
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

    // .................................................................

    public static File getSettingsFolder() {
        File settingsFolder = null;

        if(overrideSettingsFolder != null) {
            settingsFolder = new File(overrideSettingsFolder);

            if(!settingsFolder.exists()) {
                settingsFolder.mkdirs();
            }

            return settingsFolder;
        }

        try {
            settingsFolder = platform.getSettingsFolder();
        } catch(Exception e) {
            showError(Translate.t("Problem getting data folder"),
                      Translate.t("Error getting the data folder."), e);
            error(e);
            return null;
        }

        if(!settingsFolder.exists()) {
            if(!settingsFolder.mkdirs()) {
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
        } catch(Exception e) { }

        if(sketchbookFolder == null) {
            sketchbookFolder = promptSketchbookLocation();
        }

        // create the folder if it doesn't exist already
        boolean result = true;

        if(!sketchbookFolder.exists()) {
            result = sketchbookFolder.mkdirs();
        }

        if(!result) {
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

        if(!folder.exists()) {
            folder.mkdirs();
            return folder;
        }

        String prompt = Translate.t("Select (or create new) folder for sketches...");
        folder = Base.selectFolder(prompt, null, null);

        if(folder == null) {
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

        } catch(Exception e) {
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

        } catch(Exception e) {
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


    public static void showReference(String filename) {
        File referenceFolder = Base.getContentFile("reference");
        File referenceFile = new File(referenceFolder, filename);
        openURL(referenceFile.getAbsolutePath());
    }

    public static void showGettingStarted() {
        if(Base.isMacOS()) {
            Base.showReference("Guide_MacOSX.html");
        } else if(Base.isWindows()) {
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
        if(title == null) title = Translate.t("Message");

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
        if(title == null) title = Translate.t("Warning");

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
        if(title == null) title = Translate.t("Error");

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

    // incomplete
    public static int showYesNoCancelQuestion(Editor editor, String title, String primary, String secondary) {
        if(!Base.isMacOS()) {
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
                                "<head> <style type=\"text/css\">" +
                                "b { font: 13pt \"Lucida Grande\" }" +
                                "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }" +
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

            if(result == options[0]) {
                return JOptionPane.YES_OPTION;
            } else if(result == options[1]) {
                return JOptionPane.CANCEL_OPTION;
            } else if(result == options[2]) {
                return JOptionPane.NO_OPTION;
            } else {
                return JOptionPane.CLOSED_OPTION;
            }
        }
    }

    public static int showYesNoQuestion(Frame editor, String title, String primary, String secondary) {
        if(!Base.isMacOS()) {
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
                                "<head> <style type=\"text/css\">" +
                                "b { font: 13pt \"Lucida Grande\" }" +
                                "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }" +
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

            if(result == options[0]) {
                return JOptionPane.YES_OPTION;
            } else if(result == options[1]) {
                return JOptionPane.NO_OPTION;
            } else {
                return JOptionPane.CLOSED_OPTION;
            }
        }
    }


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
        } catch(InterruptedException e) { }

        return image;
    }

    public static BufferedImage getLibBufferedImage(String name) {
        try {
            BufferedImage image = ImageIO.read(new File(getContentFile("lib"), name));
            return image;
        } catch(Exception e) {
            return null;
        }
    }


    /**
    * Return an InputStream for a file inside the Processing lib folder.
    */
    public static InputStream getLibStream(String filename) {
        try {
            return new FileInputStream(new File(getContentFile("lib"), filename));
        } catch(Exception e) {
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

        for(char c : what.toCharArray()) {
            if(c == '\n') count++;
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

            while((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }

            to.flush();
            from.close(); // ??
            from = null;
            to.close(); // ??
            to = null;

            targetFile.setLastModified(sourceFile.lastModified());
        } catch(Exception e) {
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

            if(!dir.delete()) {
                error(Translate.t("Could not delete %1", dir.getName()));
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
        if(!dir.exists()) return;

        String files[] = dir.list();

        for(int i = 0; i < files.length; i++) {
            if(files[i].equals(".") || files[i].equals("..")) continue;

            File dead = new File(dir, files[i]);

            if(!dead.isDirectory()) {
                if(!preferences.getBoolean("compiler.save_build_files")) {
                    if(!dead.delete()) {
                        // temporarily disabled
                        error(Translate.t("Could not delete %1", dead.getName()));
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
        if(files == null) return -1;

        for(int i = 0; i < files.length; i++) {
            if(files[i].equals(".") || (files[i].equals("..")) ||
                    files[i].equals(".DS_Store")) continue;

            File fella = new File(folder, files[i]);

            if(fella.isDirectory()) {
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

        if(list == null) return;

        for(int i = 0; i < list.length; i++) {
            if(list[i].charAt(0) == '.') continue;

            File file = new File(path, list[i]);
            String newPath = file.getAbsolutePath();

            if(newPath.startsWith(basePath)) {
                newPath = newPath.substring(basePath.length());
            }

            vector.add(newPath);

            if(file.isDirectory()) {
                listFiles(basePath, newPath, vector);
            }
        }
    }

    public static void handleSystemInfo() {
        Editor.broadcast(Translate.t("Version: ") + systemVersion + "\n");
        Editor.broadcast(Translate.t("Build Number: ") + BUILDNO + "\n");
        Editor.broadcast(Translate.t("Built By: ") + BUILDER + "\n");

        Editor.broadcast(Translate.t("Installed plugins") + ":\n");

        for(String plugin : plugins.keySet()) {
            Version v = getPluginVersion(plugin);
            Editor.broadcast("  " + plugin + " - " + v.toString() + "\n");
        }

        Editor.broadcast("\n" + Translate.t("Processes") + ":\n");

        for(Process p : processes) {
            Editor.broadcast("  " + p + "\n");
        }

        Editor.broadcast("\n" + Translate.t("Threads") + ":\n");
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);

        for(Thread t : threads) {
            Editor.broadcast("  " + t.getName() + "\n");
        }
    }

    public static void loadPlugins() {
        File folder = getUserPluginsFolder();
        Debug.message("Loading plugins from " + folder);
        File[] files = folder.listFiles();
        for (File f : files) {
            Debug.message("  Loading " + f);
            try {
                URL u = f.toURI().toURL();
                addURL(u);
            } catch (Exception ex) {
                error(ex);
            }
        }

        Reflections pluginReflections = new Reflections("");
        try {
            Set<Class<? extends Plugin>> pluginClasses = pluginReflections.getSubTypesOf(Plugin.class);
            Debug.message(pluginClasses.toString());
            for (Class<? extends Plugin> c : pluginClasses) {
                Debug.message("Found plugin class " + c.getName());
                plugins.put(c.getName(), c);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
                        return name.substring(0, name.length() - 6).replace('/', '.');
                    }
                }
            }
        } catch(IOException e) {
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

        if(uf != null) {
            if(!uf.exists()) {
                uf.mkdirs();
            }

            return uf;
        }

        File f = getSettingsFile("cache");

        if(!f.exists()) {
            f.mkdirs();
        }

        return f;
    }


    static public File getUserCoresFolder() {
        File uf = preferences.getFile("location.cores");

        if(uf != null) {
            if(!uf.exists()) {
                uf.mkdirs();
            }

            return uf;
        }

        File f = getSettingsFile("cores");

        if(!f.exists()) {
            f.mkdirs();
        }

        return f;
    }

    static public File getSystemCoresFolder() {
        return new File(getHardwareFolder(), "cores");
    }

    static public File getUserBoardsFolder() {
        File uf = preferences.getFile("location.boards");

        if(uf != null) {
            if(!uf.exists()) {
                uf.mkdirs();
            }

            return uf;
        }

        File f = getSettingsFile("boards");

        if(!f.exists()) {
            f.mkdirs();
        }

        return f;
    }

    static public File getSystemBoardsFolder() {
        return new File(getHardwareFolder(), "boards");
    }

    static public File getSystemThemesFolder() {
        return new File(getHardwareFolder(), "themes");
    }

    static public File getUserThemesFolder() {
        File tf = preferences.getFile("location.themes");

        if(tf != null) {
            if(!tf.exists()) {
                tf.mkdirs();
            }

            return tf;
        }

        File f = getSettingsFile("themes");

        if(!f.exists()) {
            f.mkdirs();
        }

        return f;
    }

    static public File getUserPluginsFolder() {
        File uf = preferences.getFile("location.plugins");

        if(uf != null) {
            if(!uf.exists()) {
                uf.mkdirs();
            }

            return uf;
        }

        File f = getSettingsFile("plugins");

        if(!f.exists()) {
            f.mkdirs();
        }

        return f;
    }

    static public File getSystemPluginsFolder() {
        return getContentFile("plugins");
    }

    static public File getUserCompilersFolder() {
        File uf = preferences.getFile("location.compilers");

        if(uf != null) {
            if(!uf.exists()) {
                uf.mkdirs();
            }

            return uf;
        }

        File f = getSettingsFile("compilers");

        if(!f.exists()) {
            f.mkdirs();
        }

        return f;
    }

    static public File getSystemCompilersFolder() {
        return new File(getHardwareFolder(), "compilers");
    }

    static public void errorReport(Thread t, Throwable e) {
        showError("Uncaught Exception", "An uncaught exception occurred in thread " + t.getName() + " (" + t.getId() + ")\n" +
                  "The cause is: " + e.getCause() + "\n" +
                  "The message is: " + e.getMessage() + "\n", e);
    }

    static public void broken(Thread t, Throwable e) {
        try {
            if (Base.preferences.getBoolean("crash.noreport") == false) {
                CrashReporter rep = new CrashReporter(e);
            }
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

    public static byte[] loadBytesRaw(File file) {
        try {
            int size = (int) file.length();
            FileInputStream input = new FileInputStream(file);
            byte buffer[] = new byte[size];
            int offset = 0;
            int bytesRead;

            while((bytesRead = input.read(buffer, offset, size - offset)) != -1) {
                offset += bytesRead;

                if(bytesRead == 0) break;
            }

            input.close();  // weren't properly being closed
            input = null;
            return buffer;
        } catch(Exception e) {
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
    public static void cleanAndScanAllSettings() {
        Thread thr = new Thread() {
            public void run() {

                Editor.bulletAll("Updating serial ports...");

                Serial.updatePortList();
                Serial.fillExtraPorts();

                rescanThemes();
                rescanCompilers();
                rescanCores();
                rescanBoards();
                rescanPlugins();
                rescanLibraries();
                Editor.bulletAll("Update complete.");
                Editor.updateAllEditors();
                Editor.selectAllEditorBoards();
            }
        };
        thr.start();
    }

    public static void rescanPlugins() {
        plugins = new TreeMap<String, Class<?>>();
        pluginInstances = new ArrayList<Plugin>();
        Editor.bulletAll("Scanning plugins...");
        loadPlugins();
    }

    public static void rescanThemes() {
        Editor.bulletAll("Scanning themes...");
        loadThemes();
        theme.fullyParseFile();
    }
    public static void rescanCompilers() {
        compilers = new TreeMap<String, Compiler>();
        Editor.bulletAll("Scanning compilers...");
        loadCompilers();
    }

    public static void rescanCores() {
        cores = new TreeMap<String, Core>();
        Editor.bulletAll("Scanning cores...");
        loadCores();
    }

    public static void rescanBoards() {
        try {
            boards = new TreeMap<String, Board>();
            Editor.bulletAll("Scanning boards...");
            loadBoards();
            Editor.updateAllEditors();
            Editor.selectAllEditorBoards();
        } catch(Exception e) {
            error(e);
        }
    }

    public static void rescanLibraries() {
        Editor.bulletAll("Scanning libraries...");
        gatherLibraries();
    }

    public static void updateLookAndFeel() {
        Editor.updateLookAndFeel();
    }

    public static ImageIcon loadIconFromFile(File f) {
        return new ImageIcon(f.getAbsolutePath());
    }

    public static ImageIcon loadIconFromResource(String res, URLClassLoader loader) {
        URL loc = loader.getResource(res);

        if(loc == null) {
            loc = Base.class.getResource("/org/uecide/icons/unknown.png");
        }

        return new ImageIcon(loc);
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

    public static ImageIcon loadIconFromResource(String res) {
        if(!res.startsWith("/")) {
            res = "/org/uecide/icons/" + res;
        }

        URL loc = Base.class.getResource(res);

        if(loc == null) {
            loc = Base.class.getResource("/org/uecide/icons/unknown.png");
        }

        return new ImageIcon(loc);
    }

    public boolean copyResourceToFile(String res, File dest) {
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
            from = null;
            to.close();
            to = null;
        } catch(Exception e) {
            error(e);
            return false;
        }

        return true;
    }

    private static ArrayList<String> getResourcesFromJarFile(File file, String root, String extension) {
        ArrayList<String> retval = new ArrayList<String>();
        ZipFile zf;

        try {
            zf = new ZipFile(file);
            final Enumeration e = zf.entries();

            while(e.hasMoreElements()) {
                final ZipEntry ze = (ZipEntry) e.nextElement();
                final String fileName = ze.getName();

                if(fileName.startsWith(root) && fileName.endsWith(extension)) {
                    retval.add(fileName);
                }
            }

            zf.close();
        } catch(Exception e) {
            error(e);
        }

        return retval;
    }

    private String getBundleVersion(String path) {
        try {
            InputStream instr = Base.class.getResourceAsStream(path);

            if(instr == null) {
                return "0.0.0a";
            }

            JarInputStream jis = new JarInputStream(instr);
            Manifest manifest = jis.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();
            return manifestContents.getValue("Version");
        } catch(Exception e) {
            error(e);
        }

        return "unknown";
    }

    public Version getLatestVersion() {
        try {
            URL url = new URL(Base.theme.get("version." + RELEASE + ".url"));
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String data = in.readLine();
            in.close();
            return new Version(data);
        } catch(Exception e) {
            error(e);
        }

        return null;
    }

    public boolean isNewVersionAvailable() {
        int time = (int)(System.currentTimeMillis() / 1000L);
        Base.preferences.setInteger("version.lastcheck", time);

        if(Base.preferences.getBoolean("version.check")) {
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
        int lastCheck = Base.preferences.getInteger("version.lastcheck");
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

    public static void loadThemes() {
        File tf = getUserThemesFolder();
        File[] files = tf.listFiles();

        for(File f : files) {
            if(f.getName().endsWith(".theme")) {
                PropertyFile newTheme = new PropertyFile(f);
                String name = f.getName();
                name = name.substring(0, name.lastIndexOf("."));
                theme.mergeData(newTheme, "theme." + name);
            }
        }
    }

    public static HashMap<String, String> getThemeList() {
        HashMap<String, String> themeList = new HashMap<String, String>();
        String[] themeKeys = theme.childKeysOf("theme");

        for(String key : themeKeys) {
            themeList.put(key, theme.get("theme." + key + ".name"));
        }

        return themeList;
    }

    public static int redProportion(Color c) {
        int red = c.getRed();
        int green = c.getGreen();
        int blue = c.getBlue();
        int tot = red + green + blue;
        return red * 100 / tot;
    }

    public static int greenProportion(Color c) {
        int red = c.getRed();
        int green = c.getGreen();
        int blue = c.getBlue();
        int tot = red + green + blue;
        return green * 100 / tot;
    }

    public static int blueProportion(Color c) {
        int red = c.getRed();
        int green = c.getGreen();
        int blue = c.getBlue();
        int tot = red + green + blue;
        return blue * 100 / tot;
    }

    public static Color reddest(Color first, Color... colors) {
        Color cmax = first;
        for (Color c : colors) {
            int rp = redProportion(c);
            if (rp > redProportion(cmax)) {
                cmax = c;
            }
        }
        return cmax;
    }

    public static Color bluest(Color first, Color... colors) {
        Color cmax = first;
        for (Color c : colors) {
            int rp = blueProportion(c);
            if (rp > blueProportion(cmax)) {
                cmax = c;
            }
        }
        return cmax;
    }

    public static Color greenest(Color first, Color... colors) {
        Color cmax = first;
        for (Color c : colors) {
            int rp = greenProportion(c);
            if (rp > greenProportion(cmax)) {
                cmax = c;
            }
        }
        return cmax;
    }

    public static String runFunctionVariable(Sketch sketch, String command, String param) {
        try {
            Class<?> c = Class.forName("org.uecide.varcmd.vc_" + command);

            if(c == null) {
                return "";
            }

            Constructor<?> ctor = c.getConstructor();
            VariableCommand  p = (VariableCommand)(ctor.newInstance());

            Class[] param_types = new Class<?>[2];
            param_types[0] = Sketch.class;
            param_types[1] = String.class;
            Method m = c.getMethod("main", param_types);

            if(m == null) {
                return "";
            }

            Object[] args = new Object[2];
            args[0] = sketch;
            args[1] = param;
            return (String)m.invoke(p, args);
        } catch(Exception e) {
            Base.error(e);
        }

        return "";
    }

    public static boolean runBuiltinCommand(Sketch sketch, String commandline) {
        try {
            String[] split = commandline.split("::");
            int argc = split.length - 1;

            String cmdName = split[0];

            String[] arg = new String[argc];

            for(int i = 0; i < argc; i++) {
                arg[i] = split[i + 1];
            }

            if(!cmdName.startsWith("__builtin_")) {
                return false;
            }

            cmdName = cmdName.substring(10);
            Class<?> c = Class.forName("org.uecide.builtin." + cmdName);

            Constructor<?> ctor = c.getConstructor();
            BuiltinCommand  p = (BuiltinCommand)(ctor.newInstance());

            if(c == null) {
                return false;
            }

            Class<?>[] param_types = new Class<?>[2];
            param_types[0] = Sketch.class;
            param_types[1] = String[].class;
            Method m = c.getMethod("main", param_types);

            Object[] args = new Object[2];
            args[0] = sketch;
            args[1] = arg;

            return (Boolean)m.invoke(p, args);


        } catch(Exception e) {
            Base.error(e);
        }

        return false;
    }


    public static String parseString(String in, PropertyFile tokens, Sketch sketch) {
        int iStart;
        int iEnd;
        int iTest;
        String out;
        String start;
        String end;
        String mid;

        if(in == null) {
            return null;
        }

        out = in;

        iStart = out.indexOf("${");

        if(iStart == -1) {
            return out;
        }

        iEnd = out.indexOf("}", iStart);
        iTest = out.indexOf("${", iStart + 1);

        while((iTest > -1) && (iTest < iEnd)) {
            iStart = iTest;
            iTest = out.indexOf("${", iStart + 1);
        }

        while(iStart != -1) {
            start = out.substring(0, iStart);
            end = out.substring(iEnd + 1);
            mid = out.substring(iStart + 2, iEnd);

            if(mid.indexOf(":") > -1) {
         //       if (sketch != null) {
                    String command = mid.substring(0, mid.indexOf(":"));
                    String param = mid.substring(mid.indexOf(":") + 1);

                    mid = runFunctionVariable(sketch, command, param);
//                } else {
//                    mid = "[context error]";
//                }
            } else {
                String tmid = tokens.get(mid);

                if(tmid == null) {
                    tmid = "";
                }

                mid = tmid;
            }


            if(mid != null) {
                out = start + mid + end;
            } else {
                out = start + end;
            }

            iStart = out.indexOf("${");
            iEnd = out.indexOf("}", iStart);
            iTest = out.indexOf("${", iStart + 1);

            while((iTest > -1) && (iTest < iEnd)) {
                iStart = iTest;
                iTest = out.indexOf("${", iStart + 1);
            }
        }

        // This shouldn't be needed as the methodology should always find any tokens put in
        // by other token replacements.  But just in case, eh?
        if(out != in) {
            out = parseString(out, tokens, sketch);
        }

        return out;
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

    public static String getIconsPath(String name) {
        PropertyFile pf = iconSets.get(name);
        return pf.get("path");
    }

    public static ImageIcon getIcon(String category, String name, int size) {

        String path = getIconsPath(iconSet);

        URL loc = Base.class.getResource(path + "/" + size + "x" + size + "/" + category + "/" + name + ".png");
        if(loc == null) {
            path = getIconsPath(defaultIconSet);
            loc = Base.class.getResource(path + "/" + size + "x" + size + "/" + category + "/" + name + ".png");
        }
        if(loc == null) {
            loc = Base.class.getResource(path + "/" + size + "x" + size + "/actions/unknown.png");
        }

        return new ImageIcon(loc);
    }

    public static void setIconSet(String name) {
        iconSet = name;
    }

    public static String getIconSet() {
        return iconSet;
    }

    public static void loadIconSets() {
        iconSets = new HashMap<String, PropertyFile>();

        System.err.println("Loading icon sets...");      

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.uecide"))
                .setScanners(new ResourcesScanner()));

        Pattern pat = Pattern.compile(".*\\.icon");
        Set<String> icons = reflections.getResources(pat);

        for (String icon : icons) {
            PropertyFile pf = new PropertyFile("/" + icon);
            if (pf.get("name") != null) {
                System.err.println("Icon file " + icon + " = " + pf.get("name"));
                iconSets.put(pf.get("name"), pf);
            }
        }
    }

}
