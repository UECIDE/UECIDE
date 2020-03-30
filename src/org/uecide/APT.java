/*
 * Copyright (c) 2015, Majenko Technologies
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

import java.io.*;
import java.util.*;

import org.apache.commons.io.input.*;

public class APT {
    File root;

    File aptFolder;
    File dbFolder;
    File cacheFolder;
    File packagesFolder;
    File packagesDB;

    Context ctx;

    HashMap<String, Package> cachedPackages;
    HashMap<String, Package> installedPackages;

    ArrayList<Source>sources = new ArrayList<Source>();

    static APT apt = null;

    public APT(Context context, String rootPath) throws FileNotFoundException, IOException {
        ctx = context;
        root = new File(rootPath);
        initRepository();
    }

    public APT(Context context, File rootFile) throws FileNotFoundException, IOException {
        ctx = context;
        root = rootFile;
        initRepository();
    }

    public void makeTree() {
        if (!aptFolder.exists()) {  
            if (!aptFolder.mkdirs()) { ctx.error("Unable to make apt folder"); return; }
        }
        if (!dbFolder.exists()) {
            if (!dbFolder.mkdir()) { ctx.error("Unable to make db folder"); return; }
        }
        if (!cacheFolder.exists()) {
            if (!cacheFolder.mkdir()) { ctx.error("Unable to make cache folder"); return; }
        }
        if (!packagesFolder.exists()) {
            if (!packagesFolder.mkdir()) { ctx.error("Unable to make packages folder"); return; }
        }
    }

    public String getOS() {
        String osname = System.getProperty("os.name");
        String osarch = System.getProperty("os.arch");

        if (osname.indexOf("Mac") != -1) {
            return "darwin-amd64";
        }
        if (osname.indexOf("Windows") != -1) {
            if (osarch.equals("i386")) {
                return "windows-i386";
            } else {
                return "windows-amd64";
            }
        }
        String arch = osarch.toLowerCase();
        if (arch.equals("arm")) {
            arch = "armhf";
        }
        return osname.toLowerCase() + "-" + arch;
    }

    public void loadSources(File sfile) throws FileNotFoundException, IOException{
        
        if (!sfile.exists()) {
            return;
        }

        FileReader fr = new FileReader(sfile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }
            String[] bits = line.split("\\s+");
            if (bits.length < 4) {
                continue;
            }

            if (bits[0].equals("deb")) {
                String url = bits[1];
                String codename = bits[2];
                String[] sects = Arrays.copyOfRange(bits, 3, bits.length);
                Source s = new Source(url, codename, getOS(), sects);
                addSource(s);
            }
        }
        br.close();
        fr.close();
    }

    public void initRepository() throws FileNotFoundException, IOException {
        aptFolder = new File(root, "apt");
        dbFolder = new File(aptFolder, "db");
        cacheFolder = new File(aptFolder, "cache");
        packagesFolder = new File(dbFolder, "packages");

        makeTree();
        sources = new ArrayList<Source>();

        loadSources(new File(dbFolder, "sources.db"));

        File dbdir = new File(dbFolder, "sources.d");
        if (dbdir.exists()) {
            File[] srcs = dbdir.listFiles();
            for (File src : srcs) {
                if (!src.getName().startsWith(".")) {
                    loadSources(src);
                }
            }
        }

        packagesDB = new File(dbFolder, "packages.db");

        cachedPackages = loadPackages(packagesDB);
        installedPackages = new HashMap<String, Package>();
        File[] pks = packagesFolder.listFiles();
        for (File pk : pks) {
            if (pk.isDirectory()) {
                if (!pk.getName().startsWith(".")) {
                    File pf = new File(pk, "control");
                    if (pf.exists()) {
                        HashMap<String, Package> ap = loadPackages(pf);
                        installedPackages.putAll(ap);
                    }
                }
            }
        }
    }

    public HashMap<String, Package> loadPackages(File f) {
        HashMap<String, Package> out = new HashMap<String, Package>();
        if (!f.exists()) {
            return out;
        }
            
        try {
            StringBuilder chunk = new StringBuilder();


            FileReader fis = new FileReader(f);
            BufferedReader in = new BufferedReader(fis);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("")) {
                    if (chunk.toString().length() > 0) {
                        Package p = new Package(ctx, chunk.toString());
                        if (p.isValid) {
                            out.put(p.getName(), p);
                        }
                    }
                    chunk = new StringBuilder();
                } else {
                    chunk.append(line);
                    chunk.append("\n");
                }
            }
            in.close();
            fis.close();
            if (chunk.toString().length() > 0) {
                Package p = new Package(ctx, chunk.toString());
                if (p.isValid) {
                    out.put(p.getName(), p);
                }
            }
            return out; 
        } catch (Exception e) {
            Debug.exception(e);
            return null;
        }
    }

    public void save() {
        makeTree();

        try {
            PrintWriter pw = new PrintWriter(packagesDB);
            for (Package p : cachedPackages.values()) {
                pw.print(p.getInfo());
                pw.print("\n");
            }
            pw.close();
        } catch (Exception e) {
            Debug.exception(e);
        }
    }

    public void addSource(Source s) {
        sources.add(s);
    }

    public void addSource(String root, String codename, String arch, String[] sections) {
        Source s = new Source(root, codename, arch, sections);
        addSource(s);
    }

    public void removeSource(String root, String codename) {
        for (Source s : sources) {
            if (s.getRoot().equals(root)) {
                if (s.getCodename().equals(codename)) {
                    sources.remove(s);
                    return;
                }
            }
        }
    }

    public void update() {
        update(false, false);
    }

    public void update(boolean resOnly, boolean silent) {
        cachedPackages = new HashMap<String, Package>();
        int done = 0;
        for (Source s : sources) {
            if (resOnly) {
                if (!s.getRoot().startsWith("res://")) {
                    continue;
                }
            }

            Package[] packages = s.getPackages(silent);
            done++;

            for (Package p : packages) {
                if (cachedPackages.get(p.getName()) != null) {
                    Version existing = cachedPackages.get(p.getName()).getVersion();
                    Version testing = p.getVersion();
                    if (testing.compareTo(existing) > 0) {
                        cachedPackages.put(p.getName(), p);
                    } else {
                        Package e = cachedPackages.get(p.getName());
                        e.addRepository(s.getRoot());
                        cachedPackages.put(e.getName(), e); // Is this needed? I'm never sure with Java
                    }
                } else {
                    cachedPackages.put(p.getName(), p);
                }
            }
        }
        if (resOnly) {
            if (!packagesDB.exists()) {
                save();
            }
        } else {
            save();
        }

        Package[] ul = getUpgradeList();
        ctx.message(ul.length + " packages can be upgraded.");
    }

    public void listPackages(String section) {
        String format = "%-50s %10s %10s %s";
        ctx.message(String.format(format, "Package", "Installed", "Available", ""));
        Package[] plist = getPackages(section);
        for (Package p : plist) {
            if ((section != null) && (!(p.getSection().equals(section)))) {
                continue;
            }
            String name = p.getName();
            Version avail = p.getVersion();
            Version inst = null;
            String msg = "";
            if (installedPackages.get(name) != null) {
                inst = installedPackages.get(name).getVersion();
                if (avail.compareTo(inst) > 0) {
                    msg = "UPDATE!";
                }
            }
            ctx.message(String.format(format, name, inst == null ? "" : inst.toString(), avail.toString(), msg));
        }
        for (Package p : installedPackages.values()) {
            if ((section != null) && (!(p.getSection().equals(section)))) {
                continue;
            }
            String name = p.getName();
            Version avail = p.getVersion();
            Version inst = null;
            String msg = "";
            if (cachedPackages.get(name) == null) {
                ctx.message(String.format(format, name, avail.toString(), "", msg));
            }
        }
    }

    public void listPackages() {
        listPackages(null);
    }

    public Package[] getPackages(String section) {
        ArrayList<Package> out = new ArrayList<Package>();
        for (Package p : cachedPackages.values()) {
            if ((section == null) || (p.getSection().equals(section))) {
                out.add(p);
            }
        }
        Package[] plist = out.toArray(new Package[0]);
        Arrays.sort(plist);
        return plist;
    }

    public Package[] getRequiredPackages() {
        ArrayList<Package> out = new ArrayList<Package>();
        for (Package p : cachedPackages.values()) {
            if (p.getPriority().equals("required")) {
                out.add(p);
            }
        }
        Package[] plist = out.toArray(new Package[0]);
        Arrays.sort(plist);
        return plist;
    }

    public Package[] getPackages() {
        return getPackages(null);
    }

    public Package getPackage(String name) {
        return cachedPackages.get(name);
    }

    public Package getInstalledPackage(String name) {
        return installedPackages.get(name);
    }

    public Package[] resolveDepends(Package top) {
        ArrayDeque<String> depList = new ArrayDeque<String>();
        HashMap<String, Package> pkgList = new HashMap<String, Package>();

        String[] deps = top.getDependencies(true);
        if (deps != null) {
            for (String dep : deps) {
                depList.add(dep);
            }

            String adep;
            while ((adep = depList.poll()) != null) {
                Package foundPkg = cachedPackages.get(adep);
                if (foundPkg == null) {
                    ctx.error("Broken dependency: " + adep);
                } else {
                    if (pkgList.get(adep) == null) {
                        pkgList.put(adep, foundPkg);
                        String[] subDeps = foundPkg.getDependencies(true);
                        if (subDeps != null) {
                            for (String dep : subDeps) {
                                depList.add(dep);
                            }
                        }
                    }
                }
            }
        }
        return pkgList.values().toArray(new Package[0]);
    }

    public boolean isUpgradable(Package p) {
        String name = p.getName();
        Package inst = installedPackages.get(name);
        if (inst == null) {
            return false;
        }
        Version iv = inst.getVersion();
        Version pv = p.getVersion();
        if (pv.compareTo(iv) > 0) {
            return true;
        }
        return false;
    }

    public boolean isInstalled(Package p) {
        if (p == null) return false;
        String name = p.getName();
        Package inst = installedPackages.get(name);
        if (inst == null) {
            return false;
        }
        return true;
    }

    public void upgradePackage(Package p) throws FileNotFoundException, IOException{
        if (!isUpgradable(p)) {
            return;
        }
        Package[] deps = resolveDepends(p);
        for (Package dep : deps) {
            if (!isInstalled(dep) || isUpgradable(dep)) {
                if (!dep.fetchPackage(cacheFolder)) {
                    ctx.error("Error downloading " + dep);
                    return;
                }
            }
        }
        if (!p.fetchPackage(cacheFolder)) {
            ctx.error("Error downloading " + p);
        }

        for (Package dep : deps) {
            if (!isInstalled(dep)) {
                dep.extractPackage(cacheFolder, packagesFolder, root);
            } else if(isUpgradable(dep)) {
                uninstallPackage(dep, true);
                dep.extractPackage(cacheFolder, packagesFolder, root);
            }
        }
        uninstallPackage(p, true);
        p.extractPackage(cacheFolder, packagesFolder, root);
        initRepository();
    }
    public void installPackage(Package p) throws FileNotFoundException, IOException{
        Package[] deps = resolveDepends(p);
        for (Package dep : deps) {
            if (!isInstalled(dep)) {
                if (!dep.fetchPackage(cacheFolder)) {
                    ctx.error("Error downloading " + dep);
                    return;
                }
            }
        }
        if (!p.fetchPackage(cacheFolder)) {
            ctx.error("Error downloading " + p);
        }

        for (Package dep : deps) {
            if (!isInstalled(dep)) {
                dep.extractPackage(cacheFolder, packagesFolder, root);
            }
        }
        if (isInstalled(p)) {
            uninstallPackage(p, true);
        }
        String reps[] = p.getReplaces();
        if (reps != null) {
            for (String rep : reps) {
                Package rp = getPackage(rep);
                if (isInstalled(rp)) {
                    uninstallPackage(rp, true);
                }
            }
        }
        p.extractPackage(cacheFolder, packagesFolder, root);
        initRepository();
    }

    public Package[] getUpgradeList() {
        ArrayList<Package> toUpdate = new ArrayList<Package>();

        for (Package p : cachedPackages.values()) {
            String name = p.getName();
            Version avail = p.getVersion();
            Version inst = null;
            String msg = "";
            if (installedPackages.get(name) != null) {
                inst = installedPackages.get(name).getVersion();
                if (avail.compareTo(inst) > 0) {
                    toUpdate.add(p);
                }
            }
        }
        return toUpdate.toArray(new Package[0]);
    }

    public Package[] getDependants(Package p) {
        ArrayList<Package> out = new ArrayList<Package>();

        for (Package ip : installedPackages.values()) {
            String[] deps = ip.getDependencies(false);
            if (deps == null) {
                continue;
            }
            for (String dep : deps) {
                if (dep.equals(p.getName())) {
                    if (out.indexOf(ip) == -1) {
                        out.add(ip);
                    }
                }
            }
        }
        return out.toArray(new Package[0]);
    }

    public void recursivelyUninstallPackage(Package p) {
        if (!isInstalled(p)) {
            return;
        }
        Package[] deps = getDependants(p);
        for (Package dep : deps) {
            recursivelyUninstallPackage(dep);
        }
        ctx.message("Uninstalling " + p);
        uninstallPackage(p, false);
    }

    public boolean uninstallPackage(Package p, boolean force) {
        if (!isInstalled(p)) {
            ctx.error(p.getName() + " is not installed.");
            return false;
        }
        try {
            if (!force) {
                Package[] deps = getDependants(p);
                if (deps.length > 0) {
                    ctx.error(p.getName() + " is required by:");
                    for (Package dep : deps) {
                        ctx.bullet(dep.getName());
                    }
                    ctx.error("It cannot be removed.");
                    return false;
                }
            }

            ctx.message("Uninstalling " + p.getName() + " ... ");
            
            File pdir = new File(packagesFolder, p.getName());
            if (pdir.exists()) {

                File md5 = new File(pdir, "md5sums");
                File plist = new File(pdir, "files");

                ArrayList<File> files = new ArrayList<File>();

                if (md5.exists()) {
                    FileReader fr = new FileReader(md5);
                    BufferedReader br = new BufferedReader(fr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] bits = line.split("\\s+");
                        if (bits.length == 2) {
                            File f = new File(UECIDE.getDataFolder(), bits[1]);
                            files.add(f);
                        }
                    }
                    br.close();
                    fr.close();
                } else if (plist.exists()) {
                    FileReader fr = new FileReader(plist);
                    BufferedReader br = new BufferedReader(fr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        File f = new File(line);
                        if (!f.isDirectory()) {
                            files.add(f);
                        }
                    }
                    br.close();
                    fr.close();
                }

                ArrayList<File> dirs = new ArrayList<File>();

                for (File f : files) {
                    File par = f.getParentFile();
                    if (!par.getCanonicalPath().equals(UECIDE.getDataFolder().getCanonicalPath())) {
                        if (dirs.indexOf(par) == -1) {    
                            dirs.add(par);
                        }
                    }
                    UECIDE.tryDelete(f);
                }

                cleanDirectoryList(dirs);

                UECIDE.removeDir(pdir);
                
            }
            initRepository();
            ctx.message("done");
            return true;
        } catch (Exception e) {
            Debug.exception(e);
        }
        return false;
    }

    void cleanDirectoryList(ArrayList<File> dirs) throws IOException {
        ArrayList<File> todo = new ArrayList<File>();
        for (File d : dirs) {
            if (d.isDirectory()) {
                String[] files = d.list();
                if (files.length == 0) {
                    File p = d.getParentFile();
                    if (!p.getCanonicalPath().equals(UECIDE.getDataFolder().getCanonicalPath())) {
                        if (todo.indexOf(p) == -1) {    
                            todo.add(p);
                        }
                        
                        UECIDE.tryDelete(d);
                    }
                }
            }
        }
        if (todo.size() > 0) {
            cleanDirectoryList(todo);
        }
    }

    public int getPackageCount() {
        return cachedPackages.values().size();
    }

    public String[] getUnique(String section, String key) {
        ArrayList<String> out = new ArrayList<String>();
        for (Package p : cachedPackages.values()) {
            if ((section == null) || (p.getSection().equals(section))) {
                if (p.get(key) != null) {
                    String v = p.get(key);
                    if (out.indexOf(v) == -1) {
                        out.add(v);
                    }
                }
            }
        }
        String[] sort = out.toArray(new String[0]);
        Arrays.sort(sort);
        return sort;
    }

    public Package[] getEqual(String key, String value) {
        return getEqual(null, key, value);
    }

    public Package[] getEqual(String section, String key, String value) {
        ArrayList<Package> out = new ArrayList<Package>();
        for (Package p : cachedPackages.values()) {
            if (section == null || p.getSection().equals(section)) {
                if ((value == null) && (p.get(key) == null)) {
                    out.add(p);
                } else if (p.get(key) != null) {
                    if (p.get(key).equals(value)) {
                        out.add(p);
                    }
                }
            }
        }
        Package[] sort = out.toArray(new Package[0]);
        Arrays.sort(sort);
        return sort;
    }

    public boolean hasSource(String root, String codename) {
        for (Source s : sources) {
            
            if (s.getRoot().equals(root)) {
                if (s.getCodename().equals(codename)) {
                    return true;
                }
            }
        }
        return false;
    }

    // A generic function which will fill the arrays with what needs to be done.  Pre-fill the arrays with
    // what you would like doing, and other actions will be added as needed.  It returns a true if any
    // changes were made to the arrays so it can be called recursively.
    public boolean calculateOperations(ArrayList<Package> install, ArrayList<Package> upgrade, ArrayList<Package> remove) {

        boolean madeChanges = false;

        // First off, do the packages that want to be installed require any other packages to be installed?
        for (Package p : install) {
            String[] deps = p.getDependencies(false);
            for (String dep : deps) {
                Package inst = installedPackages.get(dep);
                // If it's not installed then add it to the install list.
                if (inst == null) {
                    inst = cachedPackages.get(dep);
                    if (inst == null) {
                        ctx.error("Error: Unresolved dependency: " + dep);
                    } else {
                        install.add(inst);
                        madeChanges = true;
                    }
                } else {
                    // Is the dependency up to date? If not then add it to the upgrade list
                    Version installedVersion = inst.getVersion();
                    Version availableVersion = cachedPackages.get(dep).getVersion();
                    if (availableVersion.compareTo(installedVersion) > 0) {
                        upgrade.add(inst);
                        madeChanges = true;
                    }
                }
            }
        }

        // Now we'll do the same for packages that want to be upgraded. Check they have all their dependencies
        // and that they are all up to date.
        for (Package p : upgrade) {
            String[] deps = p.getDependencies(false);
            for (String dep : deps) {
                Package inst = installedPackages.get(dep);
                // If it's not installed then add it to the install list.
                if (inst == null) {
                    inst = cachedPackages.get(dep);
                    if (inst == null) {
                        ctx.error("Error: Unresolved dependency: " + dep);
                    } else {
                        install.add(inst);
                        madeChanges = true;
                    }
                } else {
                    // Is the dependency up to date? If not then add it to the upgrade list
                    Version installedVersion = inst.getVersion();
                    Version availableVersion = cachedPackages.get(dep).getVersion();
                    if (availableVersion.compareTo(installedVersion) > 0) {
                        upgrade.add(inst);
                        madeChanges = true;
                    }
                }
            }
        }
        return madeChanges;
        
    }

    public Collection<Package> getInstalledPackages() {
        return installedPackages.values();
    }

    public static APT factory(Context context) throws IOException {
        if (apt == null) {
            File dd = UECIDE.getDataFolder();

            File aptFolder = new File(dd, "apt");
            if (!aptFolder.exists()) {
                aptFolder.mkdirs();
            }

            File dbFolder = new File(aptFolder, "db");
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }

            apt = new APT(context, dd);
        }
        return apt;
    }

}
