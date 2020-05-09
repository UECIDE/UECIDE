package org.uecide;

import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public abstract class Library implements Comparable {
    public File root;
    int priority;
    String category;

    public ArrayList<String> requiredLibraries = new ArrayList<String>();
    public String name;
    public File examplesFolder = null;
    public TreeSet<File> sourceFiles = new TreeSet<File>();
    public TreeSet<File> headerFiles = new TreeSet<File>();
    public TreeSet<File> archiveFiles = new TreeSet<File>();
    public File mainInclude;
    public File utilityFolder;
    public TreeMap<String, File>examples;
    public String type;
    public String core;
    public int compiledPercent = 0;
    public File sourceFolder;
    public File archFolder;
    public boolean needsRescan = true;

    public int compat = 0;

    public HashMap<String, TreeSet<File>>sourceFilesByArch = null;
    public HashMap<String, TreeSet<File>>headerFilesByArch = null;

    public boolean valid = false;

    public boolean buildLibrary = false;

    PropertyFile properties = null;
    File propertyFile = null;

    boolean utilRecurse = false;

    public Library(File loc, int pri) {
        root = loc;
        priority = pri;
        probedFiles = new ArrayList<String>();
    }

    public void addSourceFile(File f) {
        int type = FileType.getType(f);
        switch (type) {
            case FileType.CSOURCE:
            case FileType.CPPSOURCE:
            case FileType.ASMSOURCE:
                sourceFiles.add(f);
                break;
            case FileType.HEADER:
                headerFiles.add(f);
                break;
            case FileType.LIBRARY:
                archiveFiles.add(f);
                break;
        }
    }

    ArrayList<String> probedFiles;

    public boolean hasHeader(String header) {
        for(File f : headerFiles) {
            if(f.getName().equals(header)) {
                return true;
            }
        }

        return false;
    }

    public File getHeader(String header) {
        for(File f : headerFiles) {
            if(f.getName().equals(header)) {
                return f;
            }
        }

        return null;
    }

    public void gatherIncludes(File f) {
        try {
            String data = Utils.getFileAsString(f);
            if (data == null) return;
            String[] lines = data.split("\n");

            Pattern inc = Pattern.compile("^#\\s*include\\s+[<\"](.*)[>\"]");

            for(String line : lines) {
                line = line.trim();
                Matcher m = inc.matcher(line);
                if (m.find()) {
                    String i = m.group(1);

                    // If the file is not local to the library then go ahead and add it.
                    // Local files override other libraries.
                    File localFile = new File(root, i);

                    if(!hasHeader(localFile.getName())) {
                        if(requiredLibraries.indexOf(i) == -1) {
                            requiredLibraries.add(i);
                        }
                    } else {
                        // This is a local header.  We should check it for libraries.
                        localFile = getHeader(localFile.getName());

                        if(probedFiles.indexOf(localFile.getAbsolutePath()) == -1) {
                            probedFiles.add(localFile.getAbsolutePath());
                            gatherIncludes(localFile);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Debug.exception(ex);
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getName() {
        return name;
    }

    public File getFolder() {
        return root;
    }

    public File getUtilityFolder() {
        return utilityFolder;
    }

    public String getInclude() {
        if (mainInclude == null) { return ""; }
        return "#include <" + mainInclude.getName() + ">\n";
    }

    public TreeSet<File> getSourceFiles(Sketch s) {
        TreeSet<File> sf = new TreeSet<File>();

        if (sourceFilesByArch!=null && s != null) {
            String arch = s.getArch();
            if (arch != null) {
                TreeSet<File>af = sourceFilesByArch.get(arch);
                if (af != null) {
                    sf.addAll(af);
                }
            }
        }

        if (sourceFiles != null) {
            sf.addAll(sourceFiles);
        }
        return sf;
    }

    public TreeSet<File> getHeaderFiles(Sketch s) {
        TreeSet<File> sf = new TreeSet<File>();

        if (headerFilesByArch!=null && s != null) {
            String arch = s.getArch();
            if (arch != null) {
                TreeSet<File>af = headerFilesByArch.get(arch);
                if (af != null) {
                    sf.addAll(af);
                }
            }
        }

        if (headerFiles != null) {
            sf.addAll(headerFiles);
        }
        return sf;
    }

    public ArrayList<String> getRequiredLibraries() {
        requiredLibraries = new ArrayList<String>();
        for(File f : headerFiles) {
            gatherIncludes(f);
        }

        for(File f : sourceFiles) {
            gatherIncludes(f);
        }
        return requiredLibraries;
    }

    public String getType() {
        return type;
    }

    public boolean isCore() {
        return type.startsWith("core:");
    }

    public boolean isContributed() {
        return type.startsWith("cat");
    }

    public boolean isSketch() {
        return type.equals("sketch");
    }

    public String getCore() {
        return core;
    }

    public boolean worksWith(String c) {
        if(core.equals("all")) {
            return true;
        }

        return core.equals(c);
    }

    public String getLinkName() {
        return getName().replaceAll("\\s+", "_");
    }

    public File getExamplesFolder() {
        return examplesFolder;
    }

    public String toString() {
        return getName();
    }

    public void setCompiledPercent(int p) {
        compiledPercent = p;
    }

    public int getCompiledPercent() {
        return compiledPercent;
    }

    public boolean isHeaderOnly() {
        return sourceFiles.size() == 0;
    }

    public boolean isLocal(File sketchFolder) {
        File wouldBeLocalLibs = new File(sketchFolder, "libraries");
        File wouldBeLocal = new File(wouldBeLocalLibs, root.getName());

        if(wouldBeLocal.equals(root)) {
            return true;
        }

        return false;
    }

    public int compareTo(Object o) {
        if(o instanceof Library) {
            Library ol = (Library)o;
            return this.toString().compareTo(ol.toString());
        }

        if(o instanceof String) {
            String os = (String)o;
            return this.toString().compareTo(os);
        }

        return 0;
    }

    public File getKeywords() {
        return new File(getFolder(), "keywords.txt");
    }

    public PropertyFile getProperties() {
        if (properties != null) {
            return properties;
        }
        return new PropertyFile();
    }

    public File getSourceFolder() {
        return sourceFolder;
    }

    public String getMainHeader() { 
        if (mainInclude == null) {
            return null;
        }
        return mainInclude.getName();
    }

    public Version getVersion() {
        return new Version("unknown");
    }

    public int getPriority() {
        return priority;
    }

    public boolean worksWith(Core c) {
        if (core.equals("all")) return true;
        if (c.getName().equals(core)) return true;
        return false;
    }

    public void setCore(String c) {
        core = c;
    }

    public void setCore(Core c) {
        core = c.getName();
    }

    public void setCategory(String name) {
        category = name;
    }

    public String getCategory() {
        return category;
    }

    public TreeSet<File>getHeaderFiles() {
        return headerFiles;
    }

    public static final int COMP_NONE       = 0;
    public static final int COMP_RUN        = 1;
    public static final int COMP_FAIL       = 2;
    public static final int COMP_DONE       = 3;

    public static final int RECOMPILE_NONE  = 0;
    public static final int RECOMPILE_LIGHT = 1;
    public static final int RECOMPILE_ALL   = 2;

    int compilingState = COMP_NONE;

    // Compile the library
    public boolean compile(Context ctx) {
        int recompile = needsCompile(ctx);
        ctx.triggerEvent("libraryCompileStarted", this);

        if (recompile == RECOMPILE_NONE) {
            compilingState = COMP_DONE;
            ctx.triggerEvent("libraryCompileFinished", this);
            return true;
        }

        compilingState = COMP_RUN;

        File archiveFile = getArchiveFile(ctx);

        long archiveTime = 0;

        if (recompile == RECOMPILE_ALL) {
            if (archiveFile.exists()) {
                archiveFile.delete();
            }
        } else {
            archiveTime = archiveFile.lastModified();
        }

        TreeSet<File> filesToCompile = new TreeSet<File>();

        TreeSet<File>sf = getSourceFiles(ctx.getSketch());
        for (File f : sf) {
            if (f.lastModified() > archiveTime) {
                filesToCompile.add(f);
            }
        }

        Context localCtx = new Context(ctx); // Make a copy of the context so we can mess with include paths and such

        ArrayList<File> includeDirs = new ArrayList<File>();

        TreeMap<String, ArrayList<File>> coreLibs = ctx.getCoreLibs();

        for(String lib : coreLibs.keySet()) {
            ArrayList<File> libfiles = coreLibs.get(lib);
            includeDirs.addAll(libfiles);
        }




        includeDirs.addAll(getIncludeFolders());


        ArrayList<Library> neededLibraries = new ArrayList<Library>();

        ArrayDeque<Library> queue = new ArrayDeque<Library>();

        ArrayList<String> libs = getRequiredLibraries();

        if (libs != null) {
            for (String lib : libs) {
                Library l = LibraryManager.getLibraryByName(lib, ctx.getCore());
                if (l != null) {
                    queue.add(l);
                }
            }
        }

        while (queue.size() > 0) {
            Library l = queue.remove();
            if (!neededLibraries.contains(l)) {
                System.err.println("Adding library " + l);
                neededLibraries.add(l);
                ArrayList<String> recursedLibraries = l.getRequiredLibraries();
                if (recursedLibraries != null) {
                    for (String s : recursedLibraries) {
                        Library rl = LibraryManager.getLibraryByName(s, ctx.getCore());
                        if (rl != null) {
                            if (!neededLibraries.contains(rl)) {
                                queue.add(rl);
                            }
                        }
                    }
                }
            }
        }

        for (Library l : neededLibraries) {
            includeDirs.addAll(l.getIncludeFolders());
        }

        includeDirs.add(ctx.getCore().getFolder());
        includeDirs.add(ctx.getBoard().getFolder());

        String inc = "";
        for (File f : includeDirs) {
            if (!inc.equals("")) {
                inc = inc + "::";
            }
            inc = inc + "-I" + f.getAbsolutePath();
        }

        localCtx.set("includes", inc);

        File utility = getUtilityFolder();
        PropertyFile props = localCtx.getMerged();
        if (!UECIDE.isQuiet()) ctx.bullet2(toString() + " [" + getFolder().getAbsolutePath() + "]");

        localCtx.set("library", archiveFile.getAbsolutePath());

        File libBuildFolder = new File(ctx.getBuildDir(), "lib" + getLinkName());
        libBuildFolder.mkdirs();
        int fileCount = filesToCompile.size();
        int count = 0;

        ArrayList<FileCompiler> compileJobs = new ArrayList<FileCompiler>();

        for (File f : filesToCompile) {
            FileCompiler fc = new FileCompiler(localCtx, f, libBuildFolder, sourceFolder);
            compileJobs.add(fc);
            ctx.queueJob(fc);
        }

        ctx.waitQueue();

        for (FileCompiler fc : compileJobs) {
            if (fc.getState() == FileCompiler.FAILED) {
                localCtx.dispose();
                compilingState = COMP_FAIL;
                ctx.triggerEvent("libraryCompileFailed", this);
                return false;
            }
            File out = fc.getResult();
            ctx.triggerEvent("buildFileAdded", out);

            localCtx.set("object.name", out.getAbsolutePath());
            boolean ok = (Boolean)localCtx.executeKey("compile.ar");

            count++;
            setCompiledPercent(count * 100 / fileCount);
            out.delete();
            ctx.triggerEvent("buildFileRemoved", out);
            ctx.triggerEvent("libraryFileCompiled", this);
        }

        localCtx.dispose();
        setCompiledPercent(100);
        UECIDE.tryDelete(libBuildFolder); 
        compilingState = COMP_DONE;
        ctx.triggerEvent("libraryCompileFinished", this);
        return true;
    }

    public boolean isCompiled() { return compilingState == COMP_DONE; }
    public boolean isCompiling() { return compilingState == COMP_RUN; }
    public boolean compilingFailed() { return compilingState == COMP_FAIL; }

    // Compare all the files of this library with the archive for the
    // current board. If any file is newer than the archive then it
    // needs recompiling.
    public int needsCompile(Context ctx) {
        File archiveFile = getArchiveFile(ctx);

        // If there is no archive we recompile everything
        if (!archiveFile.exists()) {
            return RECOMPILE_ALL;
        }

        long archiveTime = archiveFile.lastModified();

        // If a header is newer we recompile everything
        for (File f : getHeaderFiles(ctx.getSketch())) {
            if (f.lastModified() > archiveTime) return RECOMPILE_ALL;
        }

        // If it's just a source file that's newer we just compile the newer source files
        for (File f : getSourceFiles(ctx.getSketch())) {
            if (f.lastModified() > archiveTime) return RECOMPILE_LIGHT;
        }
        
        // Nothing seems to be newer, so don't touch it.
        return RECOMPILE_NONE;
    }

    public File getArchiveFile(Context ctx) {
        File cacheDir = ctx.getCacheDir();
        PropertyFile pf = ctx.getMerged();
        String libraryPrefix = pf.get("compiler.library.prefix", "lib");
        String librarySuffix = pf.get("compiler.library", "a");
        File archiveFile = new File(cacheDir, libraryPrefix + getLinkName() + "." + librarySuffix);
        return archiveFile;
    }

    public void purge(Context ctx) {
        File a = getArchiveFile(ctx);
        if (a.exists()) {
            a.delete();
        }
    }

    public abstract ArrayList<File> getIncludeFolders();
}


