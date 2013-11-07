package uecide.plugin;

import uecide.app.*;
import uecide.app.debug.*;
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

public class PluginManager extends BasePlugin
{
    JFrame win;
    JButton refreshButton;
    JScrollPane scroll;
    JPanel body;
    JButton upgradeAllButton;

    public static HashMap<String, JSONObject> availablePlugins = new HashMap<String, JSONObject>();
    public static HashMap<String, JSONObject> availableCores = new HashMap<String, JSONObject>();
    public static HashMap<String, JSONObject> availableBoards = new HashMap<String, JSONObject>();
    public static HashMap<String, JSONObject> availableCompilers = new HashMap<String, JSONObject>();

    public HashMap<String, PluginEntry> pluginObjects = new HashMap<String, PluginEntry>();
    public HashMap<String, PluginEntry> coreObjects = new HashMap<String, PluginEntry>();
    public HashMap<String, PluginEntry> boardObjects = new HashMap<String, PluginEntry>();
    public HashMap<String, PluginEntry> compilerObjects = new HashMap<String, PluginEntry>();

    public class PluginInfo {
        public String installed;
        public String available;
        public String url;
    }

    public void init(Editor editor)
    {
        this.editor = editor;
    }

    public void run()
    {
        win = new JFrame(Translate.t("Plugin Manager"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(false);

        Box box = Box.createVerticalBox();
        body = new JPanel(new BorderLayout());

        box.add(body);

        populate();

        Box line = Box.createHorizontalBox();
        upgradeAllButton = new JButton(Translate.t("Upgrade All"));
        upgradeAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                upgradeAll();
            }
        });
        line.add(upgradeAllButton);

        refreshButton = new JButton(Translate.t("Refresh"));
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updatePlugins();
            }
        });
        line.add(refreshButton);


        box.add(line);
        

        win.getContentPane().add(box);
        win.pack();

        Dimension size = new Dimension(500, 400); //win.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        win.setSize(size);
        win.setMinimumSize(size);
        win.setMaximumSize(size);
        win.setPreferredSize(size);
        win.setLocation((screen.width - size.width) / 2,
                          (screen.height - size.height) / 2);

        win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        Base.setIcon(win);

        win.setVisible(true);

        if (PluginManager.availablePlugins.size() == 0) {
            updatePlugins();
        }
            
    }

    public void populate() {
        body.removeAll();

        DefaultMutableTreeNode top = new DefaultMutableTreeNode(Translate.t("Updating..."));
        final JTree root = new JTree(top);
        root.setVisibleRowCount(15);
        scroll = new JScrollPane(root);
        root.setShowsRootHandles(true);
        root.setToggleClickCount(1);

        final JPanel infoPanel = new JPanel(new BorderLayout());

        JLabel test = new JLabel("");
        infoPanel.add(test);

        body.add(scroll, BorderLayout.NORTH);
        body.add(infoPanel, BorderLayout.SOUTH);

        // ---- Plugins ---- //

        DefaultMutableTreeNode pluginRoot = new DefaultMutableTreeNode(Translate.t("Plugins"));
        top.add(pluginRoot);

        for (String entry : PluginManager.availablePlugins.keySet().toArray(new String[0])) {
            JSONObject plugin = PluginManager.availablePlugins.get(entry);
            PluginEntry pe = new PluginEntry(plugin, 1);
            DefaultMutableTreeNode brdNode = new DefaultMutableTreeNode(pe);
            brdNode.setUserObject(pe);
            pluginRoot.add(brdNode);
            pluginObjects.put(entry, pe);
        }

        // ---- Boards ---- //

        DefaultMutableTreeNode boardRoot = new DefaultMutableTreeNode(Translate.t("Boards"));
        top.add(boardRoot);
        String[] entries = PluginManager.availableBoards.keySet().toArray(new String[0]);

        ArrayList<String> families = new ArrayList<String>();
        for (String entry : entries) {
            JSONObject plugin = PluginManager.availableBoards.get(entry);
            String family = (String)plugin.get("Family");
            if (families.indexOf(family) == -1) {
                families.add(family);
            }
        }

        Collections.sort(families);
        
        for (String family : families) {
            DefaultMutableTreeNode famNode = new DefaultMutableTreeNode(family);
            boardRoot.add(famNode);
            ArrayList<String> groups = new ArrayList<String>();
            for (String entry : entries) {
                JSONObject plugin = PluginManager.availableBoards.get(entry);
                String eFam = (String)plugin.get("Family");
                String eGrp = (String)plugin.get("Group");
                if (family.equals(eFam)) {
                    if (groups.indexOf(eGrp) == -1) {
                        groups.add(eGrp);
                    }
                }
            }

            Collections.sort(groups);     
            for (String group : groups) {
                DefaultMutableTreeNode grpNode = new DefaultMutableTreeNode(group);
                famNode.add(grpNode);

                ArrayList<String> validBoards = new ArrayList<String>();
                for (String entry : entries) {
                    JSONObject plugin = PluginManager.availableBoards.get(entry);
                    String eFam = (String)plugin.get("Family");
                    String eGrp = (String)plugin.get("Group");
                    if (eFam.equals(family) && eGrp.equals(group)) {
                        if (validBoards.indexOf(entry) == -1) {
                            validBoards.add(entry);
                        }
                    }
                }

                Collections.sort(validBoards, new Comparator() {
                    public int compare(Object a, Object b) {
                        String s1 = (String)a;
                        String s2 = (String)b;
                        JSONObject p1 = PluginManager.availableBoards.get(s1);
                        JSONObject p2 = PluginManager.availableBoards.get(s2);
                        String t1 = ((String)p1.get("Description")).toLowerCase();
                        String t2 = ((String)p2.get("Description")).toLowerCase();
                        return t1.compareTo(t2);
                    }
                });
                for (String board : validBoards) {
                    JSONObject plugin = PluginManager.availableBoards.get(board);
                    PluginEntry pe = new PluginEntry(plugin, 3);
                    DefaultMutableTreeNode brdNode = new DefaultMutableTreeNode(pe);
                    brdNode.setUserObject(pe);
                    grpNode.add(brdNode);
                    boardObjects.put(board, pe);
                }
            }
        }

        // ---- Cores ---- //

        DefaultMutableTreeNode coreRoot = new DefaultMutableTreeNode(Translate.t("Cores"));
        top.add(coreRoot);
        entries = PluginManager.availableCores.keySet().toArray(new String[0]);

        families = new ArrayList<String>();
        for (String entry : entries) {
            JSONObject plugin = PluginManager.availableCores.get(entry);
            String family = (String)plugin.get("Family");
            if (families.indexOf(family) == -1) {
                families.add(family);
            }
        }
        
        for (String family : families) {
            DefaultMutableTreeNode famNode = new DefaultMutableTreeNode(family);
            coreRoot.add(famNode);
            for (String entry : entries) {
                JSONObject plugin = PluginManager.availableCores.get(entry);
                String eFam = (String)plugin.get("Family");
                if (family.equals(eFam)) {
                    JSONObject plugin1 = PluginManager.availableCores.get(entry);
                    PluginEntry pe = new PluginEntry(plugin1, 2);
                    DefaultMutableTreeNode brdNode = new DefaultMutableTreeNode(pe);
                    brdNode.setUserObject(pe);
                    famNode.add(brdNode);
                    coreObjects.put(entry, pe);
                }
            }
        }

        // ---- Compilers ---- //

        DefaultMutableTreeNode compilerRoot = new DefaultMutableTreeNode(Translate.t("Compilers"));
        top.add(compilerRoot);

        for (String entry : PluginManager.availableCompilers.keySet().toArray(new String[0])) {
            JSONObject plugin = PluginManager.availableCompilers.get(entry);
            PluginEntry pe = new PluginEntry(plugin, 4);
            DefaultMutableTreeNode brdNode = new DefaultMutableTreeNode(pe);
            brdNode.setUserObject(pe);
            compilerRoot.add(brdNode);
            compilerObjects.put(entry, pe);
        }

        // Set up the tree 

        PluginNodeRenderer renderer = new PluginNodeRenderer();
        root.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        root.setCellRenderer(renderer);
        root.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)root.getLastSelectedPathComponent();
                if (node == null) {
                    return;
                }
                Object uo = node.getUserObject();

                if (uo instanceof PluginEntry) {
                    PluginEntry pe = (PluginEntry)node.getUserObject();

                    if (pe != null) {
                        infoPanel.removeAll();
                        win.repaint();
                        win.pack();
                        JLabel l = new JLabel(pe.getDescription());
                        infoPanel.add(l, BorderLayout.NORTH);
                        if (pe.isOutdated() || pe.isNewer()) {
                            l = new JLabel("Installed: " + pe.getInstalledVersion() + " Available: " + pe.getAvailableVersion());
                        } else if (pe.isInstalled()) {
                            l = new JLabel("Installed: " + pe.getInstalledVersion());
                        } else {
                            l = new JLabel("Available: " + pe.getAvailableVersion());
                        }
                        infoPanel.add(l, BorderLayout.SOUTH);
                        infoPanel.add(pe, BorderLayout.CENTER);
                        win.repaint();
                        win.pack();
                    }
                }
            }
        });

        root.expandRow(0);
        root.setRootVisible(false);

        win.repaint();
        win.pack();
    }

    public class PluginNodeRenderer extends DefaultTreeCellRenderer {
        DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
        JLabel name = new JLabel("");
        Icon installed;
        Icon available;
        Icon downloading;
        Icon queued;
        Icon upgrade;
        Icon newer;

        public PluginNodeRenderer() {
            installed = new ImageIcon(getResourceURL("uecide/plugin/PluginManager/installed.png"));
            available = new ImageIcon(getResourceURL("uecide/plugin/PluginManager/available.png"));
            downloading = new ImageIcon(getResourceURL("uecide/plugin/PluginManager/downloading.png"));
            queued = new ImageIcon(getResourceURL("uecide/plugin/PluginManager/queued.png"));
            upgrade = new ImageIcon(getResourceURL("uecide/plugin/PluginManager/upgrade.png"));
            newer = new ImageIcon(getResourceURL("uecide/plugin/PluginManager/newer.png"));
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object o = null;
            if (value instanceof DefaultMutableTreeNode) {
                o = ((DefaultMutableTreeNode)value).getUserObject();
            }

            if (o == null) {
                return nonLeafRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            }

            if (o instanceof PluginEntry) {
                PluginEntry pe = (PluginEntry)o;
                String text = pe.getDisplayName();
                name.setText(text);
                if (pe.isDownloading()) {
                    name.setIcon(downloading);
                } else if (pe.isQueued()) {
                    name.setIcon(queued);
                } else if (pe.isNewer()) {
                    name.setIcon(newer);
                } else if (pe.isOutdated()) {
                    name.setIcon(upgrade);
                } else if (pe.isInstalled()) {
                    name.setIcon(installed);
                } else {
                    name.setIcon(available);
                }

                if (Base.isWindows()) {
                    if (selected) {
                        name.setOpaque(true);
                        name.setBackground(new Color(100,100,200));
                        name.setForeground(new Color(255,255,255));
                    } else {
                        name.setOpaque(true);
                        name.setBackground(new Color(255,255,255));
                        name.setForeground(new Color(0,0,0)); 
                    }
                } else {
                    name.setOpaque(false);
                }
                
                return name;
            } else if (o instanceof String) {
                name.setText((String)o);
                name.setOpaque(false);
                name.setForeground(new Color(0,0,0)); 
                name.setIcon(null);
                return name;
            }
            return nonLeafRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    }

    public boolean isDownloading() {
        boolean isDownloading = false;

        for (String e : pluginObjects.keySet().toArray(new String[0])) {
            if (pluginObjects.get(e).isDownloading()) {
                isDownloading = true;
            }
        }

        for (String e : boardObjects.keySet().toArray(new String[0])) {
            if (boardObjects.get(e).isDownloading()) {
                isDownloading = true;
            }
        }

        for (String e : coreObjects.keySet().toArray(new String[0])) {
            if (coreObjects.get(e).isDownloading()) {
                isDownloading = true;
            }
        }

        for (String e : compilerObjects.keySet().toArray(new String[0])) {
            if (compilerObjects.get(e).isDownloading()) {
                isDownloading = true;
            }
        }

        return isDownloading;

    }

    public void cancelDownloads() {

        for (String e : pluginObjects.keySet().toArray(new String[0])) {
            pluginObjects.get(e).cancelAll();
        }

        for (String e : boardObjects.keySet().toArray(new String[0])) {
            boardObjects.get(e).cancelAll();
        }

        for (String e : coreObjects.keySet().toArray(new String[0])) {
            coreObjects.get(e).cancelAll();
        }

        for (String e : compilerObjects.keySet().toArray(new String[0])) {
            compilerObjects.get(e).cancelAll();
        }
    }

    public void close()
    {
        cancelDownloads();
        if (isDownloading()) {
            Base.showWarning("Downloading", "You have active downloads.\nYou may not close", null);
            return;
        }
        win.dispose();
        
        int p = Base.pluginInstances.indexOf(this);
        if (p>=0) {
            Base.pluginInstances.remove(p);
        }

    }

    public String getMenuTitle()
    {
        return(Translate.t("Plugin Manager"));
    }

    public void message(String m) {
    }
    
    public void message(String m, int c) {
        message(m);
    }

    public ImageIcon toolbarIcon() {
        return null;
    }

    public void updatePlugins() {
        body.removeAll();
        SwingWorker sw = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                String data = null;
                try {
                    URL page = new URL(Base.theme.get("plugins.url") + "?platform=" + Base.getOSName() + "&arch=" + Base.getOSArch());
                    BufferedReader in = new BufferedReader(new InputStreamReader(page.openStream()));
                    data = in.readLine();
                    in.close();
                } catch (UnknownHostException e) {
                    Base.showWarning(Translate.t("Update Failed"), Translate.w("The update failed because I could not find the host %1", 40, "\n", e.getMessage()), e);
                    return null;
                } catch (Exception e) {
                    Base.showWarning(Translate.t("Update Failed"), Translate.w("An unknown error occurred: %1", 40, "\n", e.toString()), e);
                    return null;
                }

                JSONObject ob = (JSONObject)JSONValue.parse(data);
                try {
                    JSONObject plugins = (JSONObject)ob.get("plugins");
                    PluginManager.availablePlugins.putAll(plugins);
                } catch (Exception ignored) {}
                try {
                    JSONObject cores = (JSONObject)ob.get("cores");
                    PluginManager.availableCores.putAll(cores);
                } catch (Exception ignored) {}
                try {
                    JSONObject boards = (JSONObject)ob.get("boards");
                    PluginManager.availableBoards.putAll(boards);
                } catch (Exception ignored) {}
                try {
                    JSONObject compilers = (JSONObject)ob.get("compilers");
                    PluginManager.availableCompilers.putAll(compilers);
                } catch (Exception ignored) {}
                populate();
                return null;
            }
        };
        sw.execute();
    }

    public File getJarFileToTmp(String name, String url) {
        try {
            File dest = new File(Base.getTmpDir(), name + ".jar");
            URL page = new URL(url);
            InputStream in = page.openStream();
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            in.close();
            out.close();
            return dest;
        } catch (Exception e) {
            return null;
        }
    }

    public int flags() {
        return BasePlugin.MENU_PLUGIN_TOP;
    }

    public void upgradeAll() {
        for (PluginEntry pe : pluginObjects.values()) {
            if (pe.isOutdated()) {
                pe.startDownload();
            }
        }

        for (PluginEntry pe : boardObjects.values()) {
            if (pe.isOutdated()) {
                pe.startDownload();
            }
        }

        for (PluginEntry pe : coreObjects.values()) {
            if (pe.isOutdated()) {
                pe.startDownload();
            }
        }

        for (PluginEntry pe : compilerObjects.values()) {
            if (pe.isOutdated()) {
                pe.startDownload();
            }
        }

    }


    public class PluginEntry extends JPanel implements ActionListener {
        String name;
        String installedVersion;
        String availableVersion;
        String url;
        JButton button;
        JLabel label;
        JProgressBar bar;
        int type;
        File dest;
        String mainClass;
        JSONObject data;
        boolean isDownloading = false;
        boolean isQueued = false;

        SwingWorker<Void, Long> downloader = null;
        ZipExtractor installer = null;

        public int PLUGIN = 1;
        public int CORE = 2;
        public int BOARD = 3;
        public int COMPILER = 4;

        PluginEntry installNext = null;

        public PluginEntry(JSONObject o, int type) {
            data = o;
            this.type = type;
            url = (String)o.get("url");
            availableVersion = (String)o.get("Version");
            if (availableVersion == null) {
                availableVersion = "unknown";
            }

            installedVersion = "";
            if (type == PLUGIN) {
                mainClass = (String)o.get("Main-Class");
                name = mainClass.substring(mainClass.lastIndexOf(".")+1);
                Plugin p = Base.plugins.get(mainClass);
                if (p != null) {
                    installedVersion = p.getVersion();
                }
            } 

            if (type == CORE) {
                name = (String)o.get("Core");
                Core c = Base.cores.get(name);
                if (c != null) {    
                    installedVersion = c.getFullVersion();
                }
            }

            if (type == BOARD) {
                name = (String)o.get("Board");
                Board c = Base.boards.get(name);
                if (c != null) {    
                    installedVersion = c.getVersion();
                }
            }

            if (type == COMPILER) {
                name = (String)o.get("Compiler");
                uecide.app.debug.Compiler c = Base.compilers.get(name);
                if (c != null) {    
                    installedVersion = c.getVersion();
                }
            }

            updateDisplay();
        }
    
        public void updateDisplay() {

            this.removeAll();
            repaint();
            win.repaint();
            win.pack();

            if (!isOutdated() && !isInstalled() && !isNewer()) {
                button = new JButton("Install");
                button.addActionListener(this);
                button.setActionCommand("install");
                this.add(button);
            } else if (isInstalled()) {
                label = new JLabel("Installed");
                this.add(label);
            } else if (isNewer()) {
                label = new JLabel("Test Version");
                this.add(label);
            } else if (isOutdated()) {
                button = new JButton("Upgrade");
                button.addActionListener(this);
                button.setActionCommand("upgrade");
                this.add(button);
            }

            if (isOutdated() || isInstalled() || isNewer()) {
                button = new JButton("Uninstall");
                button.addActionListener(this);
                button.setActionCommand("uninstall");
                this.add(button);
            }
            repaint();
            win.repaint();
            win.pack();
        }
        
        public boolean isNewer() {
            if (installedVersion == "") {
                return false;
            }
            if (installedVersion.compareTo(availableVersion) > 0) {
                return true;
            }
            return false;
        }

        public boolean isOutdated() {
            if (installedVersion == "") {
                return false;
            }
            if (installedVersion.compareTo(availableVersion) < 0) {
                return true;
            }
            return false;
        }

        public boolean isInstalled() {
            if (installedVersion.equals(availableVersion)) {
                return true;
            }
            return false;
        }

        public String get(String k) {
            return (String)data.get(k);
        }

        public String getAvailableVersion() {
            return availableVersion;
        }

        public String getInstalledVersion() {
            return installedVersion;
        }

        public String getDisplayName() {
            switch (type) {
                case 1:
                    return name;
                case 2:
                    return name;
                case 3:
                    return get("Description");
                case 4:
                    return name;
            }
            return "---";
        }
 
        public String getDescription() {
            String d = get("Description");
            if (d == null) {
                return getDisplayName();
            }
            return d;
        }

        public void cancelAll() {   
            if (downloader != null) {
                downloader.cancel(true);
            }
            if (installer != null) {
                installer.cancel(true);
            }
            isDownloading = false;
        }

        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            if (command.equals("install") || command.equals("upgrade")) {
                startDownload();
            }
            if (command.equals("uninstall")) {
                uninstall();
            }
        }

        public void uninstall() {
            if (type == PLUGIN) {
                if (mainClass.equals(PluginManager.this.getClass().getName())) {
                    Base.showWarning(Translate.t("Unable To Uninstall"), Translate.w("If you uninstall the Plugin Manager you won't be able to install any new plugins. That would be a bit silly, don't you think? I'm not going to let you do it.", 40, "\n"), null);
                    return;
                }
                Plugin p = Base.plugins.get(mainClass);
                if (p != null) {
                    File jf = p.getJarFile();
                    if (jf.exists()) {
                        jf.delete();
                        installedVersion = "";
                    }
                }
            }

            if (type == BOARD) {
                Board b = Base.boards.get(name);
                if (b != null) {
                    File bf = b.getFolder();
                    if (bf.exists() && bf.isDirectory()) {
                        Base.removeDir(bf);
                        installedVersion = "";
                    }
                }
            }

            if (type == CORE) {
                Core c = Base.cores.get(name);
                if (c != null) {
                    File cf = c.getFolder();
                    if (cf.exists() && cf.isDirectory()) {
                        Base.removeDir(cf);
                        installedVersion = "";
                    }
                }
            }

            if (type == COMPILER) {
                uecide.app.debug.Compiler c = Base.compilers.get(name);
                if (c != null) {
                    File cf = c.getFolder();
                    if (cf.exists() && cf.isDirectory()) {
                        Base.removeDir(cf);
                        installedVersion = "";
                    }
                }
            }

            Base.loadCompilers();
            Base.loadCores();
            Base.loadBoards();
            Base.gatherLibraries();
            for (Editor e : Base.editors) {
                e.rebuildCoresMenu();
                e.rebuildBoardsMenu();
                e.rebuildImportMenu();
                e.rebuildExamplesMenu();
                e.rebuildPluginsMenu();
            }

            updateDisplay();

        }

        public void startDownload(PluginEntry pe) {
            installNext = pe;
            startDownload();
        }

        public void startDownload() {
            this.removeAll();
            repaint();
            win.repaint();
            win.pack();
            bar = new JProgressBar(0, 100);

            System.err.println("Downloading " + name);

            if (type == CORE) {
                if (Base.compilers.get((String)data.get("Compiler")) == null) {
                    PluginEntry pe = compilerObjects.get((String)data.get("Compiler"));
                    if (pe != null) {
                        isQueued = true;
                        pe.startDownload(this);
                        bar.setIndeterminate(false);
                        bar.setString("Installing Compiler...");
                        bar.setStringPainted(true);
                        this.add(bar);
                        repaint();
                        win.repaint();
                        win.pack();
                        return;
                    } else {
                        Base.showWarning(Translate.t("Unable to install"), Translate.w("That core cannot be installed right now. You do not have the compiler installed, and I cannot find the compiler in my list of packages. Try refreshing the list and trying again.", 40, "\n"), null);
                        return;
                    }
                }
            }
            bar.setString("Downloading");
            bar.setStringPainted(true);
            bar.setIndeterminate(false);
            this.add(bar);
            repaint();
            win.repaint();
            win.pack();

            download();
        }

        public void setProgress(long p) {
            if (bar != null) {
                if (p == -1) {
                    bar.setIndeterminate(true);
                } else {
                    bar.setIndeterminate(false);
                    bar.setValue((int)p);
                }
            }
            if (installNext != null) {
                installNext.setProgress(p);
            }
        }

        public void setMax(long m) {
            if (bar != null) {
                bar.setMaximum((int)m);
            }
            if (installNext != null) {
                installNext.setMax(m);
            }
        }

        public void download() {

            isQueued = false;
            isDownloading = true;

            try {
                
                dest = new File(Base.getTmpDir(), name + ".jar");
                URL page = new URL(url);
                HttpURLConnection httpConn = (HttpURLConnection) page.openConnection();
                final long contentLength = httpConn.getContentLength();
                if (contentLength == -1) {
                    System.out.println("unknown content length");
                } else {
                    System.out.println("content length: " + contentLength + " bytes");              
                }
                final InputStream in = httpConn.getInputStream();
                final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));

                setMax(contentLength);

                downloader = new SwingWorker<Void, Long>(){
                    @Override
                    public Void doInBackground() {
                        try {
                            byte[] buffer = new byte[1024];
                            int n;
                            long tot = 0;
                            while ((n = in.read(buffer)) > 0) {
                                tot += n;
                                publish(tot);
                                out.write(buffer, 0, n);
                            }
                        } catch (Exception ex) {
                            Base.showWarning(Translate.t("Download Failed"), Translate.w("The download failed at point 1 because %1", 40, "\n", ex.toString()), ex);
                            isDownloading = false;
                        }
                        return null;
                    }

                    @Override
                    public void done() {
                        try {
                            in.close();
                            out.close();
                        } catch (Exception ex) {
                            Base.showWarning(Translate.t("Download Failed"), Translate.w("The download failed at point 2 because %1", 40, "\n", ex.toString()), ex);
                            isDownloading = false;
                            return;
                        }
                        PluginEntry.this.install();
                    }

                    @Override
                    protected void process(java.util.List<Long> chunk) {
                        for (long num : chunk) {
                            PluginEntry.this.setProgress(num);
                        }
                    }
                
                };
                downloader.execute();
            } catch (Exception e) {
                Base.error(e);
                Base.showWarning(Translate.t("Download Failed"), Translate.w("The download failed at point 3 because %1", 40, "\n", e.toString()), e);
                isDownloading = false;
            }
        }

        public void install() {
            bar.setString("Installing");
            setProgress(0);

            if (type == PLUGIN) {
                try {
                    Base.copyFile(dest, new File(Base.getUserPluginsFolder(), dest.getName()));
                    setProgress(100);
                    Base.reloadPlugins();
                    setInstalled();
                } catch (Exception e) {
                    Base.error(e);
                    Base.showWarning(Translate.t("Install Failed"), Translate.w("The install failed because %1", 40, "\n", e.toString()), e);
                }
            }

            if (type == CORE) {
                if (isOutdated() || isInstalled()) {
                    uninstall();
                }
                installer = new ZipExtractor(dest, Base.getUserCoresFolder(), this);
                installer.execute();
            }

            if (type == BOARD) {
                if (isOutdated() || isInstalled()) {
                    uninstall();
                }
                installer = new ZipExtractor(dest, Base.getUserBoardsFolder(), this);
                installer.execute();
            }

            if (type == COMPILER) {
                if (isOutdated() || isInstalled()) {
                    uninstall();
                }
                installer = new ZipExtractor(dest, Base.getUserCompilersFolder(), this);
                installer.execute();
            }
            
        }

        public void setInstalled() {
            if (installNext != null) {
                installNext.startDownload();
                installNext = null;
            }
            isDownloading = false;
            installedVersion = availableVersion;
            updateDisplay();
            repaint();
            win.repaint();
            win.pack();
        }

        public boolean isQueued() {
            return isQueued;
        }

        public boolean isDownloading() {
            return isDownloading;
        }
        
    }

    public static class ZipExtractor extends SwingWorker<Void, Integer>
    {
        File inputFile;
        File destination;
        PluginEntry pi;

        public ZipExtractor(File in, File out, PluginEntry p) {
            this.inputFile = in;
            this.destination = out;
            this.pi = p;
        }

        public ZipExtractor(String in, String out) {
            this.inputFile = new File(in);
            this.destination = new File(out);
        }

        @Override
        protected Void doInBackground() {
            byte[] buffer = new byte[1024];
            ArrayList<String> fileList = new ArrayList<String>();
            publish(-1);
            int files = Base.countZipEntries(inputFile);
            pi.setMax((long)files);
            if (files == -1) {
                System.err.println("Zip file empty");
                Base.showWarning(Translate.t("Install Failed"), Translate.w("The install failed: The jar file has no entries.", 40, "\n"), null);
                return null;
            }
            int done = 0;
            try {
                ZipInputStream zis = new ZipInputStream(new FileInputStream(inputFile));
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    String fileName = ze.getName();
                    File newFile = new File(destination, fileName);

                    new File(newFile.getParent()).mkdirs();

                    if (ze.isDirectory()) {
                        newFile.mkdirs();
                    } else {

                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        newFile.setExecutable(true, false);
                    }
                    done++;
                    publish(done);
                    ze = zis.getNextEntry();
                    Thread.yield();
                }
                zis.closeEntry();
                zis.close();
            } catch (Exception e) {
                Base.error(e);
                Base.showWarning(Translate.t("Install Failed"), Translate.w("The install failed because %1", 40, "\n", e.toString()), e);
                return null;
            }
            return null;
        }

        @Override
        protected void done() {
            Base.loadCompilers();
            Base.loadCores();
            Base.loadBoards();
            Base.gatherLibraries();
            for (Editor e : Base.editors) {
                e.rebuildCoresMenu();
                e.rebuildBoardsMenu();
                e.rebuildImportMenu();
                e.rebuildExamplesMenu();
                e.rebuildPluginsMenu();
            }
            pi.setInstalled();
        }

        @Override
        protected void process(java.util.List<Integer> pct) {
            int p = pct.get(pct.size() - 1);
            pi.setProgress(p);
        }
    };

}

