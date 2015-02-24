package org.uecide;

import org.uecide.*;
import org.uecide.debug.*;
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
import java.awt.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;
import java.beans.*;

import uk.co.majenko.apt.*;
import uk.co.majenko.apt.Package;

import org.markdown4j.Markdown4jProcessor;

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

    Editor editor;

    HashMap<String, String> familyNames = new HashMap<String, String>();

    public PluginManager() { 
        File dd = Base.getSettingsFolder();

        File aptFolder = new File(dd, "apt");
        if (!aptFolder.exists()) {
            System.err.println("Making apt folder");
            aptFolder.mkdirs();
        }

        File dbFolder = new File(aptFolder, "db");
        if (!dbFolder.exists()) {
            System.err.println("Making db folder");
            dbFolder.mkdirs();
        }
    
        File sourcesFile = new File(dbFolder, "sources.db");
        if  (!sourcesFile.exists()) {
            try {
                System.err.println("Making sources file");
                PrintWriter pw = new PrintWriter(sourcesFile);
                int i = 0;
                String key = "repository." + i;
                while (Base.preferences.get(key) != null) {
                    String repo = Base.preferences.get(key);
                    String parts[] = repo.split("::");
                    String url = parts[0];
                    String codename = parts[1];
                    String sections = parts[2];
                    String sects[] = sections.split(",");


                    pw.print("deb " + url + " " + codename);
                    for (String s : sects) {
                        pw.print(" " + s);
                    }
                    pw.println();
                    
                    i++;
                    key = "repository." + i;
                }
                pw.close();
            } catch (Exception e) {
                Base.error(e);
            }
        }

        apt = new APT(dd);

    }

    JComboBox familySelector;
    JCheckBox onlyUninstalled;
    JTextField searchBox;
    JTextPane infoBrowser;
    JScrollPane infoScroller;

    JButton localInstallButton;
    JButton localUninstallButton;
    JButton localUpgradeButton;
    JButton localHomepageButton;

    class keyval {
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

        try {
            longDesc = new Markdown4jProcessor().process(longDesc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        infoBrowser.setText("<html><body>" + longDesc + "</body></html>");
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

    public void openWindow(Editor ed) {
        editor = ed; 

        frame = new JDialog(editor, JDialog.ModalityType.APPLICATION_MODAL);
        mainContainer = new JPanel();
        mainContainer.setLayout(new BorderLayout());
        frame.add(mainContainer);

        terminateEverything = false;

        upper = new JPanel();
        lower = new JPanel();
        upper.setLayout(new BorderLayout());
        lower.setLayout(new BorderLayout());

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, lower);
        split.setResizeWeight(1D);
        split.setDividerLocation(500);
        mainContainer.add(split, BorderLayout.CENTER);

        queue = new TaskQueue();
        lower.add(queue, BorderLayout.CENTER);
        queue.addPropertyChangeListener(this);

        treeRoot = new DefaultMutableTreeNode("Plugins");
        treeModel = new DefaultTreeModel(treeRoot);
        tree = new JTree(treeModel);
        ToolTipManager.sharedInstance().registerComponent(tree);
        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        tree.setCellRenderer(new PluginTreeCellRenderer());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setIcon(Base.getIcon("actions", "refresh", 24));
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.err.println("Getting initial package list");
                downloadPluginList();
            }
        });
        toolbar.add(refreshButton);

        upgradeButton = new JButton("Upgrade All");
        upgradeButton.setIcon(Base.getIcon("actions", "upgrade-all", 24));
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
                askCloseWindow();
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

        PropertyFile fp = new PropertyFile(Base.getSettingsFile("apt/db/families.db"));

        fams.add(new keyval("all", "All Families"));
        for (String k : fp.keySet().toArray(new String[0])) {
            fams.add(new keyval(k, fp.get(k)));
        }

        JPanel optionsBox = new JPanel();

        familySelector = new JComboBox(fams.toArray(new keyval[0]));
        familySelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTree();
            }
        });

        optionsBox.add(new JLabel("Family:"));
        optionsBox.add(familySelector);

        searchBox = new JTextField(10);
        searchBox.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }
            public void keyReleased(KeyEvent e) {
                updateTree();
            }
            public void keyTyped(KeyEvent e) {
            }
        });
        optionsBox.add(new JLabel("Search:"));
        optionsBox.add(searchBox);
        

        onlyUninstalled = new JCheckBox("Hide Installed");
        onlyUninstalled.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTree();
            }
        });
        optionsBox.add(onlyUninstalled);
        topsection.add(optionsBox, BorderLayout.CENTER);

        upper.add(topsection, BorderLayout.NORTH);
        infoBrowser = new JTextPane();
        infoBrowser.setContentType("text/html");
        infoBrowser.setEditable(false);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());

        JToolBar packageToolbar = new JToolBar();
        packageToolbar.setFloatable(false);

        localInstallButton = new JButton("Install");
        localUninstallButton = new JButton("Uninstall");
        localUpgradeButton = new JButton("Upgrade");
        localHomepageButton = new JButton("Homepage");


        localInstallButton.setIcon(Base.getIcon("actions", "install", 24));
        localUninstallButton.setIcon(Base.getIcon("actions", "close", 24));
        localUpgradeButton.setIcon(Base.getIcon("actions", "upgrade", 24)); 
        localHomepageButton.setIcon(Base.getIcon("apps", "web", 24));

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
                Package pe = apt.getPackage(pkg);
                if (pe != null) {
                    if (pe.get("Priority").equals("required")) {
                        Base.showMessage("Uninstall Error", pe.getName() + " is a required component.\nIt cannot be uninstalled.");
                    } else {
                        if (apt.isInstalled(pe)) {
                            startUninstall(pe);
                        }
                    }
                }
            }
        });

        localHomepageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.openURL(e.getActionCommand());
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
                askCloseWindow();
            }
        });
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        if (apt.getPackageCount() == 0) {
            downloadPluginList();
//            apt.update();
//            Package p = apt.getPackage("uecide-families");
//            if (p != null) {
//                if (!apt.isInstalled(p) || apt.isUpgradable(p)) {
//                    apt.installPackage(p);
//                }
//            }
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
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(editor);
        frame.setVisible(true);
    }

    public void askCloseWindow() {
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
        Base.cleanAndScanAllSettings();
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public void downloadPluginList() {
        QueueWorker updatePluginsTask = new QueueWorker() {
            @Override
            public String getTaskName() { return "Update Repository"; }

            @Override
            public String getActiveDescription() { return "Updating"; }

            @Override
            public String getQueuedDescription() { return "Update pending"; }
        
            @Override
            public Void doInBackground() {
                apt.update(this);
                Package p = apt.getPackage("uecide-families");
                if (p != null) {
                    if (!apt.isInstalled(p) || apt.isUpgradable(p)) {
                        apt.installPackage(p);
                    }
                }
                updateTree();
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
                        icon = Base.getIcon("bookmarks", "folder-open", 16);
                    } else {
                        icon = Base.getIcon("bookmarks", "folder", 16);
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
                        icon = Base.getIcon("flags", "busy", 16);
                    } else if (!apt.isInstalled(pe)) {
                        icon = Base.getIcon("actions", "install", 16);
                    } else if (apt.isUpgradable(pe)) {
                        icon = Base.getIcon("actions", "upgrade", 16);
                    } else {
                        icon = Base.getIcon("flags", "yes", 16);
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

                    StringBuilder tt = new StringBuilder();

                    return container;
                }

            }
            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    };

    public void updateTree() {
        apt.initRepository();
        TreePath[] paths = editor.saveTreeState(tree);

        treeRoot.removeAllChildren();

        String searchCriteria = searchBox.getText().toLowerCase();
        String searchCriteriaCase = searchBox.getText();
        boolean doSearch = !searchCriteria.equals("");

        int fullNodeCount = 0;
        Package lastAdded = null;

        DefaultMutableTreeNode pluginsNode = new DefaultMutableTreeNode("Plugins");
        Package[] packages = apt.getPackages("plugins");
        for (Package p : packages) {
            if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                continue;
            }
            if (doSearch) {
                if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                    continue;
                }
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
            node.setUserObject(p);
            pluginsNode.add(node);
        }
        if (pluginsNode.getChildCount() > 0) {
            treeRoot.add(pluginsNode);
        }

        DefaultMutableTreeNode libsNode = new DefaultMutableTreeNode("Libraries");
        String[] groups = apt.getUnique("libraries", "Group");
        for (String group : groups) {
            DefaultMutableTreeNode gnode = new DefaultMutableTreeNode(group);
            packages = apt.getEqual("libraries", "Group", group);
            ArrayList<String> subGroups = new ArrayList<String>();
            for (Package p : packages) {
                String subGroup = p.get("Subgroup");
                if (subGroup == null) {
                    continue;
                }
                if (subGroups.indexOf(subGroup) == -1) {
                    subGroups.add(subGroup);
                }
            }

            int groupCount = 0;

            for (String subGroup : subGroups) {
                DefaultMutableTreeNode subGroupNode = new DefaultMutableTreeNode(subGroup);
                for (Package p : packages) {
                    if (p.get("Subgroup") == null) {
                        continue;
                    }
                    if (!p.get("Subgroup").equals(subGroup)) {
                        continue;
                    }
                    if (
                        (p.get("Family") == null) ||
                        (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                        (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
                    ) {
                        if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                            continue;
                        }
                        if (doSearch) {
                            if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                } 
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                                continue;
                            }
                        }
                        DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                        node.setUserObject(p);
                        subGroupNode.add(node);
                        lastAdded = p;
                        fullNodeCount++;
                    }
                }
                if (subGroupNode.getChildCount() > 0) {
                    gnode.add(subGroupNode);
                }
            }

            for (Package p : packages) {
                if (p.get("Subgroup") != null) {
                    continue;
                }
                if (
                    (p.get("Family") == null) ||
                    (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                    (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
                ) {
                    if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                        continue;
                    }
                    if (doSearch) {
                            if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                }
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                            continue;
                        }
                    }
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                    node.setUserObject(p);
                    gnode.add(node);
                    lastAdded = p;
                    fullNodeCount++;
                }
            }
            if (gnode.getChildCount() > 0) {
                libsNode.add(gnode);
            }
        }
        packages = apt.getEqual("libraries", "Group", null);
        for (Package p : packages) {
            if (
                (p.get("Family") == null) ||
                (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
            ) {
                    if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                        continue;
                    }
                    if (doSearch) {
                        if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                }
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                            continue;
                        }
                    }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                node.setUserObject(p);
                libsNode.add(node);
                lastAdded = p;
                fullNodeCount++;
            }
        }
        if (libsNode.getChildCount() > 0) {
            treeRoot.add(libsNode);
        }

        DefaultMutableTreeNode boardsNode = new DefaultMutableTreeNode("Boards");
        groups = apt.getUnique("boards", "Group");
        for (String group : groups) {
            DefaultMutableTreeNode gnode = new DefaultMutableTreeNode(group);
            packages = apt.getEqual("boards", "Group", group);
            int count = 0;
            for (Package p : packages) {
                if (
                    (p.get("Family") == null) ||
                    (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                    (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
                ) {
                    if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                        continue;
                    }
                    if (doSearch) {
                        if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                }
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                            continue;
                        }
                    }
                    count++;
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                    node.setUserObject(p);
                    gnode.add(node);
                    lastAdded = p;
                    fullNodeCount++;
                }
            }
            if (count > 0) {
                boardsNode.add(gnode);
            }
        }
        packages = apt.getEqual("boards", "Group", null);
        for (Package p : packages) {
            if (
                (p.get("Family") == null) ||
                (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
            ) {
                    if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                        continue;
                    }
                    if (doSearch) {
                        if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                }
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                            continue;
                        }
                    }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                node.setUserObject(p);
                boardsNode.add(node);
                lastAdded = p;
                fullNodeCount++;
            }
        }
        if (boardsNode.getChildCount() > 0) {
            treeRoot.add(boardsNode);
        }

        DefaultMutableTreeNode coresNode = new DefaultMutableTreeNode("Cores");
        groups = apt.getUnique("cores", "Group");
        for (String group : groups) {
            DefaultMutableTreeNode gnode = new DefaultMutableTreeNode(group);
            packages = apt.getEqual("cores", "Group", group);
            int count = 0;
            for (Package p : packages) {
                if (
                    (p.get("Family") == null) ||
                    (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                    (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
                ) {
                    if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                        continue;
                    }
                    if (doSearch) {
                        if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                }
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                            continue;
                        }
                    }
                    count++;
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                    node.setUserObject(p);
                    lastAdded = p;
                    fullNodeCount++;
                    gnode.add(node);
                }
            }
            if (count > 0) {
                coresNode.add(gnode);
            }
        }
        packages = apt.getEqual("cores", "Group", null);
        for (Package p : packages) {
            if (
                (p.get("Family") == null) ||
                (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
            ) {
                    if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                        continue;
                    }
                    if (doSearch) {
                        if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                }
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                            continue;
                        }
                    }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                node.setUserObject(p);
                lastAdded = p;
                fullNodeCount++;
                coresNode.add(node);
            }
        }
        if (coresNode.getChildCount() > 0) {
            treeRoot.add(coresNode);
        }

        DefaultMutableTreeNode compilersNode = new DefaultMutableTreeNode("Compilers");
        groups = apt.getUnique("compilers", "Group");
        for (String group : groups) {
            DefaultMutableTreeNode gnode = new DefaultMutableTreeNode(group);
            packages = apt.getEqual("compilers", "Group", group);
            int count =0;
            for (Package p : packages) {
                if (
                    (p.get("Family") == null) ||
                    (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                    (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
                ) {
                    if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                        continue;
                    }
                    if (doSearch) {
                        if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                }
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                            continue;
                        }
                    }
                    count++;
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                    node.setUserObject(p);
                    lastAdded = p;
                    fullNodeCount++;
                    gnode.add(node);
                }
            }
            if (count > 0) {
                compilersNode.add(gnode);
            }
        }
        packages = apt.getEqual("compilers", "Group", null);
        for (Package p : packages) {
            if (
                (p.get("Family") == null) ||
                (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
            ) {
                    if (apt.isInstalled(p) && onlyUninstalled.isSelected()) {
                        continue;
                    }
                    if (doSearch) {
                        if (searchCriteria.startsWith("provides:")) {
                                if (p.get("Provides") == null) {
                                    continue;
                                }
                                if (!p.get("Provides").replace("-UL-","_").equals(searchCriteriaCase.substring(9).trim())) {
                                    continue;
                                }
                            } else if ((!p.getName().toLowerCase().contains(searchCriteria)) && (!p.getDescription().toLowerCase().contains(searchCriteria))) {
                            continue;
                        }
                    }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                node.setUserObject(p);
                lastAdded = p;
                fullNodeCount++;
                compilersNode.add(node);
            }
        }

        if (compilersNode.getChildCount() > 0) {
            treeRoot.add(compilersNode);
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
                p.attachPercentageListener(this);
                p.fetchPackage(Base.getSettingsFile("apt/cache"));
                p.detachPercentageListener();
                return null;
            }

            public void done() {
                startInstall((Package)getUserObject());
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
                p.attachPercentageListener(this);
                String ret = apt.uninstallPackage(p, false);
                p.detachPercentageListener();
                if (ret != null) {
                    Base.showMessage("Uninstall error", ret);
                }
                return null;
            }

            public void done() {
                Package p = (Package)getUserObject();
                p.setState(0);
                updateTree();
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
                p.attachPercentageListener(this);
                if (apt.isInstalled(p)) {
                    String ret = apt.uninstallPackage(p, true);
                } 
                p.extractPackage(Base.getSettingsFile("apt/cache"), Base.getSettingsFile("apt/db/packages"), Base.getSettingsFolder());
                p.detachPercentageListener();
                return null;
            }

            public void done() {
                Package p = (Package)getUserObject();
                p.setState(0);
                updateTree();
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
        Package[] packages = apt.getPackages("libraries");
        for (Package p : packages) {
            if (p.get("Provides") == null) {
                continue;
            }
            if (p.get("Family") != null) {
                if (!p.get("Family").equals(core.getFamily())) {
                    continue;
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

}
