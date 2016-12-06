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

    HashMap<String, String> familyNames = new HashMap<String, String>();

    public PluginManager() { 
        File dd = Base.getDataFolder();

        File aptFolder = new File(dd, "apt");
        if (!aptFolder.exists()) {
            aptFolder.mkdirs();
        }

        File dbFolder = new File(aptFolder, "db");
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        }
    
        File sourcesFile = new File(dbFolder, "sources.db");
        if  (!sourcesFile.exists()) {
            try {
                PrintWriter pw = new PrintWriter(sourcesFile);
                int i = 0;
                String key = "repository." + i;
                while (Preferences.get(key) != null) {
                    String repo = Preferences.get(key);
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
    JImageTextPane infoBrowser;
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

        String imageData = pe.get("Image");
        if (imageData != null) {
            try {
                byte[] binary = javax.xml.bind.DatatypeConverter.parseBase64Binary(imageData);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(binary));
                infoBrowser.setBackgroundImage(img);
                
            } catch (Exception e) {
            }
        } else {
            infoBrowser.setBackgroundImage(null);
        }

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

        try {
            String fp = Base.getTheme().getFontCSS("pluginmanager.browser.font.p");
            String fli = Base.getTheme().getFontCSS("pluginmanager.browser.font.li");
            String fh1 = Base.getTheme().getFontCSS("pluginmanager.browser.font.h1");
            String fh2 = Base.getTheme().getFontCSS("pluginmanager.browser.font.h2");
            String fh3 = Base.getTheme().getFontCSS("pluginmanager.browser.font.h3");

            longDesc = new Markdown4jProcessor()
                .addHtmlAttribute("style", fp, "p")
                .addHtmlAttribute("style", fli, "li")
                .addHtmlAttribute("style", fh1, "h1")
                .addHtmlAttribute("style", fh2, "h2")
                .addHtmlAttribute("style", fh3, "h3")
                .process(longDesc);
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

//    class RepoEntry extends JPanel {
//        PropertyFile props;
//        String key;
//        JCheckBox enabled;
//        JLabel nameLabel;
//        JTextArea descLabel;
//        JLabel locLabel;
//        JLabel flagLabel;
//
//        RepoEntry(PropertyFile p, String k) {
//            key = k;
//            props = p;
//
//
//            setLayout(new BorderLayout());
//            JPanel inner = new JPanel();
//            inner.setOpaque(false);
//            inner.setLayout(new BorderLayout());
//            inner.setBorder(new EmptyBorder(4, 4, 4, 4));
//
//            enabled = new JCheckBox();
//            add(enabled, BorderLayout.WEST);
//
//            nameLabel = new JLabel(props.get("name"));
//            inner.add(nameLabel, BorderLayout.NORTH);
//
//            if (!props.get("country").equals("NONE")) {
//                flagLabel = new JLabel(props.get("country"));
//                flagLabel.setIcon(Base.getIcon("flags", props.get("country").toLowerCase(), 16));
//                add(flagLabel, BorderLayout.EAST);
//            }
//
//            descLabel = new JTextArea(props.get("description"));
//            descLabel.setLineWrap(true);
//            descLabel.setWrapStyleWord(true);
//            descLabel.setEditable(false);
//            descLabel.setOpaque(false);
//            inner.add(descLabel, BorderLayout.CENTER);
//
//            add(inner, BorderLayout.CENTER);
//
//            Font f = nameLabel.getFont();
//            nameLabel.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
//
//            setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1));
//
//            enabled.setSelected(apt.hasSource(props.get("url"), props.get("codename")));
//
//            enabled.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    if (enabled.isSelected()) {
//                        apt.addSource(props.get("url"), props.get("codename"), getCleanOSName(), props.get("sections").split("::"));
//                    } else {
//                        apt.removeSource(props.get("url"), props.get("codename"));
//                    }
//                    apt.saveSources();
//                }
//            });
//        }
//
//
//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            Graphics2D g2d = (Graphics2D) g;
//            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//            int w = getWidth();
//            int h = getHeight();
//            Color color1 = Base.getTheme().getColor("pluginmanager.shade.top");
//            Color color2 = Base.getTheme().getColor("pluginmanager.shade.bottom");
//            GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
//            g2d.setPaint(gp);
//            g2d.fillRect(0, 0, w, h);
//        }
//
//    }
//
//    public void openRepoManager() {
//        JDialog repoManager = new JDialog(frame, JDialog.ModalityType.APPLICATION_MODAL);
//        repoManager.setTitle("Available Repositories");
//        JPanel repoContainer = new JPanel();
//        repoContainer.setLayout(new BorderLayout());
//
//        JPanel list = new JPanel();
//        list.setLayout(new BoxLayout(list, BoxLayout.PAGE_AXIS));
//
//        JScrollPane sb = new JScrollPane(list,
//            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
//            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//
//        repoContainer.add(sb, BorderLayout.CENTER);
//
//        File dd = Base.getDataFolder();
//        File reps = new File(dd, "apt/db/repositories.db");
//        if (!reps.exists()) {
//            Base.copyResourceToFile("/org/uecide/config/repositories.db", reps);
//        }
//
//        PropertyFile props = new PropertyFile(reps);
//
//        for (String key : props.childKeys()) {
//            PropertyFile pf = props.getChildren(key);
//            RepoEntry re = new RepoEntry(pf, key);
//            list.add(re);
//        }
//
//        repoContainer.add(sb, BorderLayout.CENTER);
//        repoManager.add(repoContainer);
//        repoManager.pack();
//        repoManager.setSize(new Dimension(500, 300));
//        repoManager.setLocationRelativeTo(frame);
//        repoManager.setVisible(true);
//    }

    public void openWindow(Editor ed) {
        openWindow(ed, false);
    }

    public void openWindow(Editor ed, boolean doUpdate) {
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

//        JMenuItem repoMenu = new JMenuItem("Repositories");
//        fileMenu.add(repoMenu);
//        repoMenu.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                openRepoManager();
//            }
//        });

        JMenuItem closeMenu = new JMenuItem("Close");
        fileMenu.add(closeMenu);
        closeMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                askCloseWindow();
            }
        });
        

        upper = new JPanel();
        lower = new JPanel();
        upper.setLayout(new BorderLayout());
        lower.setLayout(new BorderLayout());

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, lower);
        split.setResizeWeight(0.9D);
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

        PropertyFile fp = new PropertyFile(Base.getDataFile("apt/db/families.db"));

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
        infoBrowser = new JImageTextPane();
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
        if (doUpdate) {
            downloadPluginList();
        }
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
System.err.println("Downloading plugin list...");
                apt.update(this);
                Package p = apt.getPackage("uecide-families");
                if (p != null) {
                    if (!apt.isInstalled(p) || apt.isUpgradable(p)) {
                        apt.installPackage(p);
                    }
                }
                updateTree();
System.err.println("Done.");
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

    public void updateTree() {
        apt.initRepository();
        TreePath[] paths = editor.saveTreeState(tree);

        treeRoot.removeAllChildren();


        int fullNodeCount = 0;
        lastAdded = null;

        PropertyFile sections;

        File sf = Base.getDataFile("apt/db/sections.db");
        if (sf.exists()) {
            sections = new PropertyFile(Base.getDataFile("apt/db/sections.db"));
        } else {
            sections = new PropertyFile();
        }

        // Add any missing entries. This whole file could do with being moved to a plugin. That way
        // it can be updated at will in future without having to add new entries to the code.
        if (sections.get("plugins") == null) { sections.set("plugins", "Plugins"); }
        if (sections.get("boards") == null) { sections.set("boards", "Boards"); }
        if (sections.get("cores") == null) { sections.set("cores", "Cores"); }
        if (sections.get("compilers") == null) { sections.set("compilers", "Compilers"); }
        if (sections.get("programmers") == null) { sections.set("programmers", "Programmers"); }
        if (sections.get("extra") == null) { sections.set("extra", "System"); }
        if (sections.get("themes") == null) { sections.set("themes", "Themes"); }
        if (sections.get("libraries") == null) { sections.set("libraries", "Libraries"); }
        if (sections.get("repos") == null) { sections.set("repos", "Repositories"); }

        for (String key : sections.keySet()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(sections.get(key));
            fullNodeCount += addSectionToTree(node, key);
            if (node.getChildCount() > 0) {
                treeRoot.add(node);
            }
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
                p.fetchPackage(Base.getDataFile("apt/cache"));
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

                String reps[] = p.getReplaces();
                if (reps != null) {
                    for (String rep : reps) {
    System.err.println("Replace: " + rep);
                        Package rp = apt.getPackage(rep);
                        if (apt.isInstalled(rp)) {
                            apt.uninstallPackage(rp, true);
                        }
                    }
                }

                p.extractPackage(Base.getDataFile("apt/cache"), Base.getDataFile("apt/db/packages"), Base.getDataFolder());
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
