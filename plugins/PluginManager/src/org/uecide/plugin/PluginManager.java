package org.uecide.plugin;

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

public class PluginManager extends Plugin implements PropertyChangeListener
{

    APT apt;

    JDialog frame;
    JPanel mainContainer;
    JSplitPane split;
    JPanel upper;
    JPanel lower;
    TaskQueue queue;
    JScrollPane treeScroll;
    JTree tree;
    DefaultMutableTreeNode treeRoot;
    JToolBar toolbar;
    JButton refreshButton;
    DefaultTreeModel treeModel;
    JButton installButton;
    JButton uninstallButton;
    JButton upgradeButton;
    boolean terminateEverything = false;

    HashMap<String, String> familyNames = new HashMap<String, String>();

    HashMap<String, JSONObject> availablePlugins = new HashMap<String, JSONObject>();
    HashMap<String, JSONObject> availableCores = new HashMap<String, JSONObject>();
    HashMap<String, JSONObject> availableBoards = new HashMap<String, JSONObject>();
    HashMap<String, JSONObject> availableCompilers = new HashMap<String, JSONObject>();

    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    public PluginManager(Editor e) { editor = e; }
    public PluginManager(EditorBase e) { editorTab = e; }

    JComboBox familySelector;

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

    public void openWindow() {
        apt = new APT(Base.getSettingsFolder());

        int i = 0;
        String key = "repository." + i;
        while (Base.preferences.get(key) != null) {
            String repo = Base.preferences.get(key);
            String parts[] = repo.split("::");
            String url = parts[0];
            String codename = parts[1];
            String sections = parts[2];
            String sects[] = sections.split(",");

            Source s = new Source(url,codename, getCleanOSName(), sects);
            apt.addSource(s);
            i++;
            key = "repository." + i;
        }

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

        tree.setCellRenderer(new PluginTreeCellRenderer());

        JButton refreshButton = new JButton(Base.loadIconFromResource("/org/uecide/plugin/PluginManager/toolbar/view-refresh.png"));
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                downloadPluginList();
            }
        });
        toolbar.add(refreshButton);

        installButton = new JButton(Base.loadIconFromResource("/org/uecide/plugin/PluginManager/toolbar/install.png"));
        installButton.setEnabled(false);
        installButton.setToolTipText("Install Plugin");
        installButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (TreePath path : tree.getSelectionPaths()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                    if (node != null) {
                        if (node.getUserObject() instanceof Package) {
                            startDownload((Package)node.getUserObject());
                        } else if (node.getUserObject() instanceof String) {

                            int kids = node.getChildCount();
                            for (int i = 0; i < kids; i++) {
                                DefaultMutableTreeNode kid = (DefaultMutableTreeNode)node.getChildAt(i);
                                if (kid.getUserObject() instanceof Package) {
                                    Package pe = (Package)kid.getUserObject();
                                    if (!apt.isInstalled(pe)) {
                                        startDownload(pe);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        toolbar.add(installButton);

        uninstallButton = new JButton(Base.loadIconFromResource("/org/uecide/plugin/PluginManager/toolbar/uninstall.png"));
        uninstallButton.setEnabled(false);
        uninstallButton.setToolTipText("Uninstall Plugin");
        uninstallButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (TreePath path : tree.getSelectionPaths()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                    if (node != null) {
                        if (node.getUserObject() instanceof Package) {
                            Package pe = (Package)node.getUserObject();
                            startUninstall(pe);
                        }
                    }
                }
            }
        });
        toolbar.add(uninstallButton);

        upgradeButton = new JButton(Base.loadIconFromResource("/org/uecide/plugin/PluginManager/toolbar/upgradeall.png"));
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
        toolbar.add(closeButton);

        treeScroll = new JScrollPane();
        treeScroll.setViewportView(tree);

        JPanel topsection = new JPanel();
        topsection.setLayout(new BorderLayout());

        topsection.add(toolbar, BorderLayout.NORTH);

        ArrayList<keyval> fams = new ArrayList<keyval>();

        PropertyFile fp = new PropertyFile(Base.getSettingsFile("apt/db/families.db"));

        fams.add(new keyval("all", "All Families"));
        for (String k : fp.keySet().toArray(new String[0])) {
            fams.add(new keyval(k, fp.get(k)));
        }


        familySelector = new JComboBox(fams.toArray(new keyval[0]));
        familySelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTree();
            }
        });

        topsection.add(familySelector, BorderLayout.CENTER);

        upper.add(topsection, BorderLayout.NORTH);
        upper.add(treeScroll, BorderLayout.CENTER);
        
        frame.setSize(new Dimension(600, 600));
        frame.setPreferredSize(new Dimension(600, 600));

        Dimension eSize = editor.getSize();
        frame.setLocationRelativeTo(editor);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                askCloseWindow();
            }
        });
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        if (apt.getPackageCount() == 0) {
            apt.update();
            Package p = apt.getPackage("uecide-families");
            if (p.isValid) {
                if (!apt.isInstalled(p) || apt.isUpgradable(p)) {
                    apt.installPackage(p);
                }
            }
        }
        updateTree();

//        downloadPluginList();
        for (i = 0; toolbar.getComponentAtIndex(i) != null; i++) {
            Component c = toolbar.getComponentAtIndex(i);
            if (c instanceof JButton) {
                JButton b = (JButton)c;
                b.setBorderPainted(false);
            }
        }

        frame.pack();
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

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_TOOLS | Plugin.MENU_TOP)) {
            JMenuItem item = new JMenuItem("Plugin Manager");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openWindow();
                }
            });
            menu.add(item);
        }
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
                apt.update();
                Package p = apt.getPackage("uecide-families");
                if (p.isValid) {
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
        refreshTree();
    }

    public boolean inSet(String needlestack, String haystack) {
        String[] straws = haystack.split(",");
        String[] needles = needlestack.split(",");
        for (String straw : straws) {
            for (String needle : needles) {
                if (needle.equals(straw)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String[] getGroups(HashMap<String, JSONObject>map, String family) {
        ArrayList<String> groups = new ArrayList<String>();
        for (String item : map.keySet()) {

            @SuppressWarnings("unchecked")
            HashMap<String, String>data = (HashMap<String, String>)map.get(item);
            String f = data.get("Family");
            String g = data.get("Group");
            if (inSet(f, family)) {
                if (groups.indexOf(g) == -1) {
                    groups.add(g);
                }
            }
        }
        String[] arr = groups.toArray(new String[groups.size()]);
        Arrays.sort(arr);
        return arr;
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
                    if (expanded) {
                        icon = Base.loadIconFromResource("files/folder-open.png");
                    } else {
                        icon = Base.loadIconFromResource("files/folder.png");
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
                    if (selected) {
                        if (userObject instanceof String) {
                            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
                            if (parent != null) {
                                Object puo = parent.getUserObject();
                                if (puo != null) {
                                    if (puo instanceof String) {
                                        String pname = (String)puo;
                                        String uname = (String)userObject;
                                        if (pname.equals("Boards")) {
                                            // Can install all boards
                                            installButton.setEnabled(true);
                                        } else if (uname.equals("Cores")) {
                                            // Can install all cores
                                            installButton.setEnabled(true);
                                        } else if (uname.equals("Compilers")) {
                                            // Can install all compilers
                                            installButton.setEnabled(true);
                                        } else {
                                            installButton.setEnabled(false);
                                        }
                                    } else {
                                        installButton.setEnabled(false);
                                    }
                                } else {
                                    installButton.setEnabled(false);
                                }
                            } else {
                                installButton.setEnabled(false);
                            }
                        } else {
                            installButton.setEnabled(false);
                        }
                        uninstallButton.setEnabled(false);
                    }
                    return container;
                }

                if (userObject instanceof Package) {
                    Package pe = (Package)userObject;
                    JLabel text = new JLabel(pe.getName());
                    String descString = pe.getDescription();
                    if (descString == null) {
                        descString = "Description Missing";
                    }
                    String[] descLines = descString.split("\n");
                    JLabel desc = new JLabel(descLines[0]);
                    String longDesc = "";
                    for (int i = 1; i < descLines.length; i++) {
                        longDesc += descLines[i] + "\n";
                    }
                    longDesc = longDesc.replace("\n", "<br/>");
                    container.setToolTipText("<html><body>" + longDesc + "</body></html>");
                    if (pe.getState() != 0) {
                        icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/downloading.png");
                    } else if (!apt.isInstalled(pe)) {
                        icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/available.png");
                    } else if (apt.isUpgradable(pe)) {
                        icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/upgrade.png");
                    } else {
                        icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/installed.png");
                    }
                    text.setBorder(paddingBorder);
//                    if (selected) {
//                        text.setBackground(bg);
//                        text.setOpaque(true);
//                        text.setForeground(fg);
//
//                        desc.setForeground(fg);
//                        desc.setForeground(fg);
//                        desc.setOpaque(true);
//
//                        container.setForeground(fg);
//                        container.setForeground(fg);
//                        container.setOpaque(true);
//                    } else {
                        container.setOpaque(false);
                        desc.setOpaque(false);
                        text.setOpaque(false);
                        Font f = text.getFont();
                        text.setFont(new Font(f.getFamily(), Font.BOLD, f.getSize()));
//                    }

                    container.setBorder(new EmptyBorder(5, 5, 5, 5));
                    JLabel i = new JLabel(icon);
                    container.add(i, BorderLayout.WEST);
                    container.add(text, BorderLayout.CENTER);
                    container.add(desc, BorderLayout.SOUTH);

                    if (selected) {
                        installButton.setEnabled(!apt.isInstalled(pe) || apt.isUpgradable(pe));
                        uninstallButton.setEnabled(apt.isInstalled(pe));
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

        DefaultMutableTreeNode pluginsNode = new DefaultMutableTreeNode("Plugins");
        treeRoot.add(pluginsNode);
        Package[] packages = apt.getPackages("plugins");
        for (Package p : packages) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
            node.setUserObject(p);
            pluginsNode.add(node);
        }

        DefaultMutableTreeNode libsNode = new DefaultMutableTreeNode("Libraries");
        treeRoot.add(libsNode);
        packages = apt.getPackages("libraries");
        for (Package p : packages) {
            if (
                (p.get("Family") == null) ||
                (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
            ) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                node.setUserObject(p);
                libsNode.add(node);
            }
        }

        DefaultMutableTreeNode boardsNode = new DefaultMutableTreeNode("Boards");
        treeRoot.add(boardsNode);
        packages = apt.getPackages("boards");
        for (Package p : packages) {
            if (
                (p.get("Family") == null) ||
                (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
            ) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                node.setUserObject(p);
                boardsNode.add(node);
            }
        }

        DefaultMutableTreeNode coresNode = new DefaultMutableTreeNode("Cores");
        treeRoot.add(coresNode);
        packages = apt.getPackages("cores");
        for (Package p : packages) {
            if (
                (p.get("Family") == null) ||
                (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
            ) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                node.setUserObject(p);
                coresNode.add(node);
            }
        }

        DefaultMutableTreeNode compilersNode = new DefaultMutableTreeNode("Compilers");
        treeRoot.add(compilersNode);
        packages = apt.getPackages("compilers");
        for (Package p : packages) {
            if (
                (p.get("Family") == null) ||
                (p.get("Family").equals(((keyval)(familySelector.getSelectedItem())).key)) ||
                (((keyval)(familySelector.getSelectedItem())).key.equals("all"))
            ) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(p.getName());
                node.setUserObject(p);
                compilersNode.add(node);
            }
        }

        treeModel.nodeStructureChanged(treeRoot);
        editor.restoreTreeState(tree, paths);
        frame.pack();
    }

    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("finished")) {
            QueueWorker[] finished = queue.getFinishedTasks();
            refreshTree();
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
        refreshTree();
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
        refreshTree();
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
        refreshTree();
    }

    public void refreshTree() {
        //Debug.message("Refreshing tree");
        //TreePath[] nodes = editor.getPaths(tree);
        //for (TreePath path : nodes) {
        //    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        //    treeModel.nodeStructureChanged(node);
       // }
       // Debug.message("Refresh done");
    }

    public void upgradeAll() {
        Package[] outOfDate = apt.getUpgradeList();
        for (Package p : outOfDate) {
            startDownload(p, false);
        }
    }

    public void launch() {
        openWindow();
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

}
