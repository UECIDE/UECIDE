package org.uecide;

import java.io.File;
import java.util.ArrayList;

public class OldLibrary extends Library {
    public OldLibrary(File location, int priority, String catname) throws LibraryFormatException {
        super(location, priority);
        setCategory(catname);

        File parent = location.getParentFile();
        Core c = Core.getCore(parent.getName());
        if (c == null) {
            setCore("any");
        } else {
            setCore(c);
        }

        if (c != null) {
            setCategory(parent.getParentFile().getName());
        }

        File[] files = location.listFiles();
        mainInclude = null;
        for (File f : files) {
            if (f.getName().startsWith(".")) continue;
            addSourceFile(f);
            if (f.getName().equals(location.getName() + ".h")) {
                mainInclude = f;
            }
        }

        if (mainInclude == null) {
            throw new LibraryFormatException("No matching header found");
        }
    }

    @Override
    public boolean worksWith(Core c) {
        File location = getFolder();
        File par = location.getParentFile();
        Core testCore = Core.getCore(par.getName());
        if (testCore == null) { // I don't know that it *doesn't* work...
            return true;
        }

        if (testCore == c) { // Match
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return getFolder().getName();
    }

    @Override
    public ArrayList<File> getIncludeFolders() {
        ArrayList<File> list = new ArrayList<File>();
        list.add(getFolder());
        File u = new File(getFolder(), "utility");
        if (u.exists()) {
            list.add(u);
        }
        u = new File(getFolder(), "src");
        if (u.exists()) {
            list.add(u);
        }
        return list;
    }
}
