package org.uecide;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.File;
import java.io.IOException;

public class LibraryManager {

    static class LibraryLocation {
        String name;
        int priority;
        File location;
        public LibraryLocation(File loc, int p, String n) {
            location = loc;
            name = n;
            priority = p;
        }
        public File getLocation() { return location; }
        public String getName() { return name; }
        public int getPriority() { return priority; }
    }

    static TreeMap<File, LibraryLocation> libraryLocations = new TreeMap<File, LibraryLocation>();
    static TreeMap<File, Library> libraryIndex = new TreeMap<File, Library>();
    static TreeMap<String, ArrayList<Library>> libraries = new TreeMap<String, ArrayList<Library>>();

    /* Delete all the libraries from the system */
    public static void clearLibraries() {
        libraries.clear();
        libraryIndex.clear();
    }

    /* Add a library from a location */
    public static boolean addLibrary(File location, int priority, String catname) {
        // Test for a 1.5x library
        try {
            File props = new File(location, "library.properties");
            if (props.exists()) {
                Library l = new NewLibrary(location, priority);
                String h = l.getMainHeader();
                addLibrary(h, l);
                return true;
            } else {
                File header = new File(location, location.getName() + ".h");
                if (header.exists()) {
                    Library l = new OldLibrary(location, priority, catname);
                    String h = l.getMainHeader();
                    addLibrary(h, l);
                    return true;
                }
            }
        } catch (LibraryFormatException ex) {
            Debug.exception(ex);
        }
        return false;
    }

    /* Add a library to the list of libraries associated with a specific header */
    public static void addLibrary(String header, Library l) {

        if (libraryIndex.get(l.getFolder()) == null) {
            libraryIndex.put(l.getFolder(), l);
        }

        if (header != null) {
            ArrayList<Library> liblist = libraries.get(header);
            if (liblist == null) {
                liblist = new ArrayList<Library>();
            }

            if (liblist.indexOf(l) > -1) {
                return; // Already there
            }

            liblist.add(l);
            libraries.put(header, liblist);
        }
    }

    /* Take a File path and find the library related to it */
    public static Library getLibraryByPath(File path) throws IOException {
        for (File f : libraryIndex.keySet()) {
            if (f.getCanonicalPath().equals(path.getCanonicalPath())) {
                return libraryIndex.get(f);
            }
        }
        return null;
    }

    public static ArrayList<Library> findLibrariesThatHaveHeader(String header) {
        ArrayList<Library> out = new ArrayList<Library>();
        for (Library lib : libraryIndex.values()) {
            if (lib.hasHeader(header)) {
                out.add(lib);
            }
        }
        return out;
    }

    /* Get a library by the header name. Returns the highest priority one */
    public static Library getLibraryByName(String header, Core core) {
        Library foundLib = null;
        int prio = -1;

        // First let's try finding the library by its main header (if it has one).
        ArrayList<Library> libs = libraries.get(header);

        // If it's not found then get all the libraries that contain the header
        if (libs == null) {
            libs = findLibrariesThatHaveHeader(header);
        }

        // If it's still not found then give up.
        if (libs == null) {
            return null;
        }
        if (libs.size() == 0) {
            return null;
        }
 
        for (Library lib : libs) {
            if (lib.worksWith(core)) {
                if (lib.getPriority() > prio) {
                    prio = lib.getPriority();
                    foundLib = lib;
                }
            }
        }
        
        return foundLib;
    }

    /* Scan a file tree for libraries, adding them to the system as we go */
    public static boolean recursivelyScanLibraries(File path, int priority, String name) {
        if (path == null) return false;
        if (!path.exists()) return false;
        if (!path.isDirectory()) return false;
        File[] dirs = path.listFiles();

        for (File dir : dirs) {
            if (!dir.isDirectory()) continue;

            // If we successfully load this folder as a library then we don't need to recurse any deeper
            if (addLibrary(dir, priority, name)) continue;
    
            // Otherwise let's delve deeper
            recursivelyScanLibraries(dir, priority, name);
        }
        return true;
    }

    /* Add a path to the list of locations to scan for libraries */
    public static void addLibraryLocation(File l, int p, String n) {
        LibraryLocation loc = new LibraryLocation(l, p, n);
        libraryLocations.put(l, loc);
    }

    /* Completely rebuild the whole library tree system */
    public static void rescanAllLibraries() {
        clearLibraries();
        for (LibraryLocation loc : libraryLocations.values()) {
            recursivelyScanLibraries(loc.getLocation(), loc.getPriority(), loc.getName());
        }
    }

    public static void dumpLibraryList(Context ctx) {
        ctx.heading("Library listing");
        for (String header : libraries.keySet()) {
            ArrayList<Library> libs = libraries.get(header);
            ctx.bullet(header);
            for (Library l : libs) {
                ctx.bullet2(l.getFolder() + " @ " + l.getPriority());
            }
        }
    }

    public static TreeSet<String> getCategories(Core c) {
        TreeSet<String> cats = new TreeSet<String>();
        for (String libname : libraries.keySet()) {
            Library lib = getLibraryByName(libname, c);
            if (lib == null) continue;
            String cat = lib.getCategory();
            if (!cats.contains(cat)) {
                cats.add(cat);
            }
        }
        return cats;
    }

    public static TreeSet<Library> getLibrariesForCategory(Core c, String cat) {
        TreeSet<Library> libs = new TreeSet<Library>();
        for (String libname : libraries.keySet()) {
            Library lib = getLibraryByName(libname, c);
            if (lib == null) continue;
            if (lib.getCategory().equals(cat)) {
                libs.add(lib);
            }
        }
        return libs;
    }

    public static void clearLocations() {
        libraryLocations.clear();
    }

    public static void updateSketchLibraries(Context ctx) {
        libraries.remove("Sketch");
        recursivelyScanLibraries(new File(ctx.getSketch().getFolder(), "libraries"), 1000, "Sketch");
    }

}
