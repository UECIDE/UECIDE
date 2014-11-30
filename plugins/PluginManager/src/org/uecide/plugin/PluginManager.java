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

public class PluginManager extends Plugin implements PropertyChangeListener
{

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

    HashMap<String, PluginEntry> pluginObjects = new HashMap<String, PluginEntry>();
    HashMap<String, PluginEntry> coreObjects = new HashMap<String, PluginEntry>();
    HashMap<String, PluginEntry> boardObjects = new HashMap<String, PluginEntry>();
    HashMap<String, PluginEntry> compilerObjects = new HashMap<String, PluginEntry>();

    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    public PluginManager(Editor e) { editor = e; }
    public PluginManager(EditorBase e) { editorTab = e; }

    public void openWindow() {
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
        split.setDividerLocation(300);
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
                        if (node.getUserObject() instanceof PluginEntry) {
                            checkPrerequisites((PluginEntry)node.getUserObject());
                            startDownload((PluginEntry)node.getUserObject());
                        } else if (node.getUserObject() instanceof String) {

                            int kids = node.getChildCount();
                            for (int i = 0; i < kids; i++) {
                                DefaultMutableTreeNode kid = (DefaultMutableTreeNode)node.getChildAt(i);
                                if (kid.getUserObject() instanceof PluginEntry) {
                                    PluginEntry pe = (PluginEntry)kid.getUserObject();
                                    if (pe.canInstall() || pe.canUpgrade()) {
                                        checkPrerequisites(pe);
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
                        if (node.getUserObject() instanceof PluginEntry) {
                            uninstallDir((PluginEntry)node.getUserObject());
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

        upper.add(toolbar, BorderLayout.NORTH);
        upper.add(treeScroll, BorderLayout.CENTER);
        
        frame.setSize(new Dimension(400, 500));
        frame.setPreferredSize(new Dimension(400, 500));

        Dimension eSize = editor.getSize();
        Point ePos = editor.getLocation();
        frame.setLocation(new Point(
            ePos.x + (eSize.width / 2) - 200,
            ePos.y + (eSize.height / 2) - 250
        ));

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                askCloseWindow();
            }
        });
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        downloadPluginList();
        for (int i = 0; toolbar.getComponentAtIndex(i) != null; i++) {
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
        masterPluginList = new ArrayList<PluginEntry>();
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
            public String getTaskName() { return "Update List"; }

            @Override
            public String getActiveDescription() { return "Updating"; }

            @Override
            public String getQueuedDescription() { return "Update pending"; }
        
            @Override
            public Void doInBackground() {
                try {
                    URL url = new URL(Base.parseString(Base.theme.get("plugins.url"), null, null));
  
 //                       "?platform=" + Base.getOSName() + 
 //                       "&os=" + Base.getOSFlavour() +
 //                       "&release=" + Base.getOSVersion() +
 //                       "&arch=" + Base.getOSArch() + 
 //                       "&version=" + Base.systemVersion);
                    setProgress(0);
                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                    String data = in.readLine();
                    in.close();
                    setProgress(33);

                    familyNames = new HashMap<String, String>();

                    availablePlugins = new HashMap<String, JSONObject>();
                    availableCores = new HashMap<String, JSONObject>();
                    availableBoards = new HashMap<String, JSONObject>();
                    availableCompilers = new HashMap<String, JSONObject>();

                    pluginObjects = new HashMap<String, PluginEntry>();
                    coreObjects = new HashMap<String, PluginEntry>();
                    boardObjects = new HashMap<String, PluginEntry>();
                    compilerObjects = new HashMap<String, PluginEntry>();

                    JSONObject ob = (JSONObject)JSONValue.parse(data);

                    @SuppressWarnings("unchecked")
                    HashMap<String, String> fams = (HashMap<String, String>)ob.get("families");
                    familyNames.putAll(fams);

                    @SuppressWarnings("unchecked")
                    HashMap<String, JSONObject> plugins = (JSONObject)ob.get("plugins");
                    availablePlugins.putAll(plugins);

                    @SuppressWarnings("unchecked")
                    HashMap<String, JSONObject> cores = (JSONObject)ob.get("cores");
                    availableCores.putAll(cores);

                    @SuppressWarnings("unchecked")
                    HashMap<String, JSONObject> boards = (JSONObject)ob.get("boards");
                    availableBoards.putAll(boards);

                    @SuppressWarnings("unchecked")
                    HashMap<String, JSONObject> compilers = (JSONObject)ob.get("compilers");
                    availableCompilers.putAll(compilers);

                    setProgress(66);
                    updateTree();

                    setProgress(100);

                } catch (Exception e) {
                    Base.error(e);
                }

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

                if (userObject instanceof PluginEntry) {
                    PluginEntry pe = (PluginEntry)userObject;
                    JLabel text = new JLabel(pe.toString());
                    icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/available.png");
                    if (pe.isInstalled()) {
                        icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/installed.png");
                    }
                    if (pe.isNewer()) {
                        icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/newer.png");
                    }
                    if (pe.isOutdated()) {
                        icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/upgrade.png");
                    }
                    if (!pe.isIdle()) {
                        icon = Base.loadIconFromResource("/org/uecide/plugin/PluginManager/downloading.png");
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
                        installButton.setEnabled(pe.canInstall() || pe.canUpgrade());
                        uninstallButton.setEnabled(pe.canUninstall());
                    }

                    StringBuilder tt = new StringBuilder();

                    tt.append("<html><head><style>");
                    tt.append("html { background-color: white; color: black; font-size: 10px; font-weight: bold; }\n");
                    tt.append("body { background-color: white; color: black; font-size: 10px; font-weight: bold; }\n");
                    tt.append("span.header { font-size: 8px; font-weight: bold; }\n");
                    tt.append("span.version { font-size: 8px; font-weight: normal; }\n");
                    tt.append("</style></head><body>");
                    tt.append(pe.getDescription());
                    tt.append("<br/>");
                    if (!(pe.getInstalledVersion().equals(""))) {
                        tt.append("<span class='header'>Installed: </span><span class='version'>");
                        tt.append(pe.getInstalledVersion());
                        tt.append("</span> ");
                    }
                    tt.append("<span class='header'>Available: </span><span class='version'>");
                    tt.append(pe.getAvailableVersion());
                    tt.append("</span><br/>");
                    tt.append("</body></html>");

                    container.setToolTipText(tt.toString());

                    return container;
                }

            }
            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    };

    public void updateTree() {
        TreePath[] paths = editor.saveTreeState(tree);

        treeRoot.removeAllChildren();

        DefaultMutableTreeNode pluginsNode = new DefaultMutableTreeNode("Plugins");
        treeRoot.add(pluginsNode);

        for (String key : availablePlugins.keySet()) {
            PluginEntry e = getUnique(key, availablePlugins, PluginEntry.PLUGIN);
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(e.getName());
            node.setUserObject(e);
            pluginsNode.add(node);
        }

        String[] fkeys = familyNames.keySet().toArray(new String[0]);
        Arrays.sort(fkeys);

        for (String family : fkeys) {
            DefaultMutableTreeNode familyNode = new DefaultMutableTreeNode(familyNames.get(family));
            DefaultMutableTreeNode boardsNode = new DefaultMutableTreeNode("Boards");
            DefaultMutableTreeNode coresNode = new DefaultMutableTreeNode("Cores");
            DefaultMutableTreeNode compilersNode = new DefaultMutableTreeNode("Compilers");

            String[] groups = getGroups(availableBoards, family);
            ArrayList<PluginEntry>list;
            for (String group : groups) {
                list = new ArrayList<PluginEntry>();
                DefaultMutableTreeNode groupnode = new DefaultMutableTreeNode(group);
                for (String brd : availableBoards.keySet()) {
                    JSONObject ob = availableBoards.get(brd);
                    String f = (String)ob.get("Family");
                    String g = (String)ob.get("Group");
                    if (inSet(f, family) && g.equals(group)) {
                        PluginEntry pe = getUnique(brd, availableBoards, PluginEntry.BOARD);
                        list.add(pe);
                    }
                }
                PluginEntry[] pes = list.toArray(new PluginEntry[0]);
                Arrays.sort(pes);
                for (PluginEntry pe : pes) {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode("node");
                    node.setUserObject(pe);
                    groupnode.add(node);
                }
                boardsNode.add(groupnode);
            }

            list = new ArrayList<PluginEntry>();
            for (String core : availableCores.keySet()) {
                JSONObject ob = availableCores.get(core);
                String f = (String)ob.get("Family");
                if (inSet(f, family)) {
                    PluginEntry pe = getUnique(core, availableCores, PluginEntry.CORE);
                    list.add(pe);
                }
            }
            PluginEntry[] pes = list.toArray(new PluginEntry[0]);
            Arrays.sort(pes);
            for (PluginEntry pe : pes) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode("node");
                node.setUserObject(pe);
                coresNode.add(node);
            }

            list = new ArrayList<PluginEntry>();
            for (String compiler : availableCompilers.keySet()) {
                JSONObject ob = availableCompilers.get(compiler);
                String f = (String)ob.get("Family");
                if (inSet(f, family)) {
                    PluginEntry pe = getUnique(compiler, availableCompilers, PluginEntry.COMPILER);
                    list.add(pe);
                }
            }
            pes = list.toArray(new PluginEntry[0]);
            Arrays.sort(pes);
            for (PluginEntry pe : pes) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode("node");
                node.setUserObject(pe);
                compilersNode.add(node);
            }

            familyNode.add(boardsNode);
            familyNode.add(coresNode);
            familyNode.add(compilersNode);
            treeRoot.add(familyNode);
        }

        treeModel.nodeStructureChanged(treeRoot);
        editor.restoreTreeState(tree, paths);
        frame.pack();
    }

    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("finished")) {
            QueueWorker[] finished = queue.getFinishedTasks();
            for (QueueWorker worker : finished) {
                String command = worker.getTaskCommand();
                if (command != null) {
                    PluginEntry pe = (PluginEntry)worker.getUserObject();
                    if (command.equals("download")) {
                        if (pe.getType() == PluginEntry.PLUGIN) {
                            installPluginJar(pe);
                        } else {
                            Debug.message("Starting startUnpack of " + pe.toString());
                            startUnpack(pe);
                            Debug.message("    Done startUnpack of " + pe.toString());
                        }
                    } else if (command.equals("unpack")) {
                        Debug.message("Starting startInstall of " + pe.toString());
                        startInstall(pe);
                        Debug.message("    Done startInstall of " + pe.toString());
                    } else if (command.equals("install")) {
                        Debug.message("Starting finishInstall of " + pe.toString());
                        finishInstall(pe);
                        Debug.message("    Done finishInstall of " + pe.toString());
                    } else if (command.equals("uninstall")) {
                        Debug.message("Starting finishUninstall of " + pe.toString());
                        finishUninstall(pe); 
                        Debug.message("    Done finishUninstall of " + pe.toString());
                    }
                }
            }
            refreshTree();
        }
    }

    public void startDownload(final PluginEntry e) {
        if (!e.isIdle()) {
            return;
        }
        e.setState(PluginEntry.DOWNLOADING);
        QueueWorker downloader = new QueueWorker() {

            @Override
            public String getTaskName() {
                return e.getName();
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
                try {
                    URL url = e.getURL();
                    File dest = e.getDownloadFile();
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                    long contentLength = httpConn.getContentLength();
                    InputStream in = httpConn.getInputStream();
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
                    byte[] buffer = new byte[16384];
                    int n;
                    int t = 0;
                    while ((n = in.read(buffer)) > 0) {
                        if (terminateEverything) {
                            in.close();
                            out.close();
                            return null;
                        }
                        out.write(buffer, 0, n);
                        t += n;
                        long pl = (long)t * 100;
                        pl = pl / contentLength;
                        int per = (int)pl;
                        if (per > 100) per = 100;
                        if (per < 0) per = 0;
                        setProgress(per);
                    }
                    in.close();
                    out.close();
                } catch (Exception ex) {
                    Base.error(ex);
                }
                return null;
            }

            public void done() {
            }
        };

        downloader.setTaskCommand("download");
        downloader.setUserObject(e);

        queue.addTask(downloader);
        refreshTree();
    }

    public void startUnpack(PluginEntry e) {
        e.setState(PluginEntry.UNPACKING);
        QueueWorker unpacker = new QueueWorker() {

            @Override
            public String getTaskName() {
                PluginEntry pe = (PluginEntry)userObject;
                return pe.getName();
            }

            @Override
            public String getQueuedDescription() {
                return "Unpack pending";
            }

            @Override
            public String getActiveDescription() {
                return "Unpacking";
            }

            @Override
            public Void doInBackground() {
                try {
                    PluginEntry pe = (PluginEntry)userObject;
                    File zipFile = pe.getDownloadFile();
                    long files = 0;
                    ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
                    ZipEntry ze = zis.getNextEntry();
                    while (ze != null) {
                        files++;
                        ze = zis.getNextEntry();
                    }
                    zis.close();
                    File destFolder = pe.getUnpackFolder();

                    long fc = 0;

                    zis = new ZipInputStream(new FileInputStream(zipFile));
                    ze = zis.getNextEntry();
                    while (ze != null) {
                        if (terminateEverything) {
                            return null;
                        }
                        File outputFile = new File(destFolder, ze.getName());
                        if (outputFile != null) {
                            File p = outputFile.getParentFile();
                            if (!p.exists()) {
                                p.mkdirs();
                            }
                            if (ze.isDirectory()) {
                                outputFile.mkdirs();
                            } else {
                                byte[] buffer = new byte[16384];
                                FileOutputStream fos = new FileOutputStream(outputFile);
                                int len;
                                while ((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                                fos.close();
                                outputFile.setExecutable(true, false);
                            }
                        }
                        fc++;
                        long per = (fc * 100) / files;
                        setProgress((int)per);
                        ze = zis.getNextEntry();
                    }
                    zis.closeEntry();
                    zis.close();

                } catch (Exception ex) {
                    Base.error(ex);
                }
                return null;
            }

            public void done() {
            }
        };
        unpacker.setTaskCommand("unpack");
        unpacker.setUserObject(e);
        queue.addTask(unpacker);
        refreshTree();
    }

    public void startInstall(PluginEntry e) {
        e.setState(PluginEntry.INSTALLING);
        QueueWorker installer = new QueueWorker() {

            @Override
            public String getTaskName() {
                PluginEntry pe = (PluginEntry)userObject;
                return pe.getName();
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
                try {
                    PluginEntry pe = (PluginEntry)userObject;
                    File installFolder = pe.getInstallFolder();

                    if (installFolder != null) {
                        Base.removeDir(installFolder);
                    }

                    LinkedList<File> filelist = new LinkedList<File>();
                    ArrayList<File> fulllist = new ArrayList<File>();
                    addFilesToList(pe.getUnpackFolder(), filelist);
                    long files = 0;
                    while (filelist.size() > 0) {
                        File thisFile = filelist.removeFirst();
                        fulllist.add(thisFile);
                        files++;
                        if (thisFile.isDirectory()) {
                            addFilesToList(thisFile, filelist);
                        }
                    }

                    File destDir = null;
                    if (pe.getType() == PluginEntry.BOARD) {
                        destDir = Base.getUserBoardsFolder();
                    }
                    if (pe.getType() == PluginEntry.CORE) {
                        destDir = Base.getUserCoresFolder();
                    }
                    if (pe.getType() == PluginEntry.COMPILER) {
                        destDir = Base.getUserCompilersFolder();
                    }
                    long fc = 0;
                    for (File f : fulllist) {
                        if (terminateEverything) {
                            return null;
                        }
                        String pth = f.getAbsolutePath();
                        String pfx = pe.getUnpackFolder().getAbsolutePath() + "/";
                        String df = pth.substring(pfx.length());
                        File out = new File(destDir, df);
                        if (f.isDirectory()) {
                            out.mkdirs();
                        } else {
                            File par = out.getParentFile();
                            if (!par.exists()) {
                                par.mkdirs();
                            }
                            Base.copyFile(f, out);
                            out.setExecutable(true, false);
                        }
                        
                        fc++;
                        long pct = (fc * 100) / files;
                        setProgress((int)pct);
                    }

                    
                } catch (Exception ex) {
                    Base.error(ex);
                }
                return null;
            }

            public void addFilesToList(File dir, LinkedList<File> list) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    list.add(file);
                }
            }

            public void done() {
            }
        };
        installer.setTaskCommand("install");
        installer.setUserObject(e);
        queue.addTask(installer);
        refreshTree();
        
    }

    public void finishInstall(PluginEntry e) {
        e.setState(PluginEntry.IDLE);
        switch (e.getType()) {
            case PluginEntry.PLUGIN:
                Base.rescanPlugins();
                break;

            case PluginEntry.BOARD:
                Base.rescanBoards();
                break;

            case PluginEntry.CORE:
                Base.rescanCores();
                break;
        
            case PluginEntry.COMPILER:
                Base.rescanCompilers();
                break;
        }
        Debug.message("Did it finish?");
    }

    public void finishUninstall(PluginEntry e) {
        e.setState(PluginEntry.IDLE);
        switch (e.getType()) {
            case PluginEntry.PLUGIN:
                Base.rescanPlugins();
                break;

            case PluginEntry.BOARD:
                Base.rescanBoards();
                break;

            case PluginEntry.CORE:
                Base.rescanCores();
                break;
        
            case PluginEntry.COMPILER:
                Base.rescanCompilers();
                break;
        }
    }

    public void installPluginJar(PluginEntry e) {
        File dest = e.getInstallFolder();
        if (dest != null) {
            Base.copyFile(e.getDownloadFile(), dest);
        }
        finishInstall(e);
    }

    public void uninstallDir(PluginEntry e) {
        if (!e.isIdle()) {
            System.err.println("Uninstall failed: plugin busy");
            return;
        }
        e.setState(PluginEntry.UNINSTALLING);
        if (e.getType() == PluginEntry.PLUGIN) {
            File f = e.getInstallFolder();
            f.delete();
            finishUninstall(e);
            return;
        }
        QueueWorker uninstaller = new QueueWorker() {

            @Override
            public String getTaskName() {
                PluginEntry pe = (PluginEntry)userObject;
                return pe.getName();
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
                try {
                    PluginEntry pe = (PluginEntry)userObject;
                    File dir = pe.getInstallFolder();
                    if (dir != null) {
                        setProgress(50);
                        Base.removeDir(dir);
/*
                        LinkedList<File> filelist = new LinkedList<File>();
                        ArrayList<File> fulllist = new ArrayList<File>();
                        addFilesToList(dir, filelist);
                        long files = 0;
                        while (filelist.size() > 0) {
                            File thisFile = filelist.removeFirst();
                            if (thisFile.isDirectory()) {
                                addFilesToList(thisFile, filelist);
                            }
                            fulllist.add(thisFile);
                            files++;
                        }

                        for (File f : fulllist) {
                            Debug.message("  Want to delete " + f.getAbsolutePath());
                        }

                        long fc = 0;
                        for (File thisFile : fulllist) {
                            if (thisFile.isDirectory() == false) {
                                Debug.message("    Deleting file " + thisFile.getAbsolutePath());
                                if (!thisFile.delete()) {
                                    Debug.message("    Deleting failed!");
                                }
                                fc++;
                            }
                            long pct = (fc * 100) / files;
                            setProgress((int)pct);
                        }
                        for (File thisFile : fulllist) {
                            if (thisFile.isDirectory() == true) {
                                Debug.message("    Deleting folder " + thisFile.getAbsolutePath());
                                if (!thisFile.delete()) {
                                    Debug.message("    Deleting failed!");
                                }
                                fc++;
                            }
                            long pct = (fc * 100) / files;
                            setProgress((int)pct);
                        }
                        dir.delete();
*/
                        setProgress(100);
                    }
                } catch (Exception ex) {
                    Base.error(ex);
                }
                return null;
            }

            public void addFilesToList(File dir, LinkedList<File> list) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    list.add(file);
                }
            }
        
            public void done() {
            }
        };
        uninstaller.setTaskCommand("uninstall");
        uninstaller.setUserObject(e);
        queue.addTask(uninstaller);
        refreshTree();
    }

    public void refreshTree() {
        Debug.message("Refreshing tree");
        TreePath[] nodes = editor.getPaths(tree);
        for (TreePath path : nodes) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            Object uo = node.getUserObject();
            if (uo instanceof PluginEntry) {
                PluginEntry pe = (PluginEntry)uo;
                Debug.message("Refreshing " + pe.toString());
                pe.initData();
                treeModel.nodeStructureChanged(node);
            }
        }
        Debug.message("Refresh done");
    }

    public ArrayList<PluginEntry> masterPluginList = new ArrayList<PluginEntry>();

    public PluginEntry getUnique(String key, HashMap<String, JSONObject>map, int type) {
        JSONObject ob = map.get(key);
        if (ob == null) {
            return null;
        }
        PluginEntry peTest = new PluginEntry(ob, type);
        for (PluginEntry e : masterPluginList) {
            if (e.equals(peTest)) {
                return e;
            }
        }
        masterPluginList.add(peTest);
        return peTest;
    }

    public void upgradeAll() {
        TreePath[] nodes = editor.getPaths(tree);
        for (TreePath path : nodes) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            Object uo = node.getUserObject();
            if (uo instanceof PluginEntry) {
                PluginEntry pe = (PluginEntry)uo;
                if (pe.isIdle()) {
                    if (pe.canUpgrade()) {
                        startDownload(pe);
                    }
                }
            }
        }
    }

    public void checkPrerequisites(PluginEntry pe) {
        if (pe.getType() == PluginEntry.PLUGIN) {
            return;
        }
        if (pe.getType() == PluginEntry.COMPILER) {
            return;
        }
        if (pe.getType() == PluginEntry.BOARD) {
            // Find recommended core
            String recCore = pe.get("Core");
            PluginEntry core = getUnique(recCore + ".jar", availableCores, PluginEntry.CORE);
            if (core == null) {
                JOptionPane.showMessageDialog(frame,
                    "The core for this board (" + recCore + ") is not available.\nI will install the board anyway\nbut it may not work until the core is installed.",
                    "No core available",
                    JOptionPane.ERROR_MESSAGE
                );
                
                return;
            }
    
            if (core.canInstall()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(
                    frame, 
                    "The board " + pe.getDescription() + " recommends the " + core.getDescription() + " core.\nDo you want to install this core?",
                    "Install core?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
                if (n == 0) {
                    checkPrerequisites(core);
                    startDownload(core);
                }
            }
        }
        if (pe.getType() == PluginEntry.CORE) {
            // Find recommended compiler
            String recComp = pe.get("Compiler");
            PluginEntry compiler = null;
            for (String k : availableCompilers.keySet()) {
                PluginEntry te = getUnique(k, availableCompilers, PluginEntry.COMPILER);
                if (te.getName().equals(recComp)) {
                    compiler = te;
                    break;
                }
            }
            if (compiler == null) {
                JOptionPane.showMessageDialog(frame,
                    "The compiler for this core is not available.\nI will install the compiler anyway\nbut it may not work until the compiler is installed.",
                    "No core available",
                    JOptionPane.ERROR_MESSAGE);
                
                return;
            }
            if (compiler.canInstall()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(
                    frame, 
                    "The core " + pe.getDescription() + " recommends the " + compiler.getDescription() + " compiler.\nDo you want to install this compiler?",
                    "Install compiler?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
                if (n == 0) {
                    checkPrerequisites(compiler);
                    startDownload(compiler);
                }
            }
        }
    }

    public void launch() {
        openWindow();
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

}
