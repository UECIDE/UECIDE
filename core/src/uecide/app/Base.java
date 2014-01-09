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


import javax.swing.*;
import javax.imageio.*;

import uecide.app.debug.Board;
import uecide.app.debug.Core;
import uecide.app.debug.Compiler;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The base class for the main uecide.application.
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading
 * files and images, etc) that comes from that.
 */
public class Base {
	
    public static int REVISION = 23;
    /** This might be replaced by main() if there's a lib/version.txt file. */
    public static String VERSION_NAME = "0023";
    /** Set true if this a proper release rather than a numbered revision. */
    public static boolean RELEASE = false;
    public static int BUILDNO = 0;
    public static String BUILDER = "";

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
    public static HashMap<String, File> importToLibraryTable;

    // classpath for all known libraries for p5
    // (both those in the p5/libs folder and those with lib subfolders
    // found in the sketchbook)
    public static String librariesClassPath;
  
    public static HashMap<String, Compiler> compilers;
    public static HashMap<String, Board> boards;
    public static HashMap<String, Core> cores;
    public static HashMap<String, Plugin> plugins;
    public static ArrayList<Plugin> pluginInstances;
    static Splash splashScreen;

    // Location for untitled items
    static File untitledFolder;

    public static ArrayList<Editor> editors = new ArrayList<Editor>();
    public static Editor activeEditor;

    public static PropertyFile preferences;
    public static Theme theme;

    public static void main(String args[]) {
        new Base(args);
    }

    public Base(String[] args) {
/*
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                Base.errorReport(t, e);
            }
        });
*/

        headless = false;

        if (isLinux()) {
            if ((System.getenv("DISPLAY") == null) || (System.getenv("DISPLAY").equals(""))) {
                headless = true;
            }
        }

        try {
            JarFile myself = new JarFile("lib/uecide.jar");
            Manifest manifest = myself.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();

            VERSION_NAME = manifestContents.getValue("Version");
            REVISION = Integer.parseInt(manifestContents.getValue("Compiled"));
            BUILDNO = Integer.parseInt(manifestContents.getValue("Build"));
            BUILDER = manifestContents.getValue("Built-By");

            RELEASE = true;
        } catch (Exception e) {
            error(e);
        }

        // Get the initial basic theme data
        theme = new Theme(getContentFile("lib/theme/theme.txt"));
        theme.setPlatformAutoOverride(true);

        System.err.println("Loading " + theme.get("product") + "...");

        initPlatform();
        preferences = new PropertyFile(getSettingsFile("preferences.txt"), getContentFile("lib/preferences.txt"));
        preferences.setPlatformAutoOverride(true);


        // Now we reload the theme data with user overrides
        // (we didn't know where they were before) 
        theme = new Theme(getSettingsFile("theme.txt"), getContentFile("lib/theme/theme.txt"));
        theme.setPlatformAutoOverride(true);

        if (!headless) {
            splashScreen = new Splash();
            splashScreen.setMessage("Loading " + theme.get("product.cap") + "...", 10);
        }

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        try {
            if (!headless) platform.setLookAndFeel();
        } catch (Exception e) {
            String mess = e.getMessage();
            if (mess.indexOf("ch.randelshofer.quaqua.QuaquaLookAndFeel") == -1) {
                System.err.println("Non-fatal error while setting the Look & Feel.");
                System.err.println("The error message follows, however " + theme.get("product.cap") + " should run fine.");
                System.err.println(mess);
            }
        }

//        UIManager.getDefaults().put("TabbedPane.contentBorderInsets", new Insets(0,0,0,0));
//        UIManager.getDefaults().put("TabbedPane.tabsOverlapBorder", true);

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

        // If a value is at least set, first check to see if the folder exists.
        // If it doesn't, warn the user that the sketchbook folder is being reset.
        if (sketchbookPath != null) {
            File skechbookFolder = new File(sketchbookPath);
            if (!skechbookFolder.exists()) {
                Base.showWarning("Sketchbook folder disappeared",
                         "The sketchbook folder no longer exists.\n" +
                         theme.get("product.cap") + " will switch to the default sketchbook\n" +
                         "location, and create a new sketchbook folder if\n" +
                         "necessary. " + theme.get("product.cap") + " will then stop talking about\n" +
                         "himself in the third person.", null);
                sketchbookPath = null;
            }
        }

        // If no path is set, get the default sketchbook folder for this platform
        if (sketchbookPath == null) {
            File defaultFolder = getDefaultSketchbookFolder();
            preferences.set("sketchbook.path", defaultFolder.getAbsolutePath());
            if (!defaultFolder.exists()) {
                defaultFolder.mkdirs();
            }
        }
    
        compilers = new HashMap<String, Compiler>();
        cores = new HashMap<String, Core>();
        boards = new HashMap<String, Board>();
        plugins = new HashMap<String, Plugin>();
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

        // Create a new empty window (will be replaced with any files to be opened)
        if (!opened) {
            handleNew();
        }
        if (!headless) splashScreen.setMessage("Complete", 100);
        if (!headless) splashScreen.dispose();
    }

    static protected void initPlatform() {
        try {
            Class<?> platformClass = Class.forName("uecide.app.Platform");
            if (Base.isMacOS()) {
                platformClass = Class.forName("uecide.app.macosx.Platform");
            } else if (Base.isWindows()) {
                platformClass = Class.forName("uecide.app.windows.Platform");
            } else if (Base.isLinux()) {
                platformClass = Class.forName("uecide.app.linux.Platform");
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
        rebuildSketchbookMenus();
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
 
        for (int i = 0; i < cl.length; i++) {
            if (cl[i].charAt(0) == '.')
                continue;
            File cdir = new File(folder, cl[i]);
            if (cdir.isDirectory()) {
                File cfile = new File(cdir, "compiler.txt");
                if (cfile.exists()) {
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
 
        for (int i = 0; i < cl.length; i++) {
            if (cl[i].charAt(0) == '.')
                continue;
            File cdir = new File(folder, cl[i]);
            if (cdir.isDirectory()) {
                File cfile = new File(cdir, "core.txt");
                if (cfile.exists()) {
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

        for (int i = 0; i < bl.length; i++) {
            if (bl[i].charAt(0) == '.')
                continue;
            File bdir = new File(folder, bl[i]);
            if (bdir.isDirectory()) {
                File bfile = new File(bdir, "board.txt");
                if (bfile.exists()) {
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

    protected static void handleActivated(Editor whichEditor) {
        activeEditor = whichEditor;
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
    public static void handleOpenPrompt() {
        // get the frontmost window frame for placing file dialog
        FileDialog fd = new FileDialog(activeEditor,
            Translate.t("Open %1 sketch...", theme.get("product.cap")),
            FileDialog.LOAD);

        fd.setDirectory(Base.getSketchbookFolder().getAbsolutePath());

        // Only show .pde files as eligible bachelors
        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // TODO this doesn't seem to ever be used. AWESOME.
                //System.out.println("check filter on " + dir + " " + name);
                return name.toLowerCase().endsWith(".ino")
                    || name.toLowerCase().endsWith(".pde");
            }
        });

        fd.setVisible(true);

        String directory = fd.getDirectory();
        String filename = fd.getFile();

        // User canceled selection
        if (filename == null) return;

        File inputFile = new File(directory, filename);
        createNewEditor(inputFile.getAbsolutePath());
    }


    /**
     * Open a sketch in a new window.
     * @param path Path to the pde file for the sketch in question
     * @return the Editor object, so that properties (like 'untitled')
     *         can be set by the caller
     */

    public static Editor createNewEditor(String path) {
        if (activeEditor != null) {
            if (activeEditor.getSketch().isUntitled() && !activeEditor.getSketch().isModified()) {
                activeEditor.openInternal(path);
                return activeEditor;
            }
        }
        Editor editor = new Editor(path);
        editors.add(editor);
        editor.setVisible(true);
        if (path != null) {
            updateMRU(new File(path));
        }
        return editor;
    }


    /**
    * Close a sketch as specified by its editor window.
    * @param editor Editor object of the sketch to be closed.
    * @return true if succeeded in closing, false if canceled.
    */
    public static boolean handleClose(Editor editor) {
        if (editors.size() == 1) {
            if (Base.isMacOS()) {
                Object[] options = { Translate.t("OK"), Translate.t("Cancel") };
                String prompt =
                    "<html> " +
                    "<head> <style type=\"text/css\">"+
                    "b { font: 13pt \"Lucida Grande\" }"+
                    "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                    "</style> </head>" +
                    "<b>" + Translate.t("Are you sure you want to Quit?") + "</b>" +
                    "<p>" + Translate.t("Closing the last open sketch will quit %1.", theme.get("product.cap"));

                int result = JOptionPane.showOptionDialog(editor,
                    prompt,
                    Translate.t("Quit"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
                if (result == JOptionPane.NO_OPTION ||
                    result == JOptionPane.CLOSED_OPTION
                ) {
                    return false;
                }
            }

            // This will store the sketch count as zero
            editors.remove(editor);

            // Save out the current prefs state
            preferences.save();

            // Since this wasn't an actual Quit event, call System.exit()
            System.exit(0);

        } else {
            // More than one editor window open,
            // proceed with closing the current window.
            editor.setVisible(false);
            editor.dispose();
            editors.remove(editor);
        }
        return true;
    }


    /**
    * Handler for File &rarr; Quit.
    * @return false if canceled, true otherwise.
    */
    public static boolean handleQuit() {
        // If quit is canceled, this will be replaced anyway
        // by a later handleQuit() that is not canceled.

        if (handleQuitEach()) {
            // Save out the current prefs state
            preferences.save();

            if (!Base.isMacOS()) {
                // If this was fired from the menu or an AppleEvent (the Finder),
                // then Mac OS X will send the terminate signal itself.
                System.exit(0);
            }
            return true;
        }
        return false;
    }


    /**
    * Attempt to close each open sketch in preparation for quitting.
    * @return false if canceled along the way
    */
    protected static boolean handleQuitEach() {
        int index = 0;
        for (Editor editor : editors) {
            if (editor.checkModified()) {
                // Update to the new/final sketch path for this fella
                index++;

            } else {
                return false;
            }
        }
        return true;
    }


// .................................................................


    /**
    * Asynchronous version of menu rebuild to be used on save and rename
    * to prevent the interface from locking up until the menus are done.
    */

    protected static void rebuildSketchbookMenus() {
        for (Editor e : editors) {
            e.rebuildMRUMenu();
        }
    }

    public static HashMap<String, Library> getLibraryCollection(String name) {
        return libraryCollections.get(name);
    }

    public static HashMap<String, HashMap<String, Library>> libraryCollections;

    public static void gatherLibraries() {
        libraryCollections = new HashMap<String, HashMap<String, Library>>();

        libraryCollections.put("global", loadLibrariesFromFolder(getContentFile("libraries"))); // Global libraries
        String[] corelist = (String[]) cores.keySet().toArray(new String[0]);

        for (String core : corelist) {
            libraryCollections.put(core, loadLibrariesFromFolder(cores.get(core).getLibraryFolder())); // Core libraries
        }

        libraryCollections.put("sketchbook", loadLibrariesFromFolder(new File(getSketchbookFolder(), "libraries"))); // Contributed libraries
    }

    public static HashMap<String, Library> loadLibrariesFromFolder(File folder) {
        HashMap theseLibraries = new HashMap<String, Library>();
        if (!folder.exists()) {
            return theseLibraries;
        }
        File[] list = folder.listFiles();
        for (File f : list) {
            if (f.isDirectory()) {
                File files[] = f.listFiles();
                for (File sf : files) {
                    if ((sf.getName().equals(f.getName() + ".h") || (sf.getName().startsWith(f.getName() + "_") && sf.getName().endsWith(".h")))) {
                        Library newLibrary = new Library(sf);
                        if (newLibrary.isValid()) {
                            theseLibraries.put(newLibrary.getName(), newLibrary);
                        }
                    }
                }
            }
        }
        return theseLibraries;
    }

    /**
     * Show the About box.
     */
    public static void handleAbout() {
        final Image image = Base.getLibImage("theme/about.png", activeEditor);
        final Window window = new Window(activeEditor) {
            public void paint(Graphics g) {
                int x = Integer.parseInt(theme.get("about.version.x"));
                int y = Integer.parseInt(theme.get("about.version.y"));

                if (x < 0) {
                    x = image.getWidth(activeEditor) + x;
                }

                if (y < 0) {
                    y = image.getHeight(activeEditor) + y;
                }

                g.drawImage(image, 0, 0, null);

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g.setFont(theme.getFont("about.version.font"));
                g.setColor(theme.getColor("about.version.color"));
                g.drawString("v" + Base.VERSION_NAME, x, y);
            }
        };
        window.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                window.dispose();
            }
        });
        int w = image.getWidth(activeEditor);
        int h = image.getHeight(activeEditor);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        window.setBounds((screen.width-w)/2, (screen.height-h)/2, w, h);
        window.setVisible(true);
    }

    /**
    * Show the Preferences window.
    */
    public static void handlePrefs() {
        if (preferencesFrame == null) preferencesFrame = new Preferences();
        preferencesFrame.showFrame(activeEditor);
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

        } else if (osname.equals("Linux")) {  // true for the ibm vm
            return "linux";

        } else {
            return "other";
        }
    }

    /**
    * returns true if Processing is running on a Mac OS X machine.
    */
    public static boolean isMacOS() {
        return System.getProperty("os.name").indexOf("Mac") != -1;
    }


    /**
    * returns true if running on windows.
    */
    public static boolean isWindows() {
        return System.getProperty("os.name").indexOf("Windows") != -1;
    }


    /**
    * true if running on linux.
    */
    public static boolean isLinux() {
        return System.getProperty("os.name").indexOf("Linux") != -1;
    }


    // .................................................................


    public static File getSettingsFolder() {
        File settingsFolder = null;

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

    public Editor getActiveEditor()
    {
        return activeEditor;
    }

    //Get the core libraries
        public static File getCoreLibraries(String path) {
        return getContentFile(path);	
    }

    public static String getHardwarePath() {
        return getHardwareFolder().getAbsolutePath();
    }


    public static String getAvrBasePath() {
        if(Base.isLinux()) {
            return ""; // avr tools are installed system-wide and in the path
        } else {
            return getHardwarePath() + File.separator + "tools" +
                File.separator + "avr" + File.separator + "bin" + File.separator;
        }  
    }

    public static File getSketchbookFolder() {
        return new File(preferences.get("sketchbook.path"));
    }


    public static File getSketchbookLibrariesFolder() {
        return new File(getSketchbookFolder(), "libraries");
    }


    public static String getSketchbookLibrariesPath() {
        return getSketchbookLibrariesFolder().getAbsolutePath();
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
        File imageLocation = new File(getContentFile("lib/theme"), "icon.png");
        Image image = Toolkit.getDefaultToolkit().createImage(imageLocation.getAbsolutePath());
        frame.setIconImage(image);
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
	activeEditor.message(Translate.t("Version: ") + VERSION_NAME + "\n");
	activeEditor.message(Translate.t("Build Number: ") + BUILDNO + "\n");
	activeEditor.message(Translate.t("Built By: ") + BUILDER + "\n");

        activeEditor.message(Translate.t("Installed plugins") + ":\n");
        String[] entries = (String[]) plugins.keySet().toArray(new String[0]);

        for (int i = 0; i < entries.length; i++) {
            Plugin t = plugins.get(entries[i]);

            String ver = Translate.t("unknown");
            String com = Translate.t("unknown");


            // Older plugins may not have these methods - ignore them if they don't
            try {
                ver = t.getVersion();
                com = t.getCompiled();
            } catch (Exception e) {
                ver = Translate.t("unknown");
                com = Translate.t("unknown");
            }

            activeEditor.message("  " + entries[i] + " - " + ver + " " + Translate.t("compiled") + " " + com + "\n");
        }

        activeEditor.message("\n" + Translate.t("Processes") + ":\n");
        for (Process p : processes) {
            activeEditor.message("  " + p + "\n");
        }

        activeEditor.message("\n" + Translate.t("Threads") + ":\n");
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread t : threads) {
            activeEditor.message("  " + t.getName() + "\n");
        }
    }


    public static File openFileDialog(String title, final String type)
    {
        // get the frontmost window frame for placing file dialog
        FileDialog fd = new FileDialog(activeEditor,
            title,
            FileDialog.LOAD);

        final String types[] = type.split(",");

        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                for (int i = 0; i < types.length; i++) {
                    if (name.toLowerCase().endsWith("." + types[i])) {
                        return true;
                    }
                }
                return false;
            }
        });

        fd.setVisible(true);

        String directory = fd.getDirectory();
        String filename = fd.getFile();

        // User canceled selection
        if (filename == null) return null;

        File inputFile = new File(directory, filename);
        return inputFile;
    }

    public static void handleAddLibrary()
    {
        File inputFile = openFileDialog(Translate.t("Add Library..."), "zip");

        if (inputFile == null) {
            return;
        }

        if (!inputFile.exists()) {
            activeEditor.message(inputFile.getName() + ": " + Translate.t("not found") + "\n", 2);
            return;
        }

        if (!testLibraryZipFormat(inputFile.getAbsolutePath())) {
            activeEditor.message(Translate.t("Error: %1 is not correctly packaged.", inputFile.getName()) + "\n", 2);
            return;
        }


        new ZipExtractor(inputFile, getSketchbookLibrariesFolder()).execute();
    }

    public static boolean testLibraryZipFormat(String inputFile)
    {
        ArrayList<String> fileList = new ArrayList<String>();
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(inputFile));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                fileList.add(fileName);
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }

        // Now look through fileList for an entry of X/X.cpp and X/X.h where X==X
        boolean foundHeader = false;

        for (int i=0; i<fileList.size(); i++) {
            String entry = fileList.get(i);
            if (entry.endsWith(".h")) {
                String[] bits = entry.split("/");
                String dn = bits[0];
                if (dn.endsWith("-master")) { // It is a github zip
                    dn = dn.substring(0, dn.indexOf("-master"));
                }
                if (bits[1].equals(dn + ".h")) {
                    foundHeader = true;
                }
            }
        }
        return (foundHeader);
    }

    public static int countZipEntries(File inputFile)
    {
        int count = 0;
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(inputFile));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                count++;
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (Exception e) {
            return -1;
        }
        return count;
    }

    public static class ZipExtractor extends SwingWorker<Void, Integer>
    {
        File inputFile;
        File destination;

        public ZipExtractor(File in, File out) {
            this.inputFile = in;
            this.destination = out;
        }

        public ZipExtractor(String in, String out) {
            this.inputFile = new File(in);
            this.destination = new File(out);
        }

        @Override
        protected Void doInBackground() {
            byte[] buffer = new byte[1024];
            ArrayList<String> fileList = new ArrayList<String>();
            publish(-1);
            int files = countZipEntries(inputFile);
            if (files == -1) {
                System.err.println("Zip file empty");
                return null;
            }
            int done = 0;
            try {
                ZipInputStream zis = new ZipInputStream(new FileInputStream(inputFile));
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    String fileName = ze.getName();
                    String[] bits = fileName.split("/");
                    String dn = bits[0];
                    if (dn.endsWith("-master")) { // It is a github zip
                        dn = dn.substring(0, dn.indexOf("-master"));
                    }
                    
                    bits[0] = dn;
                    fileName = "";
                    for (int i = 0; i < bits.length-1; i++) {
                        fileName += bits[i] + "/";
                    }
                    fileName += bits[bits.length-1];
                    System.err.println(fileName);

                    File newFile = new File(destination, fileName);

                    new File(newFile.getParent()).mkdirs();

                    if (ze.isDirectory()) {
                        newFile.mkdirs();
                    } else {

                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        newFile.setExecutable(true, false);
                    }
                    done++;
                    publish((done * 100) / files);
                    ze = zis.getNextEntry();
                    Thread.yield();
                }
                zis.closeEntry();
                zis.close();
            } catch (Exception e) {
                activeEditor.status.progressNotice(Translate.t("Install failed"));
                error(e);
                return null;
            }
            return null;
        }

        @Override
        protected void done() {
            activeEditor.status.progressNotice(Translate.t("Installed."));
            activeEditor.status.unprogress();
            loadCores();
            loadBoards();
            gatherLibraries();
            for (Editor e : editors) {
                e.rebuildCoresMenu();
                e.rebuildBoardsMenu();
                e.rebuildImportMenu();
                e.rebuildExamplesMenu();
                e.rebuildPluginsMenu();
            }
        }

        @Override
        protected void process(java.util.List<Integer> pct) {
            int p = pct.get(pct.size() - 1);
            if (p == -1) {
                activeEditor.status.progress(Translate.t("Examining..."));
                activeEditor.status.progressIndeterminate(Translate.t("Examining..."));
            } else {
                activeEditor.status.progress(Translate.t("Installing..."));
                activeEditor.status.progressUpdate(p);
            }
        }
    };

    public void updateProgress(final int perc)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                activeEditor.status.progressUpdate(perc);
            }
        });
    }

    public static void handleAddBoards() 
    {
        File inputFile = openFileDialog(Translate.t("Add Boards..."), "zip");

        if (inputFile == null) {
            return;
        }

        if (!inputFile.exists()) {
            System.err.println(inputFile.getName() + ": " + Translate.t("not found"));
            return;
        }

        final File bf = getUserBoardsFolder();
        new ZipExtractor(inputFile, bf).execute();
    }

    public static void handleAddCore()
    {
        File inputFile = openFileDialog(Translate.t("Add Core..."), "jar");

        if (inputFile == null) {
            return;
        }

        if (!inputFile.exists()) {
            System.err.println(inputFile.getName() + ": " + Translate.t("not found"));
            return;
        }

        final File bf = getUserCoresFolder();
        if (!bf.exists()) {
            bf.mkdirs();
        }

        try {
            JarFile jf = new JarFile(inputFile);
            Manifest manifest = jf.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();

            String plat = manifestContents.getValue("Platform");
            System.err.println("Core is for " + plat);

            if (!(
                plat.equals(getOSFullName()) ||
                plat.equals(getOSName()) ||
                plat.equals("any")
            )) {
                Base.showWarning(Translate.t("Incompatible Core"), Translate.w("The core you selected is for %1.  You are running on %2.", 40, "\n", plat, getOSFullName()), null);
                return;
            }

            new ZipExtractor(inputFile, bf).execute();
        } catch (Exception e) {
            error(e);
        }
    }

    public static void handleInstallPlugin() {
        handleInstallPlugin((File)null);
    }

    public static void handleInstallPlugin(File inputFile)
    {
        if (inputFile == null) {
            inputFile = openFileDialog(Translate.t("Add Plugin..."), "zip,jar");
        }

        if (inputFile == null) {
            return;
        }

        if (!inputFile.exists()) {
            System.err.println(inputFile.getName() + ": " + Translate.t("not found"));
            return;
        }
        File bf = getUserPluginsFolder();
        if (!bf.exists()) {
            bf.mkdirs();
        }
        if (bf.getName().toLowerCase().endsWith(".zip")) {
            new ZipExtractor(inputFile, bf).execute();
        } else {
            File d = getUserPluginsFolder();
            if (!d.exists()) {
                d.mkdirs();
            }
            File dst = new File(d, inputFile.getName());
            try {
                copyFile(inputFile, dst);
            } catch (Exception e) {
                System.err.println(e);
            }
        }
        loadPlugins();
        for (Editor e : editors) {
            e.rebuildPluginsMenu();
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

        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isDirectory()) {
                loadPluginsFromFolder(contents[i]);
            } else if (contents[i].getName().toLowerCase().endsWith(".jar")) {
                loadPlugin(contents[i]);
            }
        }
    }
    

    public static void loadPlugin(File jar)
    {
        try {
            URL[] urlList = new URL[1];
            urlList[0]  = jar.toURI().toURL();

            URLClassLoader loader = new URLClassLoader(urlList);

            String className = null;

            JarFile jf = new JarFile(jar);
            Manifest manifest = jf.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();

            className = manifestContents.getValue("Main-Class");
            if (className == null) {
                className = findClassInZipFile(jar);

                if (className == null) {
                    return;
                }
            }

            Map pluginInfo = new LinkedHashMap();
            pluginInfo.put("version", manifestContents.getValue("Version"));
            pluginInfo.put("compiled", manifestContents.getValue("Compiled"));
            pluginInfo.put("jarfile", jar.getAbsolutePath());
            pluginInfo.put("shortcut", manifestContents.getValue("Shortcut"));
            pluginInfo.put("modifier", manifestContents.getValue("Modifier"));
            Plugin op = plugins.get(className);
            if (op != null) {
                String oldVersion = op.getVersion();
                String newVersion = manifestContents.getValue("Version");
                int diff = oldVersion.compareTo(newVersion);
                if (diff != -1) { // New version no newer than old version
                    return;
                }
            }
                

            Class<?> pluginClass;
            try {
                pluginClass = Class.forName(className, true, loader);
            } catch (Exception ex) {
                error(ex);
                return;
            }
            Plugin plugin = (Plugin) pluginClass.newInstance();

            // If the setInfo method doesn't exist we don't care.
            try {
                plugin.setInfo(pluginInfo);
            } catch (Exception blah) {
            }

            try {
                plugin.setLoader(loader);
            } catch (Exception blah) {
            }
            plugins.put(className, plugin);
        } catch (Exception e) {
            System.err.println(e.getMessage());
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
        for (Editor ed : editors) {
            ed.applyPreferences();
        }
    }

    public static void reloadPlugins() {
        loadPlugins();
        for (Editor e : editors) {
            e.rebuildPluginsMenu();
        }
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

    } else if (isLinux()) {
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
        for (Editor ed : editors) {
            ed.message(e + "\n", 2);
        }
        System.err.println(e);
    }

    public static void error(Throwable e) {

        for (Editor ed : editors) {
            ed.message(e.getMessage() + "\n", 2);
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        System.err.println(sw.toString());
    }
}


