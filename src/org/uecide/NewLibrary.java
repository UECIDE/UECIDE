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
        if (!source.exists()) throw new LibraryFormatException("src folder doesn't exist");
        if (!source.isDirectory()) throw new LibraryFormatException("src is not a folder");
    }

    @Override
    public Version getVersion() {
        return new Version(properties.get("version"));
    }

    @Override
    public ArrayList<File> getHeaderFiles() {
        ArrayList<File> out = new ArrayList<File>();
        File[] list = source.listFiles();
        for (File f : list) {
            if (f.isDirectory()) continue;
            if (f.getName().endsWith(".h")) {
                out.add(f);
            }
        }
        return out;
    }
}
