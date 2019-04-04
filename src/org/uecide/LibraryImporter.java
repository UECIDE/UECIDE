package org.uecide;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.*;

public class LibraryImporter extends JFrame {

    JLabel zipPath = null;
    File selectedZipFile = null;

    JScrollPane libraryList = null;

    ArrayList<LibraryImportSettings> foundLibraries = new ArrayList<LibraryImportSettings>();
    ArrayList<LibraryImportSettings> orphanLibraries = new ArrayList<LibraryImportSettings>();

    class LibraryImportSettings {
        String name;
        Path path;
        JComboBox<OptionSet> importOptions;
        JComboBox<String> headerFile;

        public LibraryImportSettings(String n, Path p) {
            name = n;
            path = p;
            importOptions = null;
            headerFile = null;
        }

        public LibraryImportSettings(Path p) {
            name = null;
            path = p;
            importOptions = null;
            headerFile = null;
        }

        public void setOptions(JComboBox<OptionSet> op) {
            importOptions = op;
        }
    
        public JComboBox<OptionSet> getOptions() {
            return importOptions;
        }

        public void setHeader(JComboBox<String> h) {
            headerFile = h;
        }

        public String getName() { 
            if (name == null) {
                if (headerFile != null) {
                    String hf = (String)headerFile.getSelectedItem();
                    if (hf.endsWith(".h")) {
                        return hf.substring(0, hf.length() - 2);
                    }
                    return hf;
                }
            }
            return name;
        }

        public Path getPath() {
            return path;
        }
       
    }

    public LibraryImporter() {

        setLayout(new BorderLayout());

        JPanel fileSelectPanel = new JPanel();
        fileSelectPanel.setLayout(new BorderLayout());

        zipPath = new JLabel("Press 'Select...' to choose a ZIP file...");
        fileSelectPanel.add(zipPath, BorderLayout.CENTER);
        
        JButton zipSelectButton = new JButton("Select...");
        zipSelectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectZipFile();
                if (selectedZipFile != null) {
                    processZipFile();
                }
            }
        });
        fileSelectPanel.add(zipSelectButton, BorderLayout.EAST);

        add(fileSelectPanel, BorderLayout.NORTH);

        libraryList = new JScrollPane();
        add(libraryList, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LibraryImporter.this.setVisible(false);
                LibraryImporter.this.dispose();
            }
        });
        buttons.add(cancel);

        JButton importLibraries = new JButton("Import Libraries");
        importLibraries.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                installLibraries();
                Base.rescanLibraries();
                LibraryImporter.this.setVisible(false);
                LibraryImporter.this.dispose();
            }
        });
        buttons.add(importLibraries);

        add(buttons, BorderLayout.SOUTH);

        pack();

        setSize(400, 300);
        setVisible(true);
    }


    void selectZipFile() {
        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if(f.getName().toLowerCase().endsWith(".zip")) {
                    return true;
                }

                if(f.isDirectory()) {
                    return true;
                }

                return false;
            }

            public String getDescription() {
                return Base.i18n.string("filters.zip");
            }
        });

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.importlib");
            if (loc == null) {
                loc = Base.getSketchbookFolder();
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(Base.getSketchbookFolder());
        }

        int rv = fc.showOpenDialog(this);

        if (rv == JFileChooser.APPROVE_OPTION) {
            selectedZipFile = fc.getSelectedFile();
            if (selectedZipFile != null) {
                zipPath.setText(selectedZipFile.getAbsolutePath());
            }
            if (Preferences.getBoolean("editor.save.remloc")) {
                Preferences.setFile("editor.locations.importlib", fc.getSelectedFile().getParentFile());
            }
        }

    }

    TreeSet<Path> getZipEntries(File zipFile) {
        TreeSet<Path> files = new TreeSet<Path>();

        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();

            while(ze != null) {
                if(!ze.isDirectory()) {
                    files.add(Paths.get(ze.getName()));
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
        } catch(Exception e) {
            Base.error(e);
            return null;
        }

        return files;
    }

    String getZipFileContent(File zipFile, Path filePath) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();

            while(ze != null) {
                Path thisPath = Paths.get(ze.getName());
                if (thisPath.equals(filePath)) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    reader.close();
                    zis.close();
                    return sb.toString();
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
        } catch(Exception e) {
            Base.error(e);
        }

        return null;
    }



    void processZipFile() {
        foundLibraries = new ArrayList<LibraryImportSettings>();
        orphanLibraries = new ArrayList<LibraryImportSettings>();
        ArrayList<Path> usedPaths = new ArrayList<Path>();

        HashMap<Path, ArrayList<String>> otherHeaders = new HashMap<Path, ArrayList<String>>();

        TreeSet<Path> files = getZipEntries(selectedZipFile);
        for (Path f : files) {

            if (pathIsUsed(f, usedPaths)) {
                continue;
            }
            if (f.getParent() == null) continue; // Top level file

            if (f.getFileName().toString().equals(f.getParent().getFileName().toString() + ".h")) {
                // Positively identified old style library
                foundLibraries.add(new LibraryImportSettings(f.getParent().getFileName().toString(), f.getParent()));
                usedPaths.add(f.getParent());
                continue;
            }

            if (f.getFileName().toString().equals("library.properties")) {
                // Positively identified new style library
                String data = getZipFileContent(selectedZipFile, f);
                if (data == null) continue; // Broken zip file?

                String[] lines = data.split("\n");
                Pattern p = Pattern.compile("^\\s*(.*)\\s*=\\s*(.*)\\s*$");
            
                for (String line : lines) {
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        if (m.group(1).equals("name")) {
                            foundLibraries.add(new LibraryImportSettings(m.group(2), f.getParent()));
                            usedPaths.add(f.getParent());
                        }
                    }
                    
                }
                
                continue;
            }

            if (f.toString().endsWith(".h")) {
                // Other .h file - treat specially
                ArrayList<String> existingHeaders = otherHeaders.get(f.getParent());
                if (existingHeaders == null) {
                    existingHeaders = new ArrayList<String>();
                }
                existingHeaders.add(f.getFileName().toString());
                otherHeaders.put(f.getParent(), existingHeaders);
            }
        }

        JPanel libraryListPane = new JPanel();
        libraryListPane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.33;
        libraryListPane.add(new JBoldLabel("Automatically Detected Libraries", 1.5f), c);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        libraryListPane.add(new JBoldLabel("Library Name"), c);
        c.gridx = 1;
        libraryListPane.add(new JBoldLabel("Path"), c);
        c.gridx = 2;
        libraryListPane.add(new JBoldLabel("Actions"), c);

        c.gridy++;
        c.gridx = 0;

        for (LibraryImportSettings lib : foundLibraries) {
            c.gridx = 0;
            libraryListPane.add(new JLabel(lib.getName()), c);
            c.gridx = 1;
            libraryListPane.add(new JLabel(lib.getPath().toString()), c);
            c.gridx = 2;
            libraryListPane.add(createOptionList(lib, false), c);
            c.gridy++;

        }

        c.gridx = 0;
        c.gridwidth = 3;
        libraryListPane.add(new JBoldLabel(" ", 1.5f), c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        libraryListPane.add(new JBoldLabel("Orphaned Header Files", 1.5f), c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        libraryListPane.add(new JBoldLabel("Header File"), c);
        c.gridx = 1;
        libraryListPane.add(new JBoldLabel("Path"), c);
        c.gridx = 2;
        libraryListPane.add(new JBoldLabel("Actions"), c);
        c.gridy++;
        c.gridx = 0;

        for (Path p : otherHeaders.keySet()) {
            ArrayList<String> h = otherHeaders.get(p);

            JComboBox<String> headerList = new JComboBox<String>(h.toArray(new String[0]));
            LibraryImportSettings s = new LibraryImportSettings(p);
            JComboBox actionList = createOptionList(s, true);
            s.setHeader(headerList);

            orphanLibraries.add(s);

            libraryListPane.add(headerList, c);
            c.gridx = 1;
            libraryListPane.add(new JLabel(p.toString()), c);
            c.gridx = 2;
            libraryListPane.add(actionList, c);
            c.gridx = 0;
            c.gridy++;
        }

        libraryList.setViewportView(libraryListPane);

    }

    boolean pathIsUsed(Path p, ArrayList<Path> usedPaths) {
        for (Path up : usedPaths) {
            if (p.startsWith(up)) return true;
        }
        return false;
    }

    class OptionSet {
        int type;
        String destination;
        
        public OptionSet(int t, String d) {
            type = t;
            destination = d;
        }

        public String toString() {
            if (type == 0) return "Ignore";
            if (type == 1) return "Install to " + Library.getCategoryName(destination);
            return "Error";
        }

        public boolean isInstall() {
            return (type == 1);
        }

        public File installLocation() {
            return Library.getCategoryLocation(destination);
        }
    }

    JComboBox createOptionList(LibraryImportSettings lib, boolean ig) {
        ArrayList<OptionSet> options = new ArrayList<OptionSet>();

        if (ig) {
            options.add(new OptionSet(0, ""));
        }

        TreeSet<String> cats = Library.getLibraryCategories();
        for (String cat : cats) {
            if (cat.startsWith("cat:")) {
                options.add(new OptionSet(1, cat));
            }
        }

        if (!ig) {
            options.add(new OptionSet(0, ""));
        }

        JComboBox<OptionSet> cb = new JComboBox<OptionSet>(options.toArray(new OptionSet[0]));
        lib.setOptions(cb);
        return cb;
    }

    void installLibraries() {
        for (LibraryImportSettings l : foundLibraries) {
            JComboBox<OptionSet> options = l.getOptions();
            OptionSet selectedOption = (OptionSet)options.getSelectedItem();            
            if (selectedOption.isInstall()) {
                installLibrary(l);
            }
        }
        for (LibraryImportSettings l : orphanLibraries) {
            JComboBox<OptionSet> options = l.getOptions();
            OptionSet selectedOption = (OptionSet)options.getSelectedItem();            
            if (selectedOption.isInstall()) {
                installLibrary(l);
            }
        }
    }

    void installLibrary(LibraryImportSettings l) {
        JComboBox<OptionSet> options = l.getOptions();
        OptionSet selectedOption = (OptionSet)options.getSelectedItem();

        File cat = selectedOption.installLocation();
        File root = new File(cat, l.getName());
        Path rootPath = root.toPath();

        root.mkdirs();

        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(selectedZipFile));
            ZipEntry ze = zis.getNextEntry();

            while(ze != null) {
                Path thisPath = Paths.get(ze.getName());
                if (thisPath.equals(l.getPath())) {
                    ze = zis.getNextEntry();
                    continue;
                }
                if (thisPath.startsWith(l.getPath())) {
                    String rel = l.getPath().toUri().relativize(thisPath.toUri()).getPath();
                    File dest = new File(root, rel);
    
                    if (ze.isDirectory()) {
                        dest.mkdirs();
                    } else {
                        FileOutputStream fos = new FileOutputStream(dest);
                        int len;
                        byte[] buffer = new byte[1024];

                        while((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                        fos.close();
                    }
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
        } catch(Exception e) {
            Base.error(e);
        }


        
    }

}
