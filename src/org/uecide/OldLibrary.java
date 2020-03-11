package org.uecide;

import java.io.File;
import java.util.ArrayList;

public class OldLibrary extends Library {
    public OldLibrary(File location, int priority) throws LibraryFormatException {
        super(location, priority);

        File parent = location.getParentFile();
        Core c = UECIDE.getCore(parent.getName());
        if (c == null) {
            setCore("any");
        } else {
            setCore(c);
        }

        if (c == null) {
            setCategory(parent.getName());
        } else {
            setCategory(parent.getParentFile().getName());
        }
    }

    @Override
    public ArrayList<File> getHeaderFiles() {
        ArrayList<File> out = new ArrayList<File>();
        File[] list = getFolder().listFiles();
        for (File f : list) {
            if (f.isDirectory()) continue;
            if (f.getName().endsWith(".h")) {
                out.add(f);
            }
        }
        return out;
    }
}
