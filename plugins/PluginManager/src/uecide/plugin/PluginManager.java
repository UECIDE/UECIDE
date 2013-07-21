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
    JScrollPane listScroller;
    JTable list;
    JButton refreshButton;
    JButton upgradeAllButton;
    JButton upgradeButton;
    JButton installButton;
    JButton cancelButton;

    AbstractTableModel tm;

    public static HashMap<String, JSONObject> availableVersions = new HashMap<String, JSONObject>();

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
        updateDisplay();

        win = new JFrame(Translate.t("Plugin Manager"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(false);

        Box box = Box.createVerticalBox();

        tm = new AbstractTableModel() {
            public String getColumnName(int col) {
                switch(col) {
                    case 0: return Translate.t("Plugin"); 
                    case 1: return Translate.t("Installed");
                    case 2: return Translate.t("Available");
                    case 3: return Translate.t("Operation");
                    default: return "";
                }
            }
            public int getRowCount() {
                return pluginList.size();
            }
            public int getColumnCount() { 
                return 4;
            }
            public Object getValueAt(int row, int col) {
                String[] entries = pluginList.keySet().toArray(new String[pluginList.size()]);
                if (col == 0) {
                    return entries[row];
                }
                if (col == 1) {
                    return ((PluginInfo)pluginList.get(entries[row])).installed;
                }
                if (col == 2) {
                    return ((PluginInfo)pluginList.get(entries[row])).available;
                }
                if (col == 3) {
                    String installed = ((PluginInfo)pluginList.get(entries[row])).installed;
                    if (installed == null) {
                        return "Install";
                    }
                    if (installed.equals("")) {
                        return "Install";
                    }
                    String available = ((PluginInfo)pluginList.get(entries[row])).available;
                    if (available == null) {
                        return "";
                    }
                    if (!(available.equals(installed))) {
                        if (!(available.equals(""))) {
                            return "Upgrade";
                        }
                    }
                    return "";
                }
                return "";
            }
            public boolean isCellEditable(int row, int col) {
                return false;
            }
            public void setValueAt(Object value, int row, int col) {
            }
        };

        list = new JTable(tm);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                updateButtons();
            }
        });
        listScroller = new JScrollPane(list);
        box.add(listScroller);

        Box buttons = Box.createHorizontalBox();

        refreshButton = new JButton(Translate.t("Refresh"));
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateList();
            }
        });

        installButton = new JButton(Translate.t("Install"));
        installButton.setEnabled(false);
        installButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                installJar();
            }
        });

        upgradeButton = new JButton(Translate.t("Upgrade"));
        upgradeButton.setEnabled(false);
        upgradeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                upgradeJar();
            }
        });


        upgradeAllButton = new JButton(Translate.t("Upgrade All"));
        upgradeAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                upgradeAllPlugins();
            }
        });

        cancelButton = new JButton(Translate.t("Close"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                win.dispose();
            }
        });


        buttons.add(refreshButton);
        buttons.add(installButton);
        buttons.add(upgradeButton);
        buttons.add(upgradeAllButton);
        buttons.add(cancelButton);

        box.add(buttons);


        win.getContentPane().add(box);
        win.pack();

        Dimension size = win.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
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

        if (PluginManager.availableVersions.size() == 0) {
            updateList();
        }
            
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
            URL page = new URL("http://uecide.org/version.php");
            BufferedReader in = new BufferedReader(new InputStreamReader(page.openStream()));
            String data = in.readLine();
            in.close();

            JSONObject ob = (JSONObject)JSONValue.parse(data);
            JSONObject plugins = (JSONObject)ob.get("plugins");
            PluginManager.availableVersions.putAll(plugins);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class GetLatestVersions extends SwingWorker<Void, Void> {
        @Override
        public Void doInBackground() {
            updatePlugins();
            updateDisplay();
            return null;
        }
        public void done() {
            win.repaint();
            refreshButton.setEnabled(true);
            installButton.setEnabled(false);
            upgradeButton.setEnabled(false);
            upgradeAllButton.setEnabled(true);
            list.clearSelection();
            list.setEnabled(true);
            win.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            tm.fireTableDataChanged();
        }
    }

    public void updateList() {
        win.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        refreshButton.setEnabled(false);
        installButton.setEnabled(false);
        upgradeButton.setEnabled(false);
        upgradeAllButton.setEnabled(false);
        list.clearSelection();
        list.setEnabled(false);
        new GetLatestVersions().execute();
    }

    public void updateDisplay() {

        pluginList = new HashMap<String, PluginInfo>();

        String[] entries = Base.plugins.keySet().toArray(new String[Base.plugins.size()]);
        for (String entry : entries) {
            String name = Base.plugins.get(entry).getClass().getSimpleName();
            String version = Base.plugins.get(entry).getVersion();
            PluginInfo npi = new PluginInfo();
            npi.installed = version;
            pluginList.put(name, npi);
        }

        entries = PluginManager.availableVersions.keySet().toArray(new String[PluginManager.availableVersions.size()]);
        for (String entry : entries) {
            JSONObject ob = availableVersions.get(entry);
            String name = entry;
            String version = (String)ob.get("version");
            String url = (String)ob.get("url");
            PluginInfo opi = (PluginInfo)pluginList.get(name);
            if (opi == null) {
                PluginInfo npi = new PluginInfo();
                npi.installed = "";
                npi.available = version;
                npi.url = url;
                pluginList.put(name, npi);
            } else {
                opi.available = version;    
                opi.url = url;
                pluginList.put(name, opi);
            }
        }
    }

    public void upgradeAllPlugins() {
        new DoFullUpgrade().execute();
    }

    public class DoFullUpgrade extends SwingWorker<Void, String> {
        public Void doInBackground() {
            String[] entries = pluginList.keySet().toArray(new String[pluginList.size()]);
            for (String entry : entries) {
                PluginInfo item = pluginList.get(entry);
                if (item.installed == null) continue;
                if (item.installed.equals("")) continue;
                if (item.available == null) continue;
                if (item.available.equals("")) continue;
                if (item.installed.equals(item.available)) continue;
                publish(entry);
                editor.message("Upgrading " + entry + "...", 1);

                try {
                    File dest = getJarFileToTmp(entry, item.url);
                    if (dest == null) { 
                        editor.message("failed\n", 2);
                        continue;
                    }
                    if (dest.exists()) {
                        Base.handleInstallPlugin(dest);
                
                        dest.delete();
                        editor.message("done\n", 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        public void progress(java.util.List<String> el) {
            for (String e : el) {
            }
        }

        public void done() {
            updateDisplay();
            win.repaint();
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

    public void installJar() {
        int row = list.getSelectedRow();
        String[] entries = pluginList.keySet().toArray(new String[pluginList.size()]);
        String name = entries[row];
        PluginInfo item = pluginList.get(name);
        
        File dest = getJarFileToTmp(name, item.url);
        if (dest == null) { 
            return;
        }
        if (dest.exists()) {
            Base.handleInstallPlugin(dest);
    
            dest.delete();
            Base.reloadPlugins();
            updateDisplay();
            win.repaint();
        }
    }

    public void upgradeJar() {
        installJar();
    }

    public void updateButtons() {
        boolean installable = false;
        int row = list.getSelectedRow();
        String[] entries = pluginList.keySet().toArray(new String[pluginList.size()]);
        String name = entries[row];
        String installed = ((PluginInfo)pluginList.get(name)).installed;
        if (installed == null || installed.equals("")) {
            installButton.setEnabled(true);
            upgradeButton.setEnabled(false);
            return;
        }
        String available = ((PluginInfo)pluginList.get(name)).available;
        if ((available != null) && (!(available.equals(""))) && (!(available.equals(installed)))) {
            installButton.setEnabled(false);
            upgradeButton.setEnabled(true);
            return;
        }
        installButton.setEnabled(false);
        upgradeButton.setEnabled(false);
    }

    public int flags() {
        return BasePlugin.MENU_PLUGIN_TOP;
    }
    
}

