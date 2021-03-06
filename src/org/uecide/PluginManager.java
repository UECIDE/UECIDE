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

import org.uecide.*;
import org.uecide.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;
import java.beans.*;

import org.markdown4j.Markdown4jProcessor;

import com.wittams.gritty.swing.*;

public class PluginManager implements PropertyChangeListener
{

    APT apt;

    JDialog frame;
    JPanel mainContainer;
    JSplitPane split;
    JSplitPane treeSplit;
    JPanel upper;
    JPanel lower;
    TaskQueue queue;
    JScrollPane treeScroll;
    JTree tree;
    DefaultMutableTreeNode treeRoot;
    JToolBar toolbar;
    JButton refreshButton;
    DefaultTreeModel treeModel;
    JButton upgradeButton;
    boolean terminateEverything = false;

    JMenuBar menuBar;
    JMenu fileMenu;

    Editor editor;

    GrittyTerminal outputConsole;
    ConsoleTty outputTty;

    HashMap<String, String> familyNames = new HashMap<String, String>();

    static class ConsoleOutputStream extends OutputStream {
        ConsoleTty thisTty;

        public ConsoleOutputStream(ConsoleTty tty) {
            thisTty = tty;
        }
/*

        @Override
        public void write(byte[] b) {
            thisTty.feed(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            thisTty.feed(b);
        }
*/

        @Override
        public void write(int b) {
            thisTty.feed((byte)b);
        }
    }

    public PluginManager() throws FileNotFoundException, IOException { 
        File dd = Base.getDataFolder();

        File aptFolder = new File(dd, "apt");
        if (!aptFolder.exists()) {
            aptFolder.mkdirs();
        }

        File dbFolder = new File(aptFolder, "db");
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        }
    
        apt = new APT(dd);
    }

    JComboBox familySelector;
    JCheckBox onlyUninstalled;
    JTextField searchBox;
//    JImageTextPane infoBrowser;
    MarkdownPane infoBrowser;
    JScrollPane infoScroller;

    JButton localInstallButton;
    JButton localUninstallButton;
    JButton localUpgradeButton;
    JButton localHomepageButton;

    static class keyval {
        public String key;
        public String val;
        public keyval(String k, String v) {
            key = k;
            val = v;
        }
        public String toString() {
            return val;
        }
    }


    public String getCleanOSName() {
        if (Base.isMacOS()) {
            return "darwin-amd64";
        }
        if (Base.isLinux()) {
            return "linux-" + Base.getOSArch();
        }
        if (Base.isWindows()) {
            if (Base.getOSArch().equals("i386")) {
                return "windows-i386";
            }
            return "windows-amd64";
        }
        return "unknown-unknown";
    }

    public void updateDescription(Package pe) {
        String descString = pe.getDescription();
        if (descString == null) {
            descString = "Description Missing";
        }
        String[] descLines = descString.split("\n");
        String heading = descLines[0];
        String longDesc = "";

        for (int i = 1; i < descLines.length; i++) {
            String dl = descLines[i];
            if (dl.startsWith(" ")) {
                dl = dl.substring(1);
            }
            if (dl.equals(".")) {
                dl = "";
            }
            longDesc += dl + "\n";
        }
        longDesc = heading + "\n==========\n" + longDesc;

        if (pe.get("Provides") != null) {
            longDesc += "\n\n----\n\n";
            longDesc += "Usage:\n";
            longDesc += "------\n";
            longDesc += "    #include <" + pe.get("Provides").replace("-UL-","_") + ">\n";
        }

        longDesc += "\n\n----\n\n";
        longDesc += "* Package name: " + pe.getName() + "\n";
        longDesc += "* Available: ";
        longDesc += pe.getVersion();
        longDesc += "\n";
        Package ipe = apt.getInstalledPackage(pe.getName());
        if (ipe != null) {
            longDesc += "* Installed: ";
            longDesc += ipe.getVersion();
            longDesc += "\n";
        }
        if (pe.get("Softwareversion") != null) {
            longDesc += "* Content Version: ";
            longDesc += pe.get("Softwareversion");
            longDesc += "\n";
        }

        if (pe.get("Depends") != null) {
            longDesc += "* Depends: ";
            longDesc += pe.get("Depends");
            longDesc += "\n";
        }

        infoBrowser.setText(longDesc);
        infoBrowser.setCaretPosition(0);
        JScrollBar sb = infoScroller.getVerticalScrollBar();
        sb.setValue(sb.getMinimum());

        localInstallButton.setEnabled(!apt.isInstalled(pe));
        localUninstallButton.setEnabled(apt.isInstalled(pe));
        localUpgradeButton.setEnabled(apt.isUpgradable(pe));
        localInstallButton.setActionCommand(pe.getName());
        localUninstallButton.setActionCommand(pe.getName());
        localUpgradeButton.setActionCommand(pe.getName());
        localHomepageButton.setActionCommand(pe.get("Homepage"));
        localHomepageButton.setEnabled(pe.get("Homepage") != null);

    }

    public void openWindow(Editor ed) throws FileNotFoundException, IOException {
        openWindow(ed, false);
    }

    public void openWindow(Editor ed, boolean doUpdate) throws FileNotFoundException, IOException {
        editor = ed; 

        frame = new JDialog(editor, JDialog.ModalityType.APPLICATION_MODAL);
        frame.setTitle("Plugin Manager");
        mainContainer = new JPanel();
        mainContainer.setLayout(new BorderLayout());
        frame.add(mainContainer);

        terminateEverything = false;

        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        frame.add(menuBar, BorderLayout.NORTH);

        JMenuItem installPackage = new JMenuItem("Install Package...");
        fileMenu.add(installPackage);
        installPackage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                askInstallPackage();
            }
        });

        JMenuItem analysePackage = new JMenuItem("Analyse Packages");
        fileMenu.add(analysePackage);
        analysePackage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                analyseAllPackages();
            }
        });

        fileMenu.addSeparator();

        JMenuItem purgePackages = new JMenuItem("Purge Package Cache");
        fileMenu.add(purgePackages);
        purgePackages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                purgePackageCache();
            }
        });

        JMenuItem closeMenu = new JMenuItem("Close");
        fileMenu.add(closeMenu);
        closeMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    askCloseWindow();
                } catch (Exception ex) {
                    Base.exception(ex);
                }
            }
        });

        upper = new JPanel();
        lower = new JPanel();
        upper.setLayout(new BorderLayout());
        lower.setLayout(new BorderLayout());

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, lower);
        split.setResizeWeight(0.5D);
        mainContainer.add(split, BorderLayout.CENTER);

        queue = new TaskQueue();

        outputConsole = new GrittyTerminal();
        outputConsole.getTermPanel().setAntiAliasing(true);
        outputConsole.getTermPanel().setFont(Preferences.getFont("theme.console.fonts.comnmand"));
        outputTty = new ConsoleTty();

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(outputConsole.getTermPanel(), BorderLayout.CENTER);
        p.add(outputConsole.getScrollBar(), BorderLayout.EAST);

        lower.add(p);
        queue.addPropertyChangeListener(this);

        outputConsole.setTty(outputTty);
        outputConsole.start();

        ConsoleOutputStream stream = new ConsoleOutputStream(outputTty); 

        PrintStream ttyFeed = new PrintStream(stream);

        System.setOut(ttyFeed);
        System.setErr(ttyFeed);

        treeRoot = new DefaultMutableTreeNode("Plugins");
        treeModel = new DefaultTreeModel(treeRoot);
        tree = new JTree(treeModel);
        ToolTipManager.sharedInstance().registerComponent(tree);
        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        tree.setCellRenderer(new PluginTreeCellRenderer());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setIcon(IconManager.getIcon(24, "pim.refresh"));
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    downloadPluginList();
                } catch (Exception ex) { 
                    Base.exception(ex);
                    Base.error(ex); 
                }
            }
        });
        toolbar.add(refreshButton);

        upgradeButton = new JButton("Upgrade All");
        upgradeButton.setIcon(IconManager.getIcon(24, "pim.upgrade-all"));
        upgradeButton.setEnabled(true);
        upgradeButton.setToolTipText("Upgrade All Plugins");
        upgradeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                upgradeAll();
            }
        });
        toolbar.add(upgradeButton);

        toolbar.add(Box.createHorizontalGlue());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    askCloseWindow();
                } catch (Exception ex) {
                    Base.exception(ex);
                }
            }
        });
        //toolbar.add(closeButton);

        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout());

        treeScroll = new JScrollPane();
        treeScroll.setViewportView(tree);

        treePanel.add(toolbar, BorderLayout.NORTH);
        treePanel.add(treeScroll, BorderLayout.CENTER);

        JPanel topsection = new JPanel();
        topsection.setLayout(new BorderLayout());

        ArrayList<keyval> fams = new ArrayList<keyval>();

        PropertyFile fp = new PropertyFile(Base.getDataFile("apt/db/families.db"));

        fams.add(new keyval("all", "All Families"));
        for (String k : fp.keySet().toArray(new String[0])) {
            fams.add(new keyval(k, fp.get(k)));
        }

        JPanel optionsBox = new JPanel();

        familySelector = new JComboBox<keyval>(fams.toArray(new keyval[0]));
        familySelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    updateTree();
                } catch (Exception ex) { 
                    Base.exception(ex);
                    Base.error(ex); 
                }
            }
        });

        optionsBox.add(new JLabel("Family:"));
        optionsBox.add(familySelector);

        searchBox = new JTextField(10);
        searchBox.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }
            public void keyReleased(KeyEvent e) {
                try {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        updateTree();
                    }
                } catch (Exception ex) { 
                    Base.exception(ex);
                    Base.error(ex); 
                }
            }
            public void keyTyped(KeyEvent e) {
            }
        });
        optionsBox.add(new JLabel("Search:"));
        optionsBox.add(searchBox);
        

        onlyUninstalled = new JCheckBox("Hide Installed");
        onlyUninstalled.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    updateTree();
                } catch (Exception ex) { 
                    Base.exception(ex);
                    Base.error(ex); 
                }
            }
        });
        optionsBox.add(onlyUninstalled);
        topsection.add(optionsBox, BorderLayout.CENTER);

        upper.add(topsection, BorderLayout.NORTH);
        infoBrowser = new MarkdownPane();
        infoBrowser.setEditable(false);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());

        JToolBar packageToolbar = new JToolBar();
        packageToolbar.setFloatable(false);

        localInstallButton = new JButton("Install");
        localUninstallButton = new JButton("Uninstall");
        localUpgradeButton = new JButton("Upgrade");
        localHomepageButton = new JButton("Homepage");


        localInstallButton.setIcon(IconManager.getIcon(24, "pim.install"));
        localUninstallButton.setIcon(IconManager.getIcon(24, "pim.uninstall"));
        localUpgradeButton.setIcon(IconManager.getIcon(24, "pim.upgrade")); 
        localHomepageButton.setIcon(IconManager.getIcon(24, "pim.browser"));

        localInstallButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String pkg = e.getActionCommand();
                Package pe = apt.getPackage(pkg);
                if (pe != null) {
                    if (!apt.isInstalled(pe)) {
                        startDownload(pe);
                    }
                }
            }
        });

        localUpgradeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String pkg = e.getActionCommand();
                Package pe = apt.getPackage(pkg);
                if (pe != null) {
                    if (apt.isInstalled(pe)) {
                        startDownload(pe, false);
                    }
                }
            }
        });

        localUninstallButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String pkg = e.getActionCommand();
                Package pe = null; //apt.getPackage(pkg);

                for (Package p : apt.getInstalledPackages()) {
                    if (p.getName().equals(pkg)) {
                        pe = p;
                        break;
                    }
                }

                if (pe == null) {
                    System.err.println("Error: no package found to uninstall");
                    return;
                }

                if (pe != null) {
                    if (pe.get("Priority").equals("required")) {
                        Base.showMessage("Uninstall Error", pe.getName() + " is a required component.\nIt cannot be uninstalled.");
                    } else {
                        if (apt.isInstalled(pe)) {

                            Package[] deps = apt.getDependants(pe);
                            if (deps.length > 0) {
                                String o = pe.getName() + " is required by:\n";
                                for (Package dep : deps) {
                                    o += "    " + dep.getName() + "\n";
                                }
                                o += "Uninstalling will remove these packages as well.\nAre you sure you want to uninstall " + pe.getName() + "?";

                                Object[] options = {"Yes", "No"};


                                if (JOptionPane.showOptionDialog(frame, o, "Dependant Package", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]) == 1) {
                                    return;
                                }
                            } 
                            startUninstall(pe);
                        }
                    }
                }
            }
        });

        localHomepageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Utils.browse(e.getActionCommand());
            }
        });

        localInstallButton.setEnabled(false);
        localUninstallButton.setEnabled(false);
        localUpgradeButton.setEnabled(false);
        localHomepageButton.setEnabled(false);
        packageToolbar.add(localInstallButton);
        packageToolbar.add(localUninstallButton);
        packageToolbar.add(localUpgradeButton);
        packageToolbar.add(localHomepageButton);

        infoPanel.add(packageToolbar, BorderLayout.NORTH);

        infoScroller = new JScrollPane(infoBrowser,  
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        infoPanel.add(infoScroller, BorderLayout.CENTER);
        
        infoBrowser.setMargin(new Insets(10, 10, 10, 10));

        treeSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, infoPanel);
        treeSplit.setDividerLocation(250);
        upper.add(treeSplit, BorderLayout.CENTER);

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                if (e.getPath() != null) {
                    Object o = ((DefaultMutableTreeNode)e.getPath().getLastPathComponent()).getUserObject();
                    if (o instanceof Package) {
                        Package pe = (Package)o;
                        updateDescription(pe);
                    }
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    askCloseWindow();
                } catch (Exception ex) {
                    Base.exception(ex);
                }
            }
        });
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        if (apt.getPackageCount() == 0) {
            downloadPluginList();
        }
        updateTree();

        for (int i = 0; toolbar.getComponentAtIndex(i) != null; i++) {
            Component c = toolbar.getComponentAtIndex(i);
            if (c instanceof JButton) {
                JButton b = (JButton)c;
                b.setBorderPainted(false);
            }
        }

        frame.pack();
        if (doUpdate) {
            downloadPluginList();
        }
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(editor);
        frame.setVisible(true);

    }

    public void askCloseWindow() throws IOException {
        if (queue.getQueueSize() > 0) {
            Object[] options = {"Yes", "No"};
            int n = JOptionPane.showOptionDialog(
                frame, 
                "You have downloads in progress.\nAre you sure you want to close this window?\nClosing the window will terminate any downloads.",
                "Close window?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
            if (n != 0) {
                return;
            }
        }
        frame.dispose();
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));

        Base.cleanAndScanAllSettings();
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public void downloadPluginList() throws FileNotFoundException, IOException{
        QueueWorker updatePluginsTask = new QueueWorker() {
            @Override
            public String getTaskName() { return "Update Repository"; }

            @Override
            public String getActiveDescription() { return "Updating"; }

            @Override
            public String getQueuedDescription() { return "Update pending"; }
        
            @Override
            public Void doInBackground() {
                apt.update();
                Package p = apt.getPackage("uecide-families");
                if (p != null) {
                    if (!apt.isInstalled(p) || apt.isUpgradable(p)) {
                        try {
                            apt.installPackage(p);
                        } catch (Exception e) { 
                            Base.exception(e);
                            Base.error(e); 
                        }
                    }
                }
                try {
                    updateTree();
                } catch (Exception ex) { 
                    Base.exception(ex);
                    Base.error(ex);
                }
                return null;
            }

            @Override
            public void done() {
            }
        };

        queue.addTask(updatePluginsTask);
    }

    class PluginTreeCellRenderer implements TreeCellRenderer {
        DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            try {
                if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
                    JPanel container = new JPanel();
                    container.setLayout(new BorderLayout());
                    ImageIcon icon = null;
                    UIDefaults defaults = javax.swing.UIManager.getDefaults();
                    Color bg = defaults.getColor("List.selectionBackground");
                    Color fg = defaults.getColor("List.selectionForeground");
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Object userObject = node.getUserObject();
                    Border noBorder = BorderFactory.createEmptyBorder(0,0,0,0);
                    Border paddingBorder = BorderFactory.createEmptyBorder(2,2,2,2);

                    if (!leaf) {
                        JLabel text = new JLabel(node.toString());
                        if(expanded) {
                            icon = IconManager.getIcon(16, "tree.folder-open");
                        } else {
                            icon = IconManager.getIcon(16, "tree.folder-closed");
                        }

                        text.setBorder(paddingBorder);
                        if (selected) {
                            text.setBackground(bg);
                            text.setForeground(fg);
                            text.setOpaque(true);
                        } else {
                            text.setOpaque(false);
                        }

                        container.setOpaque(false);
                        container.setBorder(noBorder);
                        JLabel i = new JLabel(icon);
                        container.add(i, BorderLayout.WEST);
                        container.add(text, BorderLayout.CENTER);
                        return container;
                    }

                    if (userObject instanceof Package) {
                        Package pe = (Package)userObject;
                        String descString = pe.getDescription();
                        if (descString == null) {
                            descString = "Description Missing";
                        }
                        String[] descLines = descString.split("\n");
                        JLabel text = new JLabel(descLines[0]);
                        if (pe.getState() != 0) {
                            icon = IconManager.getIcon(16, "pim.busy");
                        } else if (!apt.isInstalled(pe)) {
                            icon = IconManager.getIcon(16, "pim.install");
                        } else if (apt.isUpgradable(pe)) {
                            icon = IconManager.getIcon(16, "pim.upgrade");
                        } else {
                            icon = IconManager.getIcon(16, "pim.installed");
                        }
                        text.setBorder(paddingBorder);
                        container.setOpaque(false);
                        text.setOpaque(false);

                        JLabel i = new JLabel(icon);
                        container.add(i, BorderLayout.WEST);
                        container.add(text, BorderLayout.CENTER);

                        if (selected) {
                            text.setBackground(bg);
                            text.setForeground(fg);
                            text.setOpaque(true);
                        } else {
                            text.setOpaque(false);
                        }

                        return container;
                    }

                }
            } catch (Exception ex) {
                Base.exception(ex);
            }
            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    };

    public Package lastAdded = null;

    public Package[] filterPackages(Package[] packages, String key, String value) {
        ArrayList<Package> out = new ArrayList<Package>();
        for (Package p : packages) {
            if (p.get(key) != null) {
                if (p.get(key).equals(value)) {
                    out.add(p);
                }
            } else {
                if (value == null) {
                    out.add(p);
                }
            }
        }
        return out.toArray(new Package[0]);
    }

    public String[] getKeyValues(Package[] packages, String key) {
        ArrayList<String> out = new ArrayList<String>();
        for (Package p : packages) {
            String value = p.get(key);
            if (value != null) {
                if (out.indexOf(value) == -1) {
                    out.add(value);
                }
            }
        }
        String[] o = out.toArray(new String[0]);
        Arrays.sort(o);
        return o;
    }

    public int addPackagesToNode(Package[] packages, DefaultMutableTreeNode node) {
        int addedNodes = 0;
        for (Package p : packages) {
            DefaultMutableTreeNode pNode = new DefaultMutableTreeNode(p);
            node.add(pNode);
            addedNodes++;
            lastAdded = p;
        }
        return addedNodes;
    }

    public Package[] searchPackages(Package[] packages, String search) {
        ArrayList<Package> out = new ArrayList<Package>();
        String[] bits = search.split(":");
        if (bits.length == 2) {
            bits[0] = bits[0].toLowerCase().trim();
            bits[1] = bits[1].trim();
            if (bits[0].equals("provides")) {
                for (Package p : packages) {
                    if (p.get("Provides") != null) {
                        if (p.get("Provides").equals(bits[1])) {
                            out.add(p);
                        }
                    }
                }
            }
        } else {
            for (Package p : packages) {
                String pVal = p.getName() + " " + p.getDescription();
                if (pVal.toLowerCase().contains(search.toLowerCase())) {
                    out.add(p);
                }
            }
        }
        return out.toArray(new Package[0]);
    }

    public Package[] filterPackagesByFamily(Package[] packages, String family) {
        ArrayList<Package> out = new ArrayList<Package>();
        for (Package p : packages) {
            if (p.get("Family") == null) {
                out.add(p);
            } else {
                if (p.get("Family").equals(family)) {
                    out.add(p);
                }
            }
        }
        return out.toArray(new Package[0]);
    }

    public int addGroupsToTree(DefaultMutableTreeNode root) {
        int fullNodeCount = 0;
        String searchCriteria = searchBox.getText();
        String familyCriteria = ((keyval)(familySelector.getSelectedItem())).key;

        Package[] packages = apt.getPackages();

        if (!searchCriteria.equals("")) {
            packages = searchPackages(packages, searchCriteria);
        }

        if (!familyCriteria.equals("all")) {
            packages = filterPackagesByFamily(packages, familyCriteria);
        }

        Arrays.sort(packages);

        String[] groups = getKeyValues(packages, "Group");

        for (String group : groups) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
            Package[] gPacks = filterPackages(packages, "Group", group);
            DefaultMutableTreeNode gNode = new DefaultMutableTreeNode(group);
            String[] subGroups = getKeyValues(gPacks, "Subgroup");
            for (String subGroup : subGroups) {
                Package[] sgPacks = filterPackages(gPacks, "Subgroup", subGroup);
                DefaultMutableTreeNode sgNode = new DefaultMutableTreeNode(subGroup);
                String[] subSubGroups = getKeyValues(sgPacks, "Subsubgroup");
                for (String subSubGroup : subSubGroups) {
                    Package[] ssgPacks = filterPackages(sgPacks, "Subsubgroup", subSubGroup);
                    DefaultMutableTreeNode ssgNode = new DefaultMutableTreeNode(subSubGroup);
                    fullNodeCount += addPackagesToNode(ssgPacks, ssgNode);
                    sgNode.add(ssgNode);
                }

                Package[] ssgNullPacks = filterPackages(sgPacks, "Subsubgroup", null);
                fullNodeCount += addPackagesToNode(ssgNullPacks, sgNode);
                if (sgNode.getChildCount() > 0) {
                    gNode.add(sgNode);
                }
            }
            Package[] sgNullPacks = filterPackages(gPacks, "Subgroup", null);
            fullNodeCount += addPackagesToNode(sgNullPacks, gNode);
            if (gNode.getChildCount() > 0) {
                root.add(gNode);
            }
        }
        return fullNodeCount;
    }

    public int addSectionToTree(DefaultMutableTreeNode node, String section)  {
        int fullNodeCount = 0;
        String searchCriteria = searchBox.getText();
        String familyCriteria = ((keyval)(familySelector.getSelectedItem())).key;

        Package[] packages = apt.getPackages(section);

        if (!searchCriteria.equals("")) {
            packages = searchPackages(packages, searchCriteria);
        }

        if (!familyCriteria.equals("all")) {
            packages = filterPackagesByFamily(packages, familyCriteria);
        }

        Arrays.sort(packages);

        String[] groups = getKeyValues(packages, "Group");

        for (String group : groups) {
            Package[] gPacks = filterPackages(packages, "Group", group);
            DefaultMutableTreeNode gNode = new DefaultMutableTreeNode(group);
            String[] subGroups = getKeyValues(gPacks, "Subgroup");
            for (String subGroup : subGroups) {
                Package[] sgPacks = filterPackages(gPacks, "Subgroup", subGroup);
                DefaultMutableTreeNode sgNode = new DefaultMutableTreeNode(subGroup);
                String[] subSubGroups = getKeyValues(sgPacks, "Subsubgroup");
                for (String subSubGroup : subSubGroups) {
                    Package[] ssgPacks = filterPackages(sgPacks, "Subsubgroup", subSubGroup);
                    DefaultMutableTreeNode ssgNode = new DefaultMutableTreeNode(subSubGroup);
                    fullNodeCount += addPackagesToNode(ssgPacks, ssgNode);
                    sgNode.add(ssgNode);
                }

                Package[] ssgNullPacks = filterPackages(sgPacks, "Subsubgroup", null);
                fullNodeCount += addPackagesToNode(ssgNullPacks, sgNode);
                if (sgNode.getChildCount() > 0) {
                    gNode.add(sgNode);
                }
            }

            Package[] sgNullPacks = filterPackages(gPacks, "Subgroup", null);
            fullNodeCount += addPackagesToNode(sgNullPacks, gNode);
            if (gNode.getChildCount() > 0) {
                node.add(gNode);
            }
        }
        Package[] gNullPacks = filterPackages(packages, "Group", null);
        fullNodeCount += addPackagesToNode(gNullPacks, node);
        return fullNodeCount;
    }

    public void updateTree() throws FileNotFoundException, IOException {
        apt.initRepository();
        TreePath[] paths = editor.saveTreeState(tree);

        treeRoot.removeAllChildren();

        int fullNodeCount = addGroupsToTree(treeRoot);

        TreeSet<Package> localPkg = new TreeSet<Package>();

        for (Package pkg : apt.getInstalledPackages()) {
            if (apt.getPackage(pkg.getName()) == null) {
                localPkg.add(pkg);
            }
        }

        if (localPkg.size() > 0) {
            DefaultMutableTreeNode loc = new DefaultMutableTreeNode("Local Packages");
            for (Package p : localPkg) {
                DefaultMutableTreeNode pn = new DefaultMutableTreeNode(p);
                loc.add(pn);
            }
            treeRoot.add(loc);
        }


        treeModel.nodeStructureChanged(treeRoot);

        if (fullNodeCount < 5) {
            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }
        } else {
            editor.restoreTreeState(tree, paths);
        }

        if (fullNodeCount == 1) {
            if (lastAdded != null) {
                updateDescription(lastAdded);
            }
        }

    }

    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("finished")) {
            QueueWorker[] finished = queue.getFinishedTasks();
            if (queue.getQueueSize() == 0) {
                System.out.println("Tasks complete");
            }
        }
    }

    public void startDownload(final Package e) {
        startDownload(e, true);
    }

    public void startDownload(Package pkg, boolean recurse) {

        pkg = apt.getPackage(pkg.getName());

        if (recurse) {
            Package[] deps = apt.resolveDepends(pkg);
            if (deps != null) {
                for (Package p : deps) {
                    if (!apt.isInstalled(p)) {
                        startDownload(p, false);
                    }
                }
            }
        }

        if (queue.getWorkerByName(pkg.getName()) != null) {
            return;
        }

        QueueWorker downloader = new QueueWorker() {

            boolean status;

            @Override
            public String getTaskName() {
                Package p = (Package)getUserObject();
                return p.getName();
            }

            @Override
            public String getQueuedDescription() {
                return "Download pending";
            }

            @Override
            public String getActiveDescription() {
                return "Downloading";
            }

            @Override
            public Void doInBackground() {
                Package p = (Package)getUserObject();
                status = p.fetchPackage(Base.getDataFile("apt/cache"));
                return null;
            }

            public void done() {
                if (status) startInstall((Package)getUserObject());
            }
        };

        downloader.setTaskCommand("download");
        downloader.setUserObject(pkg);
        pkg.setState(1);
        queue.addTask(downloader);
    }

    public void startUninstall(Package pkg) {

        QueueWorker downloader = new QueueWorker() {

            @Override
            public String getTaskName() {
                Package p = (Package)getUserObject();
                return p.getName();
            }

            @Override
            public String getQueuedDescription() {
                return "Uninstall pending";
            }

            @Override
            public String getActiveDescription() {
                return "Uninstalling";
            }

            @Override
            public Void doInBackground() {
                Package p = (Package)getUserObject();
                Package[] deps = apt.getDependants(p);
                if (deps.length > 0) {
                    for (Package dep : deps) {
                        apt.uninstallPackage(dep, false);
                    }
                }
                apt.uninstallPackage(p, false);
                return null;
            }

            public void done() {
                Package p = (Package)getUserObject();
                p.setState(0);
                try {
                    updateTree();
                } catch (Exception ex) { 
                    Base.exception(ex);
                    Base.error(ex); 
                }
            }
        };

        downloader.setTaskCommand("uninstall");
        downloader.setUserObject(pkg);
        pkg.setState(3);

        queue.addTask(downloader);
    }

    public void startInstall(Package pkg) {

        QueueWorker downloader = new QueueWorker() {

            @Override
            public String getTaskName() {
                Package p = (Package)getUserObject();
                return p.getName();
            }

            @Override
            public String getQueuedDescription() {
                return "Install pending";
            }

            @Override
            public String getActiveDescription() {
                return "Installing";
            }

            @Override
            public Void doInBackground() {
                Package p = (Package)getUserObject();
                if (apt.isInstalled(p)) {
                    apt.uninstallPackage(p, true);
                } 

                String reps[] = p.getReplaces();
                if (reps != null) {
                    for (String rep : reps) {
                        Package rp = apt.getPackage(rep);
                        if (apt.isInstalled(rp)) {
                            apt.uninstallPackage(rp, true);
                        }
                    }
                }

                p.extractPackage(Base.getDataFile("apt/cache"), Base.getDataFile("apt/db/packages"), Base.getDataFolder());
                return null;
            }

            public void done() {
                Package p = (Package)getUserObject();
                p.setState(0);
                try {
                    updateTree();
                } catch (Exception ex) { 
                    Base.exception(ex);
                    Base.error(ex); 
                }
            }
        };

        downloader.setTaskCommand("install");
        downloader.setUserObject(pkg);
        pkg.setState(2);
        queue.addTask(downloader);
    }

    public void upgradeAll() {
        Package[] outOfDate = apt.getUpgradeList();
        for (Package p : outOfDate) {
            startDownload(p, false);
        }
    }

    public Package findLibraryByInclude(Core core, String include) {
        Package[] packages = apt.getPackages("main");
        for (Package p : packages) {
            if (p.get("Provides") == null) {
                continue;
            }
            if (p.get("Family") != null) {
                if (!p.get("Family").equals("all")) {
                    if (!p.get("Family").equals(core.getFamily())) {
                        continue;
                    }
                }
            }
            if (p.get("Provides").replace("-UL-","_").equals(include)) {
                return p;
            }
        }
 
        return null;
    }

    public boolean isBusy() {
        return (queue.getQueueSize() > 0);
    } 

    public APT getApt() {
        return apt;
    }

    static class DebFileFilter extends javax.swing.filechooser.FileFilter {
        public boolean accept(File f) {
            if(f.getName().endsWith(".deb")) {
                return true;
            }

            if(f.isDirectory()) {
                return true;
            }

            return false;
        }

        public String getDescription() {
            return Base.i18n.string("filter.deb");
        }
    }

    public void askInstallPackage() {

        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        javax.swing.filechooser.FileFilter filter = new DebFileFilter();
        fc.setFileFilter(filter);

        int rv = fc.showOpenDialog(frame);
        if (rv == JFileChooser.APPROVE_OPTION) {
            final File f = fc.getSelectedFile();
            if (f.exists()) {
                QueueWorker installPackageTask = new QueueWorker() {
                    @Override
                    public String getTaskName() { return "Extract " + f.getName(); }

                    @Override
                    public String getActiveDescription() { return "Extracting"; }

                    @Override
                    public String getQueuedDescription() { return "Extract pending"; }

                    @Override
                    public Void doInBackground() {
                        try {
                            DebFile df = new DebFile(f);
                            String pn = df.getPackageName();
                            if (pn == null) {
                                Base.error("Error: invalid package file!");
                                return null;
                            }
                            System.out.print("Installing " + pn + " ... ");
                            File pf = new File(Base.getDataFile("apt/db/packages"), pn);
                            pf.mkdirs();
                            df.extract(pf, Base.getDataFolder());
                            System.out.println("done");
                        } catch (Exception e) {
                            Base.exception(e);
                            Base.error(e);
                        }

                        return null;
                    }

                    @Override
                    public void done() {
                    }
                };

                queue.addTask(installPackageTask);

            }
        }
    }

    public void analyseAllPackages() {
        PackageAnalyser pa = new PackageAnalyser();
        pa.openWindow(frame);
    }

    public void purgePackageCache() {
        System.out.print("Purging package cache ... ");
        File cacheFolder = Base.getDataFile("apt/cache");
        if (cacheFolder.exists()) {
            if (!cacheFolder.isDirectory()) {
                cacheFolder.delete();
            }
        }

        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }

        File[] list = cacheFolder.listFiles();
        for (File f : list) {
            if (f.getName().endsWith(".deb")) {
                f.delete();
            }
        }

        System.out.println("done");
    }
}
