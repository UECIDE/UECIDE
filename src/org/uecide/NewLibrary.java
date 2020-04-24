package org.uecide;

import java.io.File;
import java.util.ArrayList;

public class NewLibrary extends Library {
    PropertyFile properties;
    File source = null;

    public NewLibrary(File location, int priority) throws LibraryFormatException {
        super(location, priority);

        File propertiesFile = new File(location, "library.properties");
        if (!propertiesFile.exists()) {
            throw new LibraryFormatException("Can't find library.properties");
        }

        properties = new PropertyFile(propertiesFile);

        setCategory(properties.get("category"));

        source = new File(location, "src");
        boolean recurse = true;
        // If there is no source folder then it may be a flat format one.
        if (!source.exists()) {
            source = getFolder();
            recurse = false;
        }
        if (!source.isDirectory()) throw new LibraryFormatException("src is not a folder");

        mainInclude = null;
        ArrayList<File> files = FileManager.findFilesInFolder(source, recurse, FileType.CSOURCE, FileType.CPPSOURCE, FileType.HEADER, FileType.LIBRARY);
        for (File f : files) {
            addSourceFile(f);
            if (f.getName().equals(location.getName() + ".h")) {
                mainInclude = f;
            }
        }

        if (mainInclude == null) {
            System.err.println("Error loading " + location);
            throw new LibraryFormatException("No matching header file found");
        }
        
    }

    @Override
    public Version getVersion() {
        return new Version(properties.get("version"));
    }

    @Override
    public boolean worksWith(Core c) {
        File location = getFolder();
        if (location.getParentFile().getName().equals(c.getName())) return true;
        String archlist = properties.get("architectures");
        String[] arches = archlist.split(",");
        for (String arch : arches) {
            if (arch != null) {
                if (arch.equals("*")) return true;
                if (arch.equals(c.getName())) return true;
                if (arch.equals(c.getFamily())) return true;
            }
        }
        return false;
    }

    @Override 
    public String getName() {
        return properties.get("name");
    }

    @Override
    public ArrayList<File> getIncludeFolders() {
        ArrayList<File> list = new ArrayList<File>();
        list.add(source);
        File u = new File(source, "utility");
        if (u.exists()) {
            list.add(u);
        }
        return list;
    }

}
