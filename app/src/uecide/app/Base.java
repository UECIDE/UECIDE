/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-10 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package uecide.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import uecide.plugin.*;


import javax.swing.*;

import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import uecide.app.debug.Board;
import uecide.app.debug.Core;
import processing.core.*;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * The base class for the main uecide.application.
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading
 * files and images, etc) that comes from that.
 */
public class Base {
	
  //private static Logger logger = Logger.getLogger(Base.class.getName());
    static Logger logger = Logger.getLogger(Base.class.getName());

	
    public static int REVISION = 23;
    /** This might be replaced by main() if there's a lib/version.txt file. */
    public static String VERSION_NAME = "0023";
    /** Set true if this a proper release rather than a numbered revision. */
    public static boolean RELEASE = false;

    public static ArrayList<Process> processes = new ArrayList<Process>();
  
  
    static HashMap<Integer, String> platformNames = new HashMap<Integer, String>();
    static {
        platformNames.put(PConstants.WINDOWS, "windows");
        platformNames.put(PConstants.MACOSX, "macosx");
        platformNames.put(PConstants.LINUX, "linux");
    }

    static HashMap<String, Integer> platformIndices = new HashMap<String, Integer>();
    static {
        platformIndices.put("windows", PConstants.WINDOWS);
        platformIndices.put("macosx", PConstants.MACOSX);
        platformIndices.put("linux", PConstants.LINUX);
    }
    static Platform platform;

    static private boolean commandLine;

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
  
    public static HashMap<String, Board> boards;
    public static HashMap<String, Core> cores;
    public static HashMap<String, Plugin> plugins;

    // Location for untitled items
    static File untitledFolder;

    static ArrayList<Editor> editors = new ArrayList<Editor>();
    public static Editor activeEditor;

    public static void main(String args[]) {
        try {
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.DEBUG);

            JarFile myself = new JarFile("lib/uecide.jar");
            Manifest manifest = myself.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();

            VERSION_NAME = manifestContents.getValue("Version");
            REVISION = Integer.parseInt(manifestContents.getValue("Compiled"));

            RELEASE = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // setup the theme coloring fun
        Theme.init();

        initPlatform();

        // Use native popups so they don't look so crappy on osx
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        // Don't put anything above this line that might make GUI,
        // because the platform has to be inited properly first.

        // run static initialization that grabs all the prefs
        Preferences.init(null);

        // Set the look and feel before opening the window
        try {
            platform.setLookAndFeel();
        } catch (Exception e) {
            String mess = e.getMessage();
            if (mess.indexOf("ch.randelshofer.quaqua.QuaquaLookAndFeel") == -1) {
                System.err.println("Non-fatal error while setting the Look & Feel.");
                System.err.println("The error message follows, however " + Theme.get("product.cap") + " should run fine.");
                System.err.println(mess);
            }
        }

        // Create a location for untitled sketches
        untitledFolder = createTempFolder("untitled");
        untitledFolder.deleteOnExit();

        new Base(args);
    }


    static protected void setCommandLine() {
        commandLine = true;
    }


    static protected boolean isCommandLine() {
        return commandLine;
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
                            Theme.get("product.cap") + " requires a full JDK (not just a JRE)\n" +
                            "to run. Please install JDK 1.5 or later.\n" +
                            "More information can be found in the reference.", cnfe);
        }
    }


    public Base(String[] args) {
        platform.init(this);

        // Get paths for the libraries and examples in the Processing folder
        //String workingDirectory = System.getProperty("user.dir");

        examplesFolder = getContentFile("examples");
        toolsFolder = getContentFile("tools");

        // Get the sketchbook path, and make sure it's set properly
        String sketchbookPath = Preferences.get("sketchbook.path");

//        Translate.load("swedish");

        // If a value is at least set, first check to see if the folder exists.
        // If it doesn't, warn the user that the sketchbook folder is being reset.
        if (sketchbookPath != null) {
            File skechbookFolder = new File(sketchbookPath);
            if (!skechbookFolder.exists()) {
                Base.showWarning("Sketchbook folder disappeared",
                         "The sketchbook folder no longer exists.\n" +
                         Theme.get("product.cap") + " will switch to the default sketchbook\n" +
                         "location, and create a new sketchbook folder if\n" +
                         "necessary. " + Theme.get("product.cap") + " will then stop talking about\n" +
                         "himself in the third person.", null);
                sketchbookPath = null;
            }
        }

        // If no path is set, get the default sketchbook folder for this platform
        if (sketchbookPath == null) {
            File defaultFolder = getDefaultSketchbookFolder();
            Preferences.set("sketchbook.path", defaultFolder.getAbsolutePath());
            if (!defaultFolder.exists()) {
                defaultFolder.mkdirs();
            }
        }
    
        cores = new HashMap<String, Core>();
        boards = new HashMap<String, Board>();
        plugins = new HashMap<String, Plugin>();

        loadCores();
        if (cores.size() == 0) {
            System.err.println("You have no cores installed!");
            System.err.println("Please install at least one core.");
            return;
        }
        loadBoards();
        if (cores.size() == 0) {
            System.err.println("You have no boards installed!");
            System.err.println("Please install at least one board.");
            return;
        }

        loadPlugins();
        gatherLibraries();
        initMRU();

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
                    e.printStackTrace();
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
    }

    public static void initMRU()
    {
        MRUList = new ArrayList<File>();
        for (int i = 0; i < 10; i++) {
            if (Preferences.get("sketch.mru." + i) != null) {
                File f = new File(Preferences.get("sketch.mru." + i));
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
                Preferences.set("sketch.mru." + i, MRUList.get(i).getAbsolutePath());
            } else {
                Preferences.unset("sketch.mru." + i);
            }
        }
        Preferences.save();
        rebuildSketchbookMenus();
    }

    public static Board getDefaultBoard() {
        Board tb;
        String prefsBoard = Preferences.get("board");
        String[] entries;

        tb = boards.get(prefsBoard);
        if (tb != null) {
            return tb;
        }

        entries = (String[]) boards.keySet().toArray(new String[0]);
        tb = boards.get(entries[0]);
        if (tb != null) {
            return tb;
        }
        logger.debug("Base: Warning - no boards found");
        return null;
    }

    private static void loadCores() {
        cores.clear();
        loadCoresFromFolder(new File(getHardwareFolder(),"cores"));
        loadCoresFromFolder(new File(getSketchbookFolder(),"cores"));
    }

    private static void loadCoresFromFolder(File folder) {
        if (!folder.isDirectory()) 
            return;
        String cl[] = folder.list();
 
        for (int i = 0; i < cl.length; i++) {
            if (cl[i].charAt(0) == '.')
                continue;
            File cdir = new File(folder, cl[i]);
            if (cdir.isDirectory()) {
                Core newCore = new Core(cdir);
                if (newCore.isValid()) {
                    cores.put(newCore.getName(), newCore);
                }
            }
        }
    }

    private static void loadBoards() {
        boards.clear();
        loadBoardsFromFolder(new File(getHardwareFolder(), "boards"));
        loadBoardsFromFolder(new File(getSketchbookFolder(), "boards"));
    }

    private static void loadBoardsFromFolder(File folder) {
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


    // Because of variations in native windowing systems, no guarantees about
    // changes to the focused and active Windows can be made. Developers must
    // never assume that this Window is the focused or active Window until this
    // Window receives a WINDOW_GAINED_FOCUS or WINDOW_ACTIVATED event.
    protected static void handleActivated(Editor whichEditor) {
        activeEditor = whichEditor;

        // set the current window to be the console that's getting output
    }

    protected static int[] nextEditorLocation() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int defaultWidth = Preferences.getInteger("editor.window.width.default");
        int defaultHeight = Preferences.getInteger("editor.window.height.default");

        if (activeEditor == null) {
            // If no current active editor, use default placement
            return new int[] {
                (screen.width - defaultWidth) / 2,
                (screen.height - defaultHeight) / 2,
                defaultWidth, defaultHeight, 0
            };

        } else {
            // With a currently active editor, open the new window
            // using the same dimensions, but offset slightly.
            final int OVER = 50;
            // In release 0160, don't
            //location = activeEditor.getPlacement();
            Editor lastOpened = editors.get(editors.size() - 1);
            int[] location = lastOpened.getPlacement();
            // Just in case the bounds for that window are bad
            location[0] += OVER;
            location[1] += OVER;

            if (location[0] == OVER ||
                location[2] == OVER ||
                location[0] + location[2] > screen.width ||
                location[1] + location[3] > screen.height
            ) {
                // Warp the next window to a randomish location on screen.
                return new int[] {
                    (int) (Math.random() * (screen.width - defaultWidth)),
                    (int) (Math.random() * (screen.height - defaultHeight)),
                    defaultWidth, defaultHeight, 0
                };
            }
            return location;
        }
    }


    // .................................................................


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
            Translate.t("Open %1 sketch...", Theme.get("product.cap")),
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
        editor.internalCloseRunner();

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
                    "<p>" + Translate.t("Closing the last open sketch will quit %1.", Theme.get("product.cap"));

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
            Preferences.save();

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
            // make sure running sketches close before quitting
            for (Editor editor : editors) {
                editor.internalCloseRunner();
            }
            // Save out the current prefs state
            Preferences.save();

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

    public static HashMap<String, File> getLibraryCollection(String name) {
        return libraryCollections.get(name);
    }

    public static HashMap<String, HashMap<String, File>> libraryCollections;

    public static void gatherLibraries() {
        libraryCollections = new HashMap<String, HashMap<String, File>>();
        libraryCollections.put("global", loadLibrariesFromFolder(getContentFile("libraries"))); // Global libraries
        String[] corelist = (String[]) cores.keySet().toArray(new String[0]);

        for (String core : corelist) {
            libraryCollections.put(core, loadLibrariesFromFolder(cores.get(core).getLibraryFolder())); // Core libraries
        }

        libraryCollections.put("sketchbook", loadLibrariesFromFolder(new File(getSketchbookFolder(), "libraries"))); // Contributed libraries
    }

    public static HashMap<String, File> loadLibrariesFromFolder(File folder) {
        HashMap out = new HashMap<String, File>();
        if (!folder.exists()) {
            return out;
        }
        File[] list = folder.listFiles();
        for (File f : list) {
            if (f.isDirectory()) {
                File header = new File(f, f.getName() + ".h");
                if (header.exists()) {
                    out.put(header.getName(), f);
                }
            }
        }
        return out;
    }

    /**
     * Show the About box.
     */
    public static void handleAbout() {
        final Image image = Base.getLibImage("theme/about.png", activeEditor);
        final Window window = new Window(activeEditor) {
            public void paint(Graphics g) {
                int x = Integer.parseInt(Theme.get("about.version.x"));
                int y = Integer.parseInt(Theme.get("about.version.y"));

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

                g.setFont(Theme.getFont("about.version.font"));
                g.setColor(Theme.getColor("about.version.color"));
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


    public static String osNameFull() {
        return osName() + "_" + System.getProperty("os.arch");
    }
    
    public static String osName() {
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
    * Map a platform constant to its name.
    * @param which PConstants.WINDOWS, PConstants.MACOSX, PConstants.LINUX
    * @return one of "windows", "macosx", or "linux"
    */
    public static String getPlatformName(int which) {
        return platformNames.get(which);
    }


    public static int getPlatformIndex(String what) {
        Integer entry = platformIndices.get(what);
        return (entry == null) ? -1 : entry.intValue();
    }


    // These were changed to no longer rely on PApplet and PConstants because
    // of conflicts that could happen with older versions of core.jar, where
    // the MACOSX constant would instead read as the LINUX constant.


    /**
    * returns true if Processing is running on a Mac OS X machine.
    */
    public static boolean isMacOS() {
        //return PApplet.platform == PConstants.MACOSX;
        return System.getProperty("os.name").indexOf("Mac") != -1;
    }


    /**
    * returns true if running on windows.
    */
    public static boolean isWindows() {
        //return PApplet.platform == PConstants.WINDOWS;
        return System.getProperty("os.name").indexOf("Windows") != -1;
    }


    /**
    * true if running on linux.
    */
    public static boolean isLinux() {
        //return PApplet.platform == PConstants.LINUX;
        return System.getProperty("os.name").indexOf("Linux") != -1;
    }


    // .................................................................


    public static File getSettingsFolder() {
        File settingsFolder = null;

        String preferencesPath = Preferences.get("settings.path");
        if (preferencesPath != null) {
            settingsFolder = new File(preferencesPath);

        } else {
            try {
                settingsFolder = platform.getSettingsFolder();
            } catch (Exception e) {
                showError(Translate.t("Problem getting data folder"),
                Translate.t("Error getting the %1 data folder.", Theme.get("product.cap")), e);
            }
        }

        // create the folder if it doesn't exist already
        if (!settingsFolder.exists()) {
            if (!settingsFolder.mkdirs()) {
                showError(Translate.t("Settings issues"),
                        Translate.t("%1 cannot run because it could not create a folder to store your settings.", Theme.get("product.cap")), null);
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
            e.printStackTrace();
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
        return new File(Preferences.get("sketchbook.path"));
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
            Translate.t("%1 cannot run because it could not create a folder to store your sketchbook.", Theme.get("product.cap")), null);
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

        if (commandLine) {
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

        if (commandLine) {
            System.out.println(title + ": " + message);

        } else {
            System.out.println(title + ": " + message);
            JOptionPane.showMessageDialog(new Frame(), message, title,
            JOptionPane.WARNING_MESSAGE);
        }
        if (e != null) e.printStackTrace();
    }


    /**
    * Show an error message that's actually fatal to the program.
    * This is an error that can't be recovered. Use showWarning()
    * for errors that allow P5 to continue running.
    */
    public static void showError(String title, String message, Throwable e) {
        if (title == null) title = Translate.t("Error");

        if (commandLine) {
            System.err.println(title + ": " + message);

        } else {
            JOptionPane.showMessageDialog(new Frame(), message, title,
            JOptionPane.ERROR_MESSAGE);
        }
        if (e != null) e.printStackTrace();
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


    /**
    * Return an InputStream for a file inside the Processing lib folder.
    */
    public static InputStream getLibStream(String filename) throws IOException {
        return new FileInputStream(new File(getContentFile("lib"), filename));
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


    /**
    * Same as PApplet.loadBytes(), however never does gzip decoding.
    */
    public static byte[] loadBytesRaw(File file) throws IOException {
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
    }



    /**
    * Read from a file with a bunch of attribute/value pairs
    * that are separated by = and ignore comments with #.
    */
    public static HashMap<String,String> readSettings(File inputFile) {
        HashMap<String,String> outgoing = new HashMap<String,String>();
        if (!inputFile.exists()) return outgoing;  // return empty hash

        String lines[] = PApplet.loadStrings(inputFile);
        for (int i = 0; i < lines.length; i++) {
            int hash = lines[i].indexOf('#');
            String line = (hash == -1) ?
            lines[i].trim() : lines[i].substring(0, hash).trim();
            if (line.length() == 0) continue;

            int equals = line.indexOf('=');
            if (equals == -1) {
                System.err.println("ignoring illegal line in " + inputFile);
                System.err.println("  " + line);
                continue;
            }
            String attr = line.substring(0, equals).trim();
            String valu = line.substring(equals + 1).trim();
            outgoing.put(attr, valu);
        }
        return outgoing;
    }


    public static void copyFile(File sourceFile, File targetFile) throws IOException {
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
    }


    /**
    * Grab the contents of a file as a string.
    */
    public static String loadFile(File file) throws IOException {
        String[] contents = PApplet.loadStrings(file);
        if (contents == null) return null;
        return PApplet.join(contents, "\n");
    }


    /**
    * Spew the contents of a String object out to a file.
    */
    public static void saveFile(String str, File file) throws IOException {
        File temp = File.createTempFile(file.getName(), null, file.getParentFile());
        PApplet.saveStrings(temp, new String[] { str });
        if (file.exists()) {
            boolean result = file.delete();
            if (!result) {
                throw new IOException(
                    Translate.t("Could not remove old version of %1", file.getAbsolutePath()));
            }
        }
        boolean result = temp.renameTo(file);
        if (!result) {
            throw new IOException(
                    Translate.t("Could not replace %1", file.getAbsolutePath()));
        }
    }


    /**
    * Copy a folder from one place to another. This ignores all dot files and
    * folders found in the source directory, to avoid copying silly .DS_Store
    * files and potentially troublesome .svn folders.
    */
    public static void copyDir(File sourceDir, File targetDir) throws IOException {
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
                if (!Preferences.getBoolean("compiler.save_build_files")) {
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

        extractZip(inputFile.getAbsolutePath(), getSketchbookLibrariesFolder().getAbsolutePath());
        gatherLibraries();
        for (Editor e : editors) {
            e.populateMenus();
        }
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
        boolean foundCPP = false;
        boolean foundHeader = false;

        for (int i=0; i<fileList.size(); i++) {
            String entry = fileList.get(i);
            if (entry.endsWith(".h")) {
                String[] bits = entry.split("/");
                if (bits[1].equals(bits[0] + ".h")) {
                    foundHeader = true;
                }
            }
            if (entry.endsWith(".cpp")) {
                String[] bits = entry.split("/");
                if (bits[1].equals(bits[0] + ".cpp")) {
                    foundCPP = true;
                }
            }
        }
        return (foundHeader && foundCPP);
    }

    public static int countZipEntries(String inputFile)
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

    public static void extractZip(final String inputFile, final String destination)
    {
            activeEditor.status.progress(Translate.t("Extracting..."));
        
            byte[] buffer = new byte[1024];
            ArrayList<String> fileList = new ArrayList<String>();
            File slf = new File(destination);
            int files = countZipEntries(inputFile);
            if (files == -1) {
                return;
            }
            int done = 0;
            try {
                ZipInputStream zis = new ZipInputStream(new FileInputStream(inputFile));
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    String fileName = ze.getName();
                    File newFile = new File(slf, fileName);

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
                    activeEditor.status.progressUpdate((done * 100) / files);
                    ze = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
            } catch (Exception e) {
                activeEditor.status.progressNotice(Translate.t("Install failed"));
                System.err.println(e.getMessage());
                return;
            }
            activeEditor.status.progressNotice(Translate.t("Installed."));
            activeEditor.status.unprogress();
        
    }

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

        File bf = new File(getSketchbookFolder(), "boards");
        extractZip(inputFile.getAbsolutePath(), bf.getAbsolutePath());
        loadBoards();
        for (Editor e : editors) {
            e.rebuildBoardsMenu();
            e.rebuildImportMenu();
            e.rebuildExamplesMenu();
        }
    }

    public static void handleAddCore()
    {
        File inputFile = openFileDialog(Translate.t("Add Core..."), "zip");

        if (inputFile == null) {
            return;
        }

        if (!inputFile.exists()) {
            System.err.println(inputFile.getName() + ": " + Translate.t("not found"));
            return;
        }

        File bf = new File(getSketchbookFolder(), "cores");
        if (!bf.exists()) {
            bf.mkdirs();
        }
        extractZip(inputFile.getAbsolutePath(), bf.getAbsolutePath());
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

    public static void handleInstallPlugin()
    {
        File inputFile = openFileDialog(Translate.t("Add Plugin..."), "zip,jar");

        if (inputFile == null) {
            return;
        }

        if (!inputFile.exists()) {
            System.err.println(inputFile.getName() + ": " + Translate.t("not found"));
            return;
        }
        File bf = new File(getSketchbookFolder(), "plugins");
        if (!bf.exists()) {
            bf.mkdirs();
        }
        if (bf.getName().toLowerCase().endsWith(".zip")) {
            extractZip(inputFile.getAbsolutePath(), bf.getAbsolutePath());
        } else {
            File d = new File(getSketchbookFolder(), "plugins");
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

        File pf;

        pf = getContentFile("plugins");
        if (pf != null) loadPluginsFromFolder(pf);

        String[] entries = (String[]) cores.keySet().toArray(new String[0]);

        for (int i = 0; i < entries.length; i++) {
            Core c = cores.get(entries[i]);
            pf = new File(c.getFolder(), c.get("library.plugins", "plugins"));
            if (pf != null) loadPluginsFromFolder(pf);
        }

        pf = new File(getSketchbookFolder(), "plugins");
        if (pf != null) loadPluginsFromFolder(pf);
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

            if (className.startsWith("processing.")) {
                System.err.println(Translate.t("Plugin %1 is not compatible with this version. Please upgrade the plugin.", jar.getName()));
                return;
            }

            Class<?> pluginClass;
            try {
                pluginClass = Class.forName(className, true, loader);
            } catch (Exception ex) {
                ex.printStackTrace();
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
            e.printStackTrace();
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

}
