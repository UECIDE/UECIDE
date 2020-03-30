package org.uecide;

import java.util.ArrayList;
import java.util.TreeMap;
import java.io.File;
import java.io.IOException;

public class LibraryManager {

    static TreeMap<File, Integer> libraryLocations = new TreeMap<File, Integer>();
    static TreeMap<File, Library> libraryIndex = new TreeMap<File, Library>();
    static TreeMap<String, ArrayList<Library>> libraries = new TreeMap<String, ArrayList<Library>>();

    /* Delete all the libraries from the system */
    public static void clearLibraries() {
        libraries.clear();
        libraryIndex.clear();
    }

    /* Add a library from a location */
    public static boolean addLibrary(File location, int priority) {
        // Test for a 1.5x library
        try {
            File props = new File(location, "library.properties");
            if (props.exists()) {
                Library l = new NewLibrary(location, priority);
                String h = l.getMainHeader();
                addLibrary(h, l);
                return true;
            } else {
                File header = new File(location, location.getName() + ".");
                if (header.exists()) {
                    Library l = new OldLibrary(location, priority);
                    String h = l.getMainHeader();
                    addLibrary(h, l);
                    return true;
                }
            }
        } catch (LibraryFormatException ex) {
            Debug.exception(ex);
            System.err.println(location.getAbsolutePath() + ": " + ex.getMessage());
        }
        return false;
    }

    /* Add a library to the list of libraries associated with a specific header */
    public static void addLibrary(String header, Library l) {
        ArrayList<Library> liblist = libraries.get(header);

        if (liblist == null) {
            liblist = new ArrayList<Library>();
        }

        if (libraryIndex.get(l.getFolder()) == null) {
            libraryIndex.put(l.getFolder(), l);
        }

        if (liblist.indexOf(l) > -1) {
            return; // Already there
        }

        liblist.add(l);
        libraries.put(header, liblist);
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

    /* Get a library by the header name. Returns the highest priority one */
    public static Library getLibraryByName(String header, Core core) {
        Library foundLib = null;
        int prio = -1;

        ArrayList<Library> libs = libraries.get(header);
        if (libs == null) return null;

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
    public static boolean recursivelyScanLibraries(File path, int priority) {
        if (path == null) return false;
        if (!path.exists()) return false;
        if (!path.isDirectory()) return false;
        File[] dirs = path.listFiles();

        for (File dir : dirs) {
            if (!dir.isDirectory()) continue;

            // If we successfully load this folder as a library then we don't need to recurse any deeper
            if (addLibrary(dir, priority)) continue;
    
            // Otherwise let's delve deeper
            recursivelyScanLibraries(dir, priority);

        }
        return true;
    }

    /* Add a path to the list of locations to scan for libraries */
    public static void addLibraryLocation(File l, int p) {
        libraryLocations.put(l, p);
    }

    /* Completely rebuild the whole library tree system */
    public static void rescanAllLibraries() {
        clearLibraries();
        for (File f : libraryLocations.keySet()) {
            Integer priority = libraryLocations.get(f);
            recursivelyScanLibraries(f, priority);
        }
    }

}
