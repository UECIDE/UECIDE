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
import java.awt.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;

public class PluginManager extends BasePlugin
{
    JFrame win;
    JButton refreshButton;
    JTabbedPane tabs;
    JPanel plugins;
    JPanel cores;
    JPanel boards;
    JPanel compilers;
    JScrollPane pluginsScroll;
    JScrollPane coresScroll;
    JScrollPane boardsScroll;
    JScrollPane compilersScroll;

    AbstractTableModel pluginListModel;
    AbstractTableModel coreListModel;
    AbstractTableModel boardListModel;

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

    public HashMap<String, PluginInfo> pluginList = null;

    public void init(Editor editor)
    {
        this.editor = editor;
    }

    public void run()
    {
        JPanel tp;

        win = new JFrame(Translate.t("Plugin Manager"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(false);

        tabs = new JTabbedPane();

        plugins = new JPanel(new GridBagLayout());
        tp = new JPanel(new BorderLayout());
        tp.add(plugins, BorderLayout.NORTH);
        pluginsScroll = new JScrollPane(tp);
        tabs.add("Plugins", pluginsScroll);

        boards = new JPanel(new GridBagLayout());
        tp = new JPanel(new BorderLayout());
        tp.add(boards, BorderLayout.NORTH);
        boardsScroll = new JScrollPane(tp);
        tabs.add("Boards", boardsScroll);

        cores = new JPanel(new GridBagLayout());
        tp = new JPanel(new BorderLayout());
        tp.add(cores, BorderLayout.NORTH);
        coresScroll = new JScrollPane(tp);
        tabs.add("Cores", coresScroll);

        compilers = new JPanel(new GridBagLayout());
        tp = new JPanel(new BorderLayout());
        tp.add(compilers, BorderLayout.NORTH);
        compilersScroll = new JScrollPane(tp);
        tabs.add("Compilers", compilersScroll);

        Box box = Box.createVerticalBox();

        box.add(tabs);

        populatePlugins();
        populateCores();
        populateBoards();

        Box line = Box.createHorizontalBox();
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

    public void populatePlugins() {
        plugins.removeAll();
        String[] entries = PluginManager.availablePlugins.keySet().toArray(new String[0]);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        pluginObjects.clear();

        JLabel label;
        JButton button;
        for (String entry : entries) {
            JSONObject plugin = PluginManager.availablePlugins.get(entry);
            label = new JLabel(entry);
            c.weightx = 0.9;
            c.gridwidth = 1;
            plugins.add(label, c);

            int compare = 0; // Not installed

            c.weightx = 0.1;
            c.gridx = 1;

            PluginEntry pe = new PluginEntry(plugin, 1);
            pluginObjects.put((String)plugin.get("Main-Class"), pe);
            plugins.add(pe, c);
                
            c.gridx = 0;
            c.gridy ++;
            c.gridwidth = 2;
            label = new JLabel("Version: " + (String)plugin.get("Version"));
            plugins.add(label, c);
            c.gridy ++;
            String d = (String)plugin.get("Description");
            if (d == null) d = "";
            JTextArea ta = new JTextArea("\n" + d);
            ta.setLineWrap(true);
            ta.setEditable(false);
            ta.setOpaque(false);
            plugins.add(ta, c);
            c.gridy++;


            JSeparator s = new JSeparator(SwingConstants.HORIZONTAL);
            plugins.add(s, c);
            c.gridy ++;
        }
        win.pack();
    }

    public void populateCores() {
        cores.removeAll();
        String[] entries = PluginManager.availableCores.keySet().toArray(new String[0]);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        coreObjects.clear();

        JLabel label;
        JButton button;
        for (String entry : entries) {
            JSONObject plugin = PluginManager.availableCores.get(entry);
            label = new JLabel(entry);
            c.weightx = 0.9;
            c.gridwidth = 1;
            cores.add(label, c);

            int compare = 0; // Not installed

            c.weightx = 0.1;
            c.gridx = 1;

            PluginEntry pe = new PluginEntry(plugin, 2);
            coreObjects.put((String)plugin.get("Core"), pe);
            cores.add(pe, c);
                
            c.gridx = 0;
            c.gridy ++;
            c.gridwidth = 2;

            label = new JLabel("Version: " + (String)plugin.get("Version"));
            cores.add(label, c);
            c.gridy ++;

            String comp = (String)plugin.get("Compiler");
            if (Base.compilers.get(comp) == null) {
                label = new JLabel("Requires Compiler: " + comp + " (not installed)");
                cores.add(label, c);
                c.gridy ++;
            }

            String d = (String)plugin.get("Description");
            if (d == null) d = "";
            JTextArea ta = new JTextArea("\n" + d);
            ta.setLineWrap(true);
            ta.setEditable(false);
            ta.setOpaque(false);
            cores.add(ta, c);
            c.gridy++;


            JSeparator s = new JSeparator(SwingConstants.HORIZONTAL);
            cores.add(s, c);
            c.gridy ++;
        }
        win.pack();
    }

    public void populateBoards() {
        boards.removeAll();
        String[] entries = PluginManager.availableBoards.keySet().toArray(new String[0]);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        boardObjects.clear();
        JLabel label;
        JButton button;
        for (String entry : entries) {
            JSONObject plugin = PluginManager.availableBoards.get(entry);
            label = new JLabel((String)plugin.get("Boards"));
            c.weightx = 0.9;
            c.gridwidth = 1;
            boards.add(label, c);

            int compare = 0; // Not installed

            c.weightx = 0.1;
            c.gridx = 1;

            PluginEntry pe = new PluginEntry(plugin, 3);
            boardObjects.put(entry, pe);
            boards.add(pe, c);
                
            c.gridx = 0;
            c.gridy ++;
            c.gridwidth = 2;
            label = new JLabel("Version: " + (String)plugin.get("Version") + ", Family: " + (String)plugin.get("Family"));
            boards.add(label, c);
            c.gridy ++;
            String d = (String)plugin.get("Description");
            if (d == null) d = "";
            JTextArea ta = new JTextArea("\n" + d);
            ta.setLineWrap(true);
            ta.setEditable(false);
            ta.setOpaque(false);
            boards.add(ta, c);
            c.gridy++;


            JSeparator s = new JSeparator(SwingConstants.HORIZONTAL);
            boards.add(s, c);
            c.gridy ++;
        }
        win.pack();
    }

    public void populateCompilers() {
        compilers.removeAll();
        String[] entries = PluginManager.availableCompilers.keySet().toArray(new String[0]);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        compilerObjects.clear();

        JLabel label;
        JButton button;
        for (String entry : entries) {
            JSONObject plugin = PluginManager.availableCompilers.get(entry);
            label = new JLabel(entry);
            c.weightx = 0.9;
            c.gridwidth = 1;
            compilers.add(label, c);

            int compare = 0; // Not installed

            c.weightx = 0.1;
            c.gridx = 1;

            PluginEntry pe = new PluginEntry(plugin, 4);
            compilerObjects.put((String)plugin.get("Compiler"), pe);
            compilers.add(pe, c);
                
            c.gridx = 0;
            c.gridy ++;
            c.gridwidth = 2;
            label = new JLabel("Version: " + (String)plugin.get("Version"));
            compilers.add(label, c);
            c.gridy ++;
            String d = (String)plugin.get("Description");
            if (d == null) d = "";
            JTextArea ta = new JTextArea("\n" + d);
            ta.setLineWrap(true);
            ta.setEditable(false);
            ta.setOpaque(false);
            compilers.add(ta, c);
            c.gridy++;


            JSeparator s = new JSeparator(SwingConstants.HORIZONTAL);
            compilers.add(s, c);
            c.gridy ++;
        }
        win.pack();
    }

    public void close()
    {
        win.dispose();
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
        try {
            URL page = new URL("http://uecide.org/version.php?platform=" + Base.getOSName() + "&arch=" + Base.getOSArch());
            BufferedReader in = new BufferedReader(new InputStreamReader(page.openStream()));
            String data = in.readLine();
            in.close();

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
            populatePlugins();
            populateCores();
            populateBoards();
            populateCompilers();
        } catch (Exception e) {
            e.printStackTrace();
        }
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


    public class PluginEntry extends JPanel implements ActionListener {
        String name;
        String installedVersion;
        String availableVersion;
        String url;
        JButton button;
        JLabel label;
        JProgressBar bar;
        int type;
        SwingWorker sw;
        File dest;
        String mainClass;
        JSONObject data;

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
                name = (String)o.get("Name");
                Board c = Base.boards.get(name);
                if (c != null) {    
                    installedVersion = "Unknown";
                }
            }

            if (type == COMPILER) {
                name = (String)o.get("Compiler");
                uecide.app.debug.Compiler c = Base.compilers.get(name);
                if (c != null) {    
                    installedVersion = c.getVersion();
                }
            }

            if (availableVersion == null) {
                return;
            }
            if (installedVersion == null) {
                button = new JButton("Install");
                this.add(button);
                return;
            }
            if (installedVersion == "") {
                button = new JButton("Install");
                button.addActionListener(this);
                button.setActionCommand("install");
                this.add(button);
                return;
            }
            if (installedVersion.equals(availableVersion)) {
                label = new JLabel("Installed");
                this.add(label);
                return;
            }
            if (installedVersion.compareTo(availableVersion) < 0) {
                button = new JButton("Upgrade");
                button.addActionListener(this);
                button.setActionCommand("upgrade");
                this.add(button);
                return;
            }
        }

        public void actionPerformed(ActionEvent e) {
            startDownload();
        }

        public void startDownload(PluginEntry pe) {
            installNext = pe;
            startDownload();
        }

        public void startDownload() {
            this.removeAll();
            bar = new JProgressBar(0, 100);

            if (type == CORE) {
                if (Base.compilers.get((String)data.get("Compiler")) == null) {
                    PluginEntry pe = compilerObjects.get((String)data.get("Compiler"));
                    if (pe != null) {
                        pe.startDownload(this);
                        bar.setIndeterminate(false);
                        bar.setString("Installing Compiler...");
                        bar.setStringPainted(true);
                        this.add(bar);
                        this.getParent().revalidate();
                        return;
                    }
                }
            }
            bar.setString("Downloading");
            bar.setStringPainted(true);
            bar.setIndeterminate(false);
            this.add(bar);
            this.getParent().revalidate();

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

                sw = new SwingWorker<Void, Long>(){
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
                        }
                        return null;
                    }

                    @Override
                    public void done() {
                        try {
                            in.close();
                            out.close();
                            PluginEntry.this.install();
                        } catch (Exception ex) {
                        }
                    }

                    @Override
                    protected void process(java.util.List<Long> chunk) {
                        for (long num : chunk) {
                            PluginEntry.this.setProgress(num);
                        }
                    }
                
                };
                sw.execute();
            } catch (Exception e) {
                e.printStackTrace();
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
                    e.printStackTrace();
                }
            }

            if (type == CORE) {
                ZipExtractor ze = new ZipExtractor(dest, Base.getUserCoresFolder(), this);
                ze.execute();
            }

            if (type == BOARD) {
                ZipExtractor ze = new ZipExtractor(dest, Base.getUserBoardsFolder(), this);
                ze.execute();
            }

            if (type == COMPILER) {
                ZipExtractor ze = new ZipExtractor(dest, Base.getUserCompilersFolder(), this);
                ze.execute();
            }
            
        }

        public void setInstalled() {
            this.removeAll();
            label = new JLabel("Installed");
            this.add(label);
            this.getParent().revalidate();
            if (installNext != null) {
                installNext.startDownload();
                installNext = null;
            }
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
                e.printStackTrace();
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

