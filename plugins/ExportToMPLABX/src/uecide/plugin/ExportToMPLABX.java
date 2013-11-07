
package uecide.plugin;

import uecide.app.*;
import uecide.app.debug.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class ExportToMPLABX extends BasePlugin {

    JFrame win;
    JTextArea projectName;
    JTextArea projectDirectory;
    JButton dirButton;
    JButton exportButton;
    JButton cancelButton;
    Editor editor;

    int folderNumber;

    LinkedHashMap<String, String> tokens;

    public void init(Editor editor) {
        this.editor = editor;
        tokens = new LinkedHashMap<String, String>();

        File themeFolder = Base.getContentFile("lib/theme");

        win = new JFrame("Export to MPLAB-X");
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(true);

        JPanel gutter = new JPanel(new BorderLayout());
        gutter.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel border = new JPanel(new BorderLayout());
        border.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        gutter.add(border);
        
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        grid.setBorder(new EmptyBorder(5, 5, 5, 5));

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        JLabel label = new JLabel("Project Name: ");
        grid.add(label, c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        projectName = new JTextArea(editor.getSketch().getName() + ".X");
        projectName.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        Dimension s = projectName.getPreferredSize();
        s.width = 200;
        projectName.setPreferredSize(s);
        grid.add(projectName, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.5;
        label = new JLabel("Project Directory: ");
        grid.add(label, c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        projectDirectory = new JTextArea(System.getProperty("user.home"));
        projectDirectory.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        grid.add(projectDirectory, c);

        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.1;
        File openIconFile = new File(themeFolder, "open.png");
        ImageIcon openButtonIcon = new ImageIcon(openIconFile.getAbsolutePath());
        dirButton = new JButton(openButtonIcon);
        dirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                selectDirectory();
            }
        });
        grid.add(dirButton, c);

        Box line = Box.createHorizontalBox();
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                win.setVisible(false);
            }
        });
        line.add(cancelButton);
        exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                doExport();
                win.setVisible(false);
            }
        });
        line.add(exportButton);

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;

        grid.add(line, c);


        border.add(grid);

        win.getContentPane().add(gutter);
        win.pack();
        
        Dimension size = win.getSize();
        size.width = 450;
        win.setMinimumSize(size);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        win.setLocation((screen.width - size.width) / 2,
                          (screen.height - size.height) / 2);

        win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                win.setVisible(false);
            }
        });
        Base.registerWindowCloseKeys(win.getRootPane(), new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                win.setVisible(false);
            }
        });

        Base.setIcon(win);
    }

    public void selectDirectory()
    {
        JFileChooser fc = new JFileChooser(projectDirectory.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int rv = fc.showOpenDialog(null);

        if (rv == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f.exists() && f.isDirectory()) {
                projectDirectory.setText(f.getAbsolutePath());
            }
        }
    }

    public String getMenuTitle() {
        return "Export to MPLAB-X";
    }

    public String folderName() {
        folderNumber++;
        return new String("f" + folderNumber);
    }

    public void doExport() {
        try {
            String pn = projectName.getText();
            if (!pn.endsWith(".X")) {
                pn = pn + ".X";
            }

            folderNumber = 0;

            tokens.put("PROJECT", pn.substring(0, pn.length()-2));
            tokens.put("DEPENDS", "");
            tokens.put("TARGETS", "");
            tokens.put("DEBUGTARGETS", "");

            File dstDir = new File(projectDirectory.getText(), pn);
            dstDir.mkdirs();
            File nbproject = new File(dstDir, "nbproject");
            nbproject.mkdirs();

            Board board = editor.board;
            Core core = editor.core;

            if (core == null) {
                System.err.println("No core");
                return;
            }

            Sketch sketch = editor.getSketch();

            if (sketch == null) {
                System.err.println("No sketch");
                return;
            }

            File buildPath = sketch.getBuildFolder();

            sketch.prepare();

            ArrayList<File> sketchCPP = gatherFiles(sketch.getFolder(), ".cpp");
            ArrayList<File> sketchC = gatherFiles(sketch.getFolder(), ".c");
            ArrayList<File> sketchH = gatherFiles(sketch.getFolder(), ".h");
            ArrayList<File> sketchS = gatherFiles(sketch.getFolder(), ".S");
            Collection<Library> libs = sketch.getImportedLibraries();

            ArrayList<File> coreCPP = gatherFiles(core.getAPIFolder(), ".cpp");
            ArrayList<File> coreC = gatherFiles(core.getAPIFolder(), ".c");
            ArrayList<File> coreH = gatherFiles(core.getAPIFolder(), ".h");
            ArrayList<File> coreS = gatherFiles(core.getAPIFolder(), ".S");

            ArrayList<File> boardCPP = gatherFiles(board.getFolder(), ".cpp");
            ArrayList<File> boardC = gatherFiles(board.getFolder(), ".c");
            ArrayList<File> boardH = gatherFiles(board.getFolder(), ".h");
            ArrayList<File> boardS = gatherFiles(board.getFolder(), ".S");

            ArrayList<LibraryContainer> libraries = new ArrayList<LibraryContainer>();

            for (Library lib : libs) {
                libraries.add(new LibraryContainer(lib));
            }

            File mainFile = new File(buildPath, sketch.getName() + ".cpp");

            copyFilesToDirectory(sketchCPP, dstDir);
            copyFilesToDirectory(sketchC, dstDir);
            copyFilesToDirectory(sketchH, dstDir);
            copyFilesToDirectory(sketchS, dstDir);

            Base.copyFile(mainFile, new File(dstDir, mainFile.getName()));

            File coreDir = new File(dstDir, "core");
            coreDir.mkdir();

            copyFilesToDirectory(coreCPP, coreDir);
            copyFilesToDirectory(coreC, coreDir);
            copyFilesToDirectory(coreH, coreDir);
            copyFilesToDirectory(coreS, coreDir);

            File boardDir = new File(dstDir, "board");
            boardDir.mkdir();
  
            copyFilesToDirectory(boardCPP, boardDir);
            copyFilesToDirectory(boardC, boardDir);
            copyFilesToDirectory(boardH, boardDir);
            copyFilesToDirectory(boardS, boardDir);

            for (LibraryContainer lib : libraries) {
                lib.copyTo(dstDir);
            }

            StringBuilder objects = new StringBuilder();
            StringBuilder includes = new StringBuilder();
            includes.append("core;board");

            StringBuilder config = new StringBuilder();
            config.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            config.append("<configurationDescriptor version=\"62\">\n");
            config.append("  <logicalFolder name=\"root\" displayName=\"root\" projectFiles=\"true\">\n");
            config.append("    <logicalFolder name=\"HeaderFiles\" displayName=\"Header Files\" projectFiles=\"true\">\n");
            for (File f : sketchH) { config.append("      <itemPath>" + f.getName() + "</itemPath>\n"); }

            config.append("      <logicalFolder name=\"" + folderName() + "\" displayName=\"core\" projectFiles=\"true\">\n");
            for (File f : coreH) { config.append("        <itemPath>core" + File.separator + f.getName() + "</itemPath>\n"); }
            config.append("      </logicalFolder>\n");

            config.append("      <logicalFolder name=\"" + folderName() + "\" displayName=\"board\" projectFiles=\"true\">\n");
            for (File f : boardH) { config.append("        <itemPath>board" + File.separator + f.getName() + "</itemPath>\n"); }
            config.append("      </logicalFolder>\n");

            for (LibraryContainer lib : libraries) {
            config.append("      <logicalFolder name=\"" + folderName() + "\" displayName=\"" + lib.name + "\" projectFiles=\"true\">\n");
            for (File f : lib.hFiles) {
            config.append("        <itemPath>" + lib.name + File.separator + f.getName() + "</itemPath>\n");
            }
            if (lib.hasUtility) {
            if (lib.uhFiles.size() > 0) {
            config.append("        <logicalFolder name=\"" + folderName() + "\" displayName=\"utility\" projectFiles=\"true\">\n");
            for (File f : lib.uhFiles) {
            config.append("          <itemPath>" + lib.name + File.separator + "utility" + File.separator + f.getName() + "</itemPath>\n");
            }
            config.append("        </logicalFolder>\n");
            }
            }
            config.append("      </logicalFolder>\n");
            }

            config.append("    </logicalFolder>\n");
            config.append("    <logicalFolder name=\"LinkerScript\" displayName=\"Linker Files\" projectFiles=\"true\">\n");
            config.append("    </logicalFolder>\n");
            config.append("    <logicalFolder name=\"SourceFiles\" displayName=\"Source Files\" projectFiles=\"true\">\n");

            config.append("      <itemPath>" + mainFile.getName() + "</itemPath>\n");
            objects.append(mainFile.getName().replace(".cpp", ".o "));

            for (File f : sketchCPP) { 
                config.append("      <itemPath>" + f.getName() + "</itemPath>\n"); 
                objects.append(f.getName().replace(".cpp",".o "));
            }

            for (File f : sketchC) { 
                config.append("      <itemPath>" + f.getName() + "</itemPath>\n"); 
                objects.append(f.getName().replace(".c",".o "));
            }

            for (File f : sketchS) { 
                config.append("      <itemPath>" + f.getName() + "</itemPath>\n"); 
                objects.append(f.getName().replace(".S",".o "));
            }

            config.append("      <logicalFolder name=\"" + folderName() + "\" displayName=\"core\" projectFiles=\"true\">\n");

            for (File f : coreCPP) { 
                config.append("        <itemPath>core" + File.separator + f.getName() + "</itemPath>\n"); 
                objects.append("core" + File.separator + f.getName().replace(".cpp",".o "));
            }

            for (File f : coreC) { 
                config.append("        <itemPath>core" + File.separator + f.getName() + "</itemPath>\n"); 
                objects.append("core" + File.separator + f.getName().replace(".c",".o "));
            }

            for (File f : coreS) { 
                config.append("        <itemPath>core" + File.separator + f.getName() + "</itemPath>\n"); 
                objects.append("core" + File.separator + f.getName().replace(".S",".o "));
            }
            config.append("      </logicalFolder>\n");


            config.append("      <logicalFolder name=\"" + folderName() + "\" displayName=\"board\" projectFiles=\"true\">\n");

            for (File f : boardCPP) { 
                config.append("        <itemPath>board" + File.separator + f.getName() + "</itemPath>\n"); 
                objects.append("board" + File.separator + f.getName().replace(".cpp",".o "));
            }

            for (File f : boardC) { 
                config.append("        <itemPath>board" + File.separator + f.getName() + "</itemPath>\n"); 
                objects.append("board" + File.separator + f.getName().replace(".c",".o "));
            }

            for (File f : boardS) { 
                config.append("        <itemPath>board" + File.separator + f.getName() + "</itemPath>\n"); 
                objects.append("board" + File.separator + f.getName().replace(".S",".o "));
            }
            config.append("      </logicalFolder>\n");

            for (LibraryContainer lib : libraries) {
                includes.append(";" + lib.name);
                config.append("      <logicalFolder name=\"" + folderName() + "\" displayName=\"" + lib.name + "\" projectFiles=\"true\">\n");

                for (File f : lib.cppFiles) { 
                    config.append("        <itemPath>" + lib.name + File.separator + f.getName() + "</itemPath>\n"); 
                    objects.append(lib.name + File.separator + f.getName().replace(".cpp",".o "));
                }

                for (File f : lib.cFiles) { 
                    config.append("        <itemPath>" + lib.name + File.separator + f.getName() + "</itemPath>\n"); 
                    objects.append(lib.name + File.separator + f.getName().replace(".c",".o "));
                }

                for (File f : lib.sFiles) { 
                    config.append("        <itemPath>" + lib.name + File.separator + f.getName() + "</itemPath>\n"); 
                    objects.append(lib.name + File.separator + f.getName().replace(".S",".o "));
                }

                if (lib.hasUtility) {
                    if (lib.uhFiles.size() > 0) {
                        includes.append(";" + lib.name + File.separator + "utility");
                        config.append("        <logicalFolder name=\"" + folderName() + "\" displayName=\"utility\" projectFiles=\"true\">\n");
                        for (File f : lib.ucppFiles) { 
                            config.append("          <itemPath>" + lib.name + File.separator + "utility" + File.separator + f.getName() + "</itemPath>\n"); 
                            objects.append(lib.name + File.separator + "utility" + File.separator + f.getName().replace(".cpp",".o "));
                        }
                        for (File f : lib.ucFiles) { 
                            config.append("          <itemPath>" + lib.name + File.separator + "utility" + File.separator + f.getName() + "</itemPath>\n"); 
                            objects.append(lib.name + File.separator + "utility" + File.separator + f.getName().replace(".c",".o "));
                        }
                        for (File f : lib.usFiles) { 
                            config.append("          <itemPath>" + lib.name + File.separator + "utility" + File.separator + f.getName() + "</itemPath>\n"); 
                            objects.append(lib.name + File.separator + "utility" + File.separator + f.getName().replace(".S",".o "));
                        }
                        config.append("        </logicalFolder>\n");
                    }
                }
                config.append("      </logicalFolder>\n");
            }
            config.append("    </logicalFolder>\n");
            config.append("    <logicalFolder name=\"ExternalFiles\" displayName=\"Important Files\" projectFiles=\"false\">\n");
            config.append("      <itemPath>Makefile</itemPath>\n");
            config.append("    </logicalFolder>\n");
            config.append("  </logicalFolder>\n");
            config.append("  <projectmakefile>Makefile</projectmakefile>\n");
            config.append("  <confs>\n");
            config.append("    <conf name=\"default\" type=\"2\">\n");
            config.append("      <toolsSet>\n");
            config.append("        <developmentServer>localhost</developmentServer>\n");
            config.append("        <targetDevice>PIC" + board.get("build.mcu").toUpperCase() + "</targetDevice>\n");
            config.append("        <targetHeader></targetHeader>\n");
            config.append("        <targetPluginBoard></targetPluginBoard>\n");
            config.append("        <platformTool>PICkit3PlatformTool</platformTool>\n");
            config.append("        <languageToolchain>XC32</languageToolchain>\n");
            config.append("        <languageToolchainVersion>1.20</languageToolchainVersion>\n");
            config.append("        <platform>2</platform>\n");
            config.append("      </toolsSet>\n");
            config.append("      <compileType>\n");
            config.append("        <linkerTool>\n");
            config.append("          <linkerLibItems>\n");
            config.append("          </linkerLibItems>\n");
            config.append("        </linkerTool>\n");
            config.append("        <loading>\n");
            config.append("          <useAlternateLoadableFile>false</useAlternateLoadableFile>\n");
            config.append("          <alternateLoadableFile></alternateLoadableFile>\n");
            config.append("        </loading>\n");
            config.append("      </compileType>\n");
            config.append("      <makeCustomizationType>\n");
            config.append("        <makeCustomizationPreStepEnabled>false</makeCustomizationPreStepEnabled>\n");
            config.append("        <makeCustomizationPreStep></makeCustomizationPreStep>\n");
            config.append("        <makeCustomizationPostStepEnabled>false</makeCustomizationPostStepEnabled>\n");
            config.append("        <makeCustomizationPostStep></makeCustomizationPostStep>\n");
            config.append("        <makeCustomizationPutChecksumInUserID>false</makeCustomizationPutChecksumInUserID>\n");
            config.append("        <makeCustomizationEnableLongLines>false</makeCustomizationEnableLongLines>\n");
            config.append("        <makeCustomizationNormalizeHexFile>false</makeCustomizationNormalizeHexFile>\n");
            config.append("      </makeCustomizationType>\n");
            config.append("      <C32>\n");
            config.append("        <property key=\"extra-include-directories\" value=\"" + includes.toString() + "\"/>\n");
            config.append("        <property key=\"preprocessor-macros\" value=\"ARDUINO=" + core.get("core.version") + ";MPLAB=" + Base.REVISION + ";__CTYPE_NEWLIB\"/>\n");

            config.append("      </C32>\n");
            config.append("      <C32-AS>\n");
            config.append("      </C32-AS>\n");
            config.append("      <C32-LD>\n");
            config.append("      </C32-LD>\n");
            config.append("      <C32CPP>\n");
            config.append("        <property key=\"extra-include-directories\" value=\"" + includes.toString() + "\"/>\n");
            config.append("        <property key=\"preprocessor-macros\" value=\"ARDUINO=" + core.get("core.version") + ";MPLAB=" + Base.REVISION + ";__CTYPE_NEWLIB\"/>\n");

            config.append("      </C32CPP>\n");
            config.append("      <C32Global>\n");
            config.append("      </C32Global>\n");
            config.append("      <PICkit3PlatformTool>\n");
            config.append("      </PICkit3PlatformTool>\n");
            config.append("    </conf>\n");
            config.append("  </confs>\n");
            config.append("</configurationDescriptor>\n");


            tokens.put("OBJECTS", objects.toString());
            File configFile = new File(nbproject, "configurations.xml");
            PrintWriter pw = new PrintWriter(configFile);
            pw.print(config.toString());
            pw.close();

            config = new StringBuilder();

            config.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            config.append("<project xmlns=\"http://www.netbeans.org/ns/project/1\">\n");
            config.append("    <type>com.microchip.mplab.nbide.embedded.makeproject</type>\n");
            config.append("    <configuration>\n");
            config.append("        <data xmlns=\"http://www.netbeans.org/ns/make-project/1\">\n");
            config.append("            <name>" + sketch.getName() + "</name>\n");
            config.append("            <creation-uuid>" + UUID.randomUUID() + "</creation-uuid>\n");
            config.append("            <make-project-type>0</make-project-type>\n");
            config.append("            <c-extensions/>\n");
            config.append("            <cpp-extensions>cpp</cpp-extensions>\n");
            config.append("            <header-extensions/>\n");
            config.append("            <sourceEncoding>ISO-8859-1</sourceEncoding>\n");
            config.append("            <make-dep-projects/>\n");
            config.append("        </data>\n");
            config.append("    </configuration>\n");
            config.append("</project>\n");

            configFile = new File(nbproject, "project.xml");
            pw = new PrintWriter(configFile);
            pw.print(config.toString());
            pw.close();

            File privateDir = new File(nbproject, "private");
            if (!privateDir.exists()) {
                privateDir.mkdirs();
            }
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/Makefile-default.mk", new File(nbproject, "Makefile-default.mk"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/Makefile-local-default.mk", new File(nbproject, "Makefile-local-default.mk"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/private/configurations.xml", new File(privateDir, "configurations.xml"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/private/private.properties", new File(privateDir, "private.properties"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/project.properties", new File(nbproject, "project.properties"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/Package-default.bash", new File(nbproject, "Package-default.bash"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/Makefile-variables.mk", new File(nbproject, "Makefile-variables.mk"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/Makefile-impl.mk", new File(nbproject, "Makefile-impl.mk"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/nbproject/Makefile-genesis.properties", new File(nbproject, "Makefile-genesis.properties"));
            copyResourceToFile("uecide/plugin/ExportToMPLABX/Template.X/Makefile", new File(dstDir, "Makefile"));


        } catch (Exception e){
            Base.error(e);
        }
    }

    public class LibraryContainer {
        public ArrayList<File> cppFiles;
        public ArrayList<File> cFiles;
        public ArrayList<File> hFiles;
        public ArrayList<File> sFiles;

        public ArrayList<File> ucppFiles;
        public ArrayList<File> ucFiles;
        public ArrayList<File> uhFiles;
        public ArrayList<File> usFiles;

        public File utilityFolder;

        public File root;
        public String name;
        public boolean hasUtility;

        public Library library;

        public LibraryContainer(Library lib) {
            library = lib;
            this.root = lib.getFolder();
            this.name = root.getName();
            cppFiles = gatherFiles(root, ".cpp"); 
            cFiles = gatherFiles(root, ".c"); 
            hFiles = gatherFiles(root, ".h"); 
            sFiles = gatherFiles(root, ".s"); 

            hasUtility = false;
            utilityFolder = new File(root, "utility");
            if (utilityFolder.exists() && utilityFolder.isDirectory()) {
                hasUtility = true;
                ucppFiles = gatherFiles(utilityFolder, ".cpp");
                ucFiles = gatherFiles(utilityFolder, ".c");
                uhFiles = gatherFiles(utilityFolder, ".h");
                usFiles = gatherFiles(utilityFolder, ".s");
            } else {
                ucppFiles = null;
                ucFiles = null;
                uhFiles = null;
                usFiles = null;
            }
        } 

        public void copyTo(File dest) {
            File newRoot = new File(dest, name);
            File newUtility = null;

            if (!newRoot.exists()) {
                dest.mkdirs();
            }

            copyFilesToDirectory(cppFiles, newRoot);
            copyFilesToDirectory(cFiles, newRoot);
            copyFilesToDirectory(hFiles, newRoot);
            copyFilesToDirectory(sFiles, newRoot);

            if (utilityFolder.exists() && utilityFolder.isDirectory()) {
                newUtility = new File(newRoot, "utility");
                if (!newUtility.exists()) {
                    newUtility.mkdir();
                }
                copyFilesToDirectory(ucppFiles, newUtility);
                copyFilesToDirectory(ucFiles, newUtility);
                copyFilesToDirectory(uhFiles, newUtility);
                copyFilesToDirectory(usFiles, newUtility);
            }
        }
    }

    public void prettyPrintFileArray(String title, ArrayList<File> list) {
        System.err.println(title);
        for (File s : list) {
            System.err.println("  " + s.getAbsolutePath());
        }
    }

    public ArrayList<File> gatherFiles(File folder, final String type) {

        File[] list = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.toLowerCase().endsWith(type)) {
                    return true;
                }
                return false;
            }
        });

        ArrayList<File> out = new ArrayList<File>();

        for (File file : list) {
            out.add(file);
        }
        return out;
    }

    public void copyFilesToDirectory(ArrayList<File> list, File dest)
    {
        try {
            if (!dest.exists()) {
                dest.mkdirs();
            }
            for (File f : list) {
                File destFile = new File(dest, f.getName());
                Base.copyFile(f, destFile);
            }
        } catch (Exception e) {
            Base.error(e);
        }
    }
    
    public void copyResourceToFile(String resource, File dest) {
        try {
            InputStream in = getResourceAsStream(resource);
            if (in == null) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int nb;
            while ((nb = in.read(buffer)) > 0) {
                String t = new String(buffer, 0, nb);
                sb.append(t);
            }
            String out = sb.toString();

            String[] entries = (String[]) tokens.keySet().toArray(new String[0]);

            for (String token : entries) {
                out = out.replace("%%" + token + "%%", tokens.get(token));
            }

            PrintWriter pw = new PrintWriter(dest);
            pw.print(out);
            pw.close();
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public void run() {
        win.setVisible(true);   
    }
}
