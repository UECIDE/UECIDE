package uecide.app;

import uecide.plugin.*;
import uecide.app.debug.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.JToolBar;
import java.nio.charset.*;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.modes.ArduinoTokenMaker;

import java.lang.reflect.Method;


/**
 * Main editor panel for the Processing Development Environment.
 */
@SuppressWarnings("serial")
public class Editor extends JFrame implements RunnerListener {

    static protected final String EMPTY =
        "                                                                     " +
        "                                                                     " +
        "                                                                     ";

    /** Command on Mac OS X, Ctrl on Windows and Linux */
    static final int SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    /** Command-W on Mac OS X, Ctrl-W on Windows and Linux */
    static final KeyStroke WINDOW_CLOSE_KEYSTROKE = KeyStroke.getKeyStroke('W', SHORTCUT_KEY_MASK);
    /** Command-Option on Mac OS X, Ctrl-Alt on Windows and Linux */
    static final int SHORTCUT_ALT_KEY_MASK = ActionEvent.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    PageFormat pageFormat;
    PrinterJob printerJob;

    // file, sketch, and tools menus for re-inserting items

    FindAndReplace findAndReplace;
    JMenu fileMenu;
    JMenu sketchMenu;
    JMenu hardwareMenu;
    JMenu toolsMenu;
    JMenu helpMenu;

    JLabel lineStatus;

    JToolBar toolbar;
    JMenu sketchbookMenu;
    JMenu examplesMenu;
    JMenu importMenu;

    JMenu boardsMenu;
    JMenu coresMenu;
    JMenu serialMenu;
    JMenu optionsMenu;
    JMenu programmersMenu;
    JMenu bootloadersMenu;

    HashMap<String, String> selectedOptions = new HashMap<String, String>();

    SerialMenuListener serialMenuListener;
  
    public EditorStatus status;
    EditorConsole console;

    JSplitPane splitPane;
    JPanel consolePanel;

    String serialPort = null;

    // currently opened program
    public Sketch sketch;

    // runtime information and window placement

    JMenuItem saveMenuItem;
    JMenuItem saveAsMenuItem;

    Runnable runHandler;
    Runnable presentHandler;
    Runnable stopHandler;
    Runnable exportHandler;
    Runnable exportAppHandler;

    JButton runButton;
    JButton burnButton;
    JButton newButton;
    JButton openButton;
    JButton saveButton;
    JButton consoleButton;

    JTabbedPane tabs;

    public String programmer = null;
    public Board board = null;
    public Core core = null;
    public uecide.app.debug.Compiler compiler = null;

    static final int ADD_NEW_FILE = 1;
    static final int RENAME_FILE = 2;

    public void setConsoleFont(Font f) {
        console.setFont(f);
    }

    public Editor(String path) {
        super(Base.theme.get("product"));

        serialPort = Base.preferences.get("serial.port");

        Base.setIcon(this);
 

        // Install default actions for Run, Present, etc.
        resetHandlers();

        final Editor me = this;
        // add listener to handle window close box hit event
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
        // don't close the window when clicked, the app will take care
        // of that via the handleQuitInternal() methods
        // http://dev.processing.org/bugs/show_bug.cgi?id=440
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // When bringing a window to front, let the Base know
        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                Base.handleActivated(me);
            }

            // added for 1.0.5
            // http://dev.processing.org/bugs/show_bug.cgi?id=1260
            public void windowDeactivated(WindowEvent e) {
            }
        });


        // For rev 0120, placing things inside a JPanel
        Container contentPain = getContentPane();
        contentPain.setLayout(new BorderLayout());
        JPanel pain = new JPanel();
        pain.setLayout(new BorderLayout());
        contentPain.add(pain, BorderLayout.CENTER);

        Box box = Box.createVerticalBox();
        Box upper = Box.createVerticalBox();

        toolbar = new JToolBar();
        toolbar.setBackground(Base.theme.getColor("buttons.bgcolor"));
        toolbar.setFloatable(false);

        File themeFolder = Base.getContentFile("lib/theme");
        if (!themeFolder.exists()) {
            System.err.println("PANIC: Theme folder doesn't exist! " + themeFolder.getAbsolutePath());
            return;
        }

        File runIconFile = new File(themeFolder, "run.png");
        ImageIcon runButtonIcon = new ImageIcon(runIconFile.getAbsolutePath());

        runButton = new JButton(runButtonIcon);
        runButton.setToolTipText(Translate.t("Verify"));
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                handleRun();
            }
        });
        toolbar.add(runButton);

        File burnIconFile = new File(themeFolder, "burn.png");
        ImageIcon burnButtonIcon = new ImageIcon(burnIconFile.getAbsolutePath());
        burnButton = new JButton(burnButtonIcon);
        burnButton.setToolTipText(Translate.t("Program"));
        burnButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                handleExport();
            }
        });
        toolbar.add(burnButton);

        toolbar.addSeparator();

        File newIconFile = new File(themeFolder, "new.png");
        ImageIcon newButtonIcon = new ImageIcon(newIconFile.getAbsolutePath());
        newButton = new JButton(newButtonIcon);
        newButton.setToolTipText(Translate.t("New"));
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                Base.handleNew();
            }
        });
        toolbar.add(newButton);

        File openIconFile = new File(themeFolder, "open.png");
        ImageIcon openButtonIcon = new ImageIcon(openIconFile.getAbsolutePath());
        openButton = new JButton(openButtonIcon);
        openButton.setToolTipText(Translate.t("Open Sketch"));
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                Base.handleOpenPrompt();
            }
        });
        toolbar.add(openButton);
       
        File saveIconFile = new File(themeFolder, "save.png");
        ImageIcon saveButtonIcon = new ImageIcon(saveIconFile.getAbsolutePath());
        saveButton = new JButton(saveButtonIcon);
        saveButton.setToolTipText(Translate.t("Save Sketch"));
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                handleSave();
            }
        });
        toolbar.add(saveButton);

        toolbar.addSeparator();

        String[] entries = (String[]) Base.plugins.keySet().toArray(new String[0]);

        for (String plugin : entries) {
            try {
                final Plugin t = Base.plugins.get(plugin);
                ImageIcon bi = t.toolbarIcon();
                if (bi != null) {
                    JButton button = new JButton(bi);
                    button.setToolTipText(t.getMenuTitle());
                    button.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            launchPlugin(t);
                        }
                    });
                    toolbar.add(button);
                }
            } catch(Exception e) {
                Base.error(e);
            }
        }

        Dimension dmax = toolbar.getMaximumSize();
        dmax.height = 32;
        dmax.width = 9999;
        toolbar.setMaximumSize(dmax);

        upper.add(toolbar);

        tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

        upper.add(tabs);

        // assemble console panel, consisting of status area and the console itself
        consolePanel = new JPanel();
        consolePanel.setLayout(new BorderLayout());

        status = new EditorStatus(this);
        consolePanel.add(status, BorderLayout.NORTH);

        console = new EditorConsole(this);
        // windows puts an ugly border on this guy
        console.setBorder(null);
        consolePanel.add(console, BorderLayout.CENTER);

        lineStatus = new JLabel("Status Goes Here");
        lineStatus.setHorizontalAlignment(SwingConstants.RIGHT);
        lineStatus.setBorder(new BevelBorder(BevelBorder.LOWERED));
        consolePanel.add(lineStatus, BorderLayout.SOUTH);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, consolePanel);

        splitPane.setOneTouchExpandable(true);
        // repaint child panes while resizing
        splitPane.setContinuousLayout(true);
        // if window increases in size, give all of increase to
        // the textarea in the uppper pane
        splitPane.setResizeWeight(1D);

        // to fix ugliness.. normally macosx java 1.3 puts an
        // ugly white border around this object, so turn it off.
        splitPane.setBorder(null);

        // the default size on windows is too small and kinda ugly
        int dividerSize = Base.preferences.getInteger("editor.divider.size");
        if (dividerSize != 0) {
            splitPane.setDividerSize(dividerSize);
        }

        box.add(splitPane);

        pain.add(box);

        // get shift down/up events so we can show the alt version of toolbar buttons

        pain.setTransferHandler(new FileDropHandler());

        buildMenuBar();

        // Finish preparing Editor (formerly found in Base)
        pack();


        // Bring back the general options for the editor


        if (path == null) {
            sketch = new Sketch(this, (File) null);
        } else {
            sketch = new Sketch(this, path);
            Base.updateMRU(sketch.getFolder());
        }

        sketch.checkForSettings();

        if (board == null) {
            selectBoard(Base.getDefaultBoard());
        }

        setMinimumSize(new Dimension(
            Base.preferences.getInteger("editor.window.width.min"),
            Base.preferences.getInteger("editor.window.height.min")
        ));
    
        if (Base.activeEditor != null) {
            Dimension oSize = Base.activeEditor.getSize();
            setSize(oSize.width, oSize.height);
            Point oPos = Base.activeEditor.getLocation();
            setLocation(oPos.x + 20, oPos.y + 20);
        } else {
            int width = Base.preferences.getInteger("editor.window.width");
            if (width < Base.preferences.getInteger("editor.window.width.min")) {
                width = Base.preferences.getInteger("editor.window.width.min");
            }
            int height = Base.preferences.getInteger("editor.window.height");
            if (height < Base.preferences.getInteger("editor.window.height.min")) {
                height = Base.preferences.getInteger("editor.window.height.min");
            }

            setSize(width, height);
            setLocation(Base.preferences.getInteger("editor.window.x"), Base.preferences.getInteger("editor.window.y"));
        }
        applyPreferences();
        setVisible(true);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                pullToFront();
            }
        });
    }

    public void pullToFront() {
        toFront();
        repaint();
    }

    static public String[] splitTokens(String what, String delim) {
        StringTokenizer toker = new StringTokenizer(what, delim);
        String pieces[] = new String[toker.countTokens()];

        int index = 0;
        while (toker.hasMoreTokens()) {
            pieces[index++] = toker.nextToken();
        }
        return pieces;
    }
  
  /**
   * Handles files dragged & dropped from the desktop and into the editor
   * window. Dragging files into the editor window is the same as using
   * "Sketch &rarr; Add File" for each file.
   */
  class FileDropHandler extends TransferHandler {
    public boolean canImport(JComponent dest, DataFlavor[] flavors) {
      return true;
    }

    @SuppressWarnings("unchecked")
    public boolean importData(JComponent src, Transferable transferable) {
      int successful = 0;

      try {
        DataFlavor uriListFlavor =
          new DataFlavor("text/uri-list;class=java.lang.String");

        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          java.util.List list = (java.util.List)
            transferable.getTransferData(DataFlavor.javaFileListFlavor);
          for (int i = 0; i < list.size(); i++) {
            File file = (File) list.get(i);
            if (sketch.addFile(file)) {
              successful++;
            }
          }
        } else if (transferable.isDataFlavorSupported(uriListFlavor)) {
          // Some platforms (Mac OS X and Linux, when this began) preferred
          // this method of moving files.
          String data = (String)transferable.getTransferData(uriListFlavor);
          String[] pieces = splitTokens(data, "\r\n");
          for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].startsWith("#")) continue;

            String path = null;
            if (pieces[i].startsWith("file:///")) {
              path = pieces[i].substring(7);
            } else if (pieces[i].startsWith("file:/")) {
              path = pieces[i].substring(5);
            }
            if (sketch.addFile(new File(path))) {
              successful++;
            }
          }
        }
      } catch (Exception e) {
        Base.error(e);
        return false;
      }

      if (successful == 0) {
        statusError(Translate.t("No files were added to the sketch."));

      } else if (successful == 1) {
        statusNotice(Translate.t("One file added to the sketch."));

      } else {
        statusNotice(Translate.t("%1 files added to the sketch.", Integer.toString(successful)));
      }
      return true;
    }
  }

  /**
   * Read and apply new values from the preferences, either because
   * the app is just starting up, or the user just finished messing
   * with things in the Base.preferences window.
   */
  protected void applyPreferences() {

    // apply the setting for 'use external editor'
    boolean external = Base.preferences.getBoolean("editor.external");

    saveMenuItem.setEnabled(!external);
    saveAsMenuItem.setEnabled(!external);

    for (int i = 0; i < tabs.getTabCount(); i++) {
        SketchEditor ed = (SketchEditor) tabs.getComponentAt(i);
        ed.refreshSettings();
        ed.setEditable(!external);
        ed.setBackground( external ?
            Base.theme.getColor("editor.external.bgcolor") :
            Base.theme.getColor("editor.bgcolor") 
        );

        ed.setFont(Base.preferences.getFont("editor.font"));
        console.setFont(Base.preferences.getFont("console.font"));
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  JMenuBar menubar;

  protected void buildMenuBar() {

    menubar = new JMenuBar();
    menubar = new JMenuBar();

    menubar.add(buildFileMenu());
    menubar.add(buildEditMenu());
    menubar.add(buildSketchMenu());
    menubar.add(buildHardwareMenu());
    menubar.add(buildToolsMenu());
    menubar.add(buildHelpMenu());
    setJMenuBar(menubar);
  }


  protected JMenu buildFileMenu() {
    JMenuItem item;
    fileMenu = new JMenu(Translate.t("File"));

    item = newJMenuItem(Translate.t("New"), 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.handleNew();
        }
      });
    fileMenu.add(item);

    item = Editor.newJMenuItem(Translate.t("Open..."), 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.handleOpenPrompt();
        }
      });
    fileMenu.add(item);

    if (sketchbookMenu == null) {
      sketchbookMenu = new JMenu(Translate.t("Recent Sketches"));
      rebuildMRUMenu();
    }
    fileMenu.add(sketchbookMenu);

    if (examplesMenu == null) {
      examplesMenu = new JMenu(Translate.t("Examples"));
      //rebuildExamplesMenu();
    }
    fileMenu.add(examplesMenu);

    item = Editor.newJMenuItem(Translate.t("Close"), 'W');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
         handleClose();
        }
      });
    fileMenu.add(item);

    saveMenuItem = newJMenuItem(Translate.t("Save"), 'S');
    saveMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleSave();
        }
      });
    fileMenu.add(saveMenuItem);

    saveAsMenuItem = newJMenuItemShift(Translate.t("Save As..."), 'S');
    saveAsMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleSaveAs();
        }
      });
    fileMenu.add(saveAsMenuItem);

    item = new JMenuItem(Translate.t("Export as SAR..."));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            sketch.exportSAR();
        }
    });
    fileMenu.add(item);
    item = new JMenuItem(Translate.t("Import SAR..."));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            sketch.importSAR();
        }
    });
    fileMenu.add(item);

    item = newJMenuItem(Translate.t("Compile and Upload"), 'U');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleExport();
        }
      });
    fileMenu.add(item);

    fileMenu.addSeparator();

    item = newJMenuItemShift(Translate.t("Page Setup"), 'P');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handlePageSetup();
        }
      });
    fileMenu.add(item);

    item = newJMenuItem(Translate.t("Print"), 'P');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handlePrint();
        }
      });
    fileMenu.add(item);

    // macosx already has its own preferences and quit menu
    if (!Base.isMacOS()) {
      fileMenu.addSeparator();

      item = newJMenuItem(Translate.t("Preferences"), ',');
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Base.handlePrefs();
          }
        });
      fileMenu.add(item);

      fileMenu.addSeparator();

      item = newJMenuItem(Translate.t("Quit"), 'Q');
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Base.handleQuit();
          }
        });
      fileMenu.add(item);
    }
    return fileMenu;
  }


  protected JMenu buildSketchMenu() {
    JMenuItem item;
    sketchMenu = new JMenu(Translate.t("Sketch"));

    item = newJMenuItem(Translate.t("Verify / Compile"), 'R');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun();
        }
      });
    sketchMenu.add(item);

    item = newJMenuItem(Translate.t("Clean Build Folder"), 'D');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          sketch.cleanBuild();
          statusNotice(Translate.t("Clean finished."));
        }
      });
    sketchMenu.add(item);

    item = new JMenuItem(Translate.t("Purge Cache Folder"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Base.removeDir(sketch.getCacheFolder());
            statusNotice(Translate.t("Purge finished."));
        }
      });
    sketchMenu.add(item);


    sketchMenu.addSeparator();

    if (importMenu == null) {
      importMenu = new JMenu(Translate.t("Import Library..."));
      //rebuildImportMenu();
    }
    sketchMenu.add(importMenu);

    item = newJMenuItem(Translate.t("Show Sketch Folder"), 'K');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openFolder(sketch.getFolder());
        }
      });
    sketchMenu.add(item);
    item.setEnabled(Base.openFolderAvailable());

    item = newJMenuItem(Translate.t("Show Build Folder"), 'K');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openFolder(sketch.getBuildFolder());
        }
      });
    sketchMenu.add(item);
    item.setEnabled(Base.openFolderAvailable());

    item = newJMenuItemShift(Translate.t("New File..."), 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          sketch.handleNewFile();
        }
      });
    sketchMenu.add(item);

    item = newJMenuItemShift(Translate.t("Add File..."), 'A');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          sketch.handleAddFile();
        }
      });
    sketchMenu.add(item);

    item = new JMenuItem(Translate.t("Rename Tab..."));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          sketch.handleRenameTab();
        }
      });
    sketchMenu.add(item);

    return sketchMenu;
  }


  protected JMenu buildHardwareMenu() {
    hardwareMenu = new JMenu(Translate.t("Hardware"));
    JMenu menu = hardwareMenu;
    JMenuItem item;

    if (boardsMenu == null) {
      boardsMenu = new JMenu(Translate.t("Board"));
      rebuildBoardsMenu();
    }
    menu.add(boardsMenu);

    if (coresMenu == null) {
        coresMenu = new JMenu(Translate.t("Cores"));
        rebuildCoresMenu();
    }
    menu.add(coresMenu);
    
    if (optionsMenu == null) {
        optionsMenu = new JMenu(Translate.t("Options"));
        rebuildOptionsMenu();
    }
    menu.add(optionsMenu);

    if (serialMenuListener == null)
      serialMenuListener  = new SerialMenuListener();
    if (serialMenu == null)
      serialMenu = new JMenu(Translate.t("Serial Port"));
    populateSerialMenu();
    menu.add(serialMenu);

    serialMenu.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent e) {}
      public void menuDeselected(MenuEvent e) {}
      public void menuSelected(MenuEvent e) {
        populateSerialMenu();
      }
    });

    if (programmersMenu == null) {
        programmersMenu = new JMenu(Translate.t("Programmers"));
        rebuildProgrammersMenu();
    }
    menu.add(programmersMenu);
    if (bootloadersMenu == null) {
        bootloadersMenu = new JMenu(Translate.t("Burn Bootloader"));
        rebuildBootloadersMenu();
    }
    menu.add(bootloadersMenu);
    return menu;
  }

    public ArrayList<String> getAllChildren(String key) {
        ArrayList<String> children = new ArrayList<String>();
        if (compiler != null) {
            ArrayList<String> compilerChildren = compiler.getProperties().children(key);
            for (String c : compilerChildren) {
                int i = children.indexOf(c);
                if (i >= 0) {
                    children.remove(i);
                }
                children.add(c);
            }
        }
        if (core != null) {
            ArrayList<String> coreChildren = core.getProperties().children(key);
            for (String c : coreChildren) {
                int i = children.indexOf(c);
                if (i >= 0) {
                    children.remove(i);
                }
                children.add(c);
            }
        }
        if (board != null) {
            ArrayList<String> boardChildren = board.getProperties().children(key);
            for (String c : boardChildren) {
                int i = children.indexOf(c);
                if (i >= 0) {
                    children.remove(i);
                }
                children.add(c);
            }
        }
        return children;
    }
    
    public boolean getBoolByKey(String key) {
        if (board.getProperties().get(key) != null) {
            return board.getProperties().getBoolean(key);
        }
        if (core.getProperties().get(key) != null) {
            return core.getProperties().getBoolean(key);
        }
        if (compiler.getProperties().get(key) != null) {
            return compiler.getProperties().getBoolean(key);
        }
        return false;
    }
    public String getAllByKey(String key) {
        if (board.getProperties().get(key) != null) {
            return board.getProperties().get(key);
        }
        if (core.getProperties().get(key) != null) {
            return core.getProperties().get(key);
        }
        if (compiler.getProperties().get(key) != null) {
            return compiler.getProperties().get(key);
        }
        return null;
    }

    public void rebuildOptionsMenu() {
        selectedOptions = new HashMap<String, String>();
        if (board == null) {
            optionsMenu.setEnabled(false);
            return;
        }
        optionsMenu.removeAll();
        ArrayList<String>options = getAllChildren("options");
        for (String opt : options) {
            String optionName = getAllByKey("options." + opt + ".name");
            String optionDefault = getAllByKey("options." + opt + ".default");
            JMenu optMen = new JMenu(optionName);
            JMenuItem defaultItem = null;
            boolean gotSelected = false;

            ArrayList<String>optKids = getAllChildren("options." + opt);
            ButtonGroup bg = new ButtonGroup();
            for (String kid : optKids) {
                if (kid.equals("name") || kid.equals("default") || kid.equals("purge")) {
                    continue;
                }
                String kidName = getAllByKey("options." + opt + "." + kid + ".name");
                if (kidName != null) {
                    AbstractAction action = new AbstractAction(kidName) {
                        public void actionPerformed(ActionEvent actionevent) {
                            setOption((String)getValue("opt"), (String)getValue("sub"));
                        }
                    };
                    action.putValue("opt", opt);
                    action.putValue("sub", kid);
                    JMenuItem item = new JRadioButtonMenuItem(action);
                    if (kid.equals(optionDefault)) {
                        defaultItem = item;
                    }
                    bg.add(item);
                    if (optionIsSet(opt, kid)) {
                        setOption(opt, kid);
                        item.setSelected(true); 
                        gotSelected = true;
                    }

                    if (Base.preferences.get("options." + board.getName() + "." + opt) != null) {
                        if (Base.preferences.get("options." + board.getName() + "." + opt).equals(kid)) {
                            setOption(opt, kid);
                            item.setSelected(true);
                            gotSelected = true;
                        }
                    }

                    if (!gotSelected) {
                        if (defaultItem != null) {
                            setOption(opt, optionDefault);
                            defaultItem.setSelected(true);
                        }
                    }
                    optMen.add(item);
                }
            }
            optionsMenu.add(optMen);
        }
        if (optionsMenu.getItemCount() == 0) {
            optionsMenu.setEnabled(false);
        } else {
            optionsMenu.setEnabled(true);
        }
    }

    public void setOption(String opt, String val) {
        selectedOptions.put(opt, val);
        optionsToPreferences();
        if (getBoolByKey("options." + opt + ".purge")) {
            sketch.needPurge();
        }
    }

    public boolean optionIsSet(String opt, String val) {
        if (selectedOptions.get(opt) != null) {
            if (selectedOptions.get(opt).equals(val)) {
                return true;
            }
        }
        return false;
    }

    public void optionsToPreferences() {
        for (String opt : selectedOptions.keySet()) {
            Base.preferences.set("options." + board.getName() + "." + opt, selectedOptions.get(opt));
        }
    }

    public String getFlags(String type) {
        String flags = "";
        for (String opt : selectedOptions.keySet()) {
            String f = getAllByKey("options." + opt + "." + selectedOptions.get(opt) + "." + type);
            if (f != null) {
                flags = flags + "::" + f;
            }
        }
        return flags;
    }
 
    public JMenu buildToolsMenu()
    {
        toolsMenu = new JMenu(Translate.t("Tools"));
        JMenu menu = toolsMenu;
        JMenuItem item;
        rebuildPluginsMenu();
        return menu;
    }

  class SerialMenuListener implements ActionListener {
    //public SerialMenuListener() { }

    public void actionPerformed(ActionEvent e) {
      selectSerialPort(((JCheckBoxMenuItem)e.getSource()).getText());
    }
  }
  
    protected void selectSerialPort(String name) {
        if(serialMenu == null) {
            return;
        }
        if (name == null) {
            return;
        }
        JCheckBoxMenuItem selection = null;
        for (int i = 0; i < serialMenu.getItemCount(); i++) {
            JCheckBoxMenuItem item = ((JCheckBoxMenuItem)serialMenu.getItem(i));
            if (item == null) {
                continue;
            }
            item.setState(false);
            if (name.equals(item.getText())) selection = item;
        }
        if (selection != null) selection.setState(true);
        Base.preferences.set("serial.port", name);
        serialPort = name;
        updateLineStatus();
    }

    public String getSerialPort() {
        return serialPort;
    }

    protected void populateSerialMenu() {
        JMenuItem rbMenuItem;
    
        serialMenu.removeAll();
        boolean empty = true;

        Serial.updatePortList();
        ArrayList<String> portList = Serial.getPortList();
        for (String p : portList) {
            boolean exists = true;
            if (Base.isLinux() || Base.isMacOS()) {
                File f = new File(p);
                if (!f.exists()) {
                    exists = false;
                }
            }
            rbMenuItem = new JCheckBoxMenuItem(p, p.equals(serialPort));
            rbMenuItem.setEnabled(exists);
            rbMenuItem.addActionListener(serialMenuListener);
            serialMenu.add(rbMenuItem);
            empty = false;
        }
        serialMenu.setEnabled(true);
    }


  protected JMenu buildHelpMenu() {
    helpMenu = new JMenu(Translate.t("Help"));
    rebuildHelpMenu();
    return helpMenu;
  }

  public void rebuildHelpMenu() {
    helpMenu.removeAll();
    JMenuItem item;

    item = new JMenuItem(Translate.t("About This Sketch"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            sketch.about();
        }
    });
    helpMenu.add(item);

    if (board != null) {
        if (board.getManual() != null) {
            item = new JMenuItem(Translate.t("Manual for %1", board.getLongName()));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Base.open(board.getManual().getAbsolutePath());
                }
            });
            helpMenu.add(item);
        }
    }

    if (core != null) {
        if (core.getManual() != null) {
            item = new JMenuItem(Translate.t("Manual for %1", board.getName()));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Base.open(core.getManual().getAbsolutePath());
                }
            });
            helpMenu.add(item);
        }
    }

    if (Base.theme.get("links.gettingstarted.url") != null) {
        item = new JMenuItem(Translate.t("Getting Started"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Base.theme.get("links.gettingstarted.url"));
            }
          });
        helpMenu.add(item);
    }

    if (Base.theme.get("links.environment.url") != null) {
        item = new JMenuItem(Translate.t("Environment"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Base.theme.get("links.environment.url"));
            }
          });
        helpMenu.add(item);
    }

    if (Base.theme.get("links.troubleshooting.url") != null) {
        item = new JMenuItem(Translate.t("Troubleshooting"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Base.theme.get("links.troubleshooting.url"));
            }
          });
        helpMenu.add(item);
    }


    if (Base.theme.get("links.reference.url") != null) {
        item = new JMenuItem(Translate.t("Reference"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Base.theme.get("links.reference.url"));
            }
          });
        helpMenu.add(item);
    }

    if (Base.theme.get("links.faq.url") != null) {
        item = new JMenuItem(Translate.t("Frequently Asked Questions"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Base.theme.get("links.faq.url"));
            }
          });
        helpMenu.add(item);
    }

    String linkName = Base.theme.get("links.homepage.name");
    if (linkName != null) {
        item = new JMenuItem(linkName);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Base.theme.get("links.homepage.url"));
            }
        });
        helpMenu.add(item);
    }

    linkName = Base.theme.get("links.forums.name");
    if (linkName != null) {
        item = new JMenuItem(linkName);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Base.theme.get("links.forums.url"));
            }
        });
        helpMenu.add(item);
    }

    // macosx already has its own about menu
    if (!Base.isMacOS()) {
      helpMenu.addSeparator();
      item = new JMenuItem(Translate.t("About %1", Base.theme.get("product.cap")));
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Base.handleAbout();
          }
        });
      helpMenu.add(item);
    }

    item = new JMenuItem(Translate.t("System Information"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Base.handleSystemInfo();
        }
    });
    helpMenu.add(item);

  }

    public void handleNewFile() {
        status.edit(Translate.t("New File Name:"), "", ADD_NEW_FILE);
    }


    protected JMenu buildEditMenu() {
        JMenu menu = new JMenu(Translate.t("Edit"));
        JMenuItem item;

        final Editor me = this;

        item = newJMenuItem(Translate.t("Undo"), 'Z');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((SketchEditor)tabs.getSelectedComponent()).undo();
            }
        });
        menu.add(item);

        item = newJMenuItem(Translate.t("Redo"), 'Y');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((SketchEditor)tabs.getSelectedComponent()).redo();
            }
        });
        menu.add(item);

        addPluginsToMenu(menu, BasePlugin.MENU_EDIT_TOP);

        menu.addSeparator();

        item = newJMenuItem(Translate.t("Cut"), 'X');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleCut();
            }
        });
        menu.add(item);

        item = newJMenuItem(Translate.t("Copy"), 'C');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((SketchEditor)tabs.getSelectedComponent()).copy();
            }
        });
        menu.add(item);
        item = newJMenuItem(Translate.t("Paste"), 'V');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((SketchEditor)tabs.getSelectedComponent()).paste();
            }
        });
        menu.add(item);

        item = newJMenuItem(Translate.t("Select All"), 'A');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((SketchEditor)tabs.getSelectedComponent()).selectAll();
            }
        });
        menu.add(item);

        addPluginsToMenu(menu, BasePlugin.MENU_EDIT_MID);
        menu.addSeparator();

        item = newJMenuItem(Translate.t("Comment/Uncomment"), '/');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              handleCommentUncomment();
            }
        });
        menu.add(item);

        item = newJMenuItem(Translate.t("Increase Indent"), ']');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              increaseIndent();
            }
        });
        menu.add(item);

        item = newJMenuItem(Translate.t("Decrease Indent"), '[');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              decreaseIndent();
            }
        });
        menu.add(item);

        addPluginsToMenu(menu, BasePlugin.MENU_EDIT_LOW);
        menu.addSeparator();

        item = newJMenuItem(Translate.t("Find..."), 'F');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                findAndReplace = new FindAndReplace(me);
            }
        });
        menu.add(item);

        addPluginsToMenu(menu, BasePlugin.MENU_EDIT_BOT);
        return menu;
    }

    static public JMenuItem newJMenuItem(String title, int what) {
        JMenuItem menuItem = new JMenuItem(title);
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
        return menuItem;
    }


    static public JMenuItem newJMenuItemShift(String title, int what) {
        JMenuItem menuItem = new JMenuItem(title);
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        modifiers |= ActionEvent.SHIFT_MASK;
        menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
        return menuItem;
    }

    static public JMenuItem newJMenuItemAlt(String title, int what) {
        JMenuItem menuItem = new JMenuItem(title);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(what, SHORTCUT_ALT_KEY_MASK));
        return menuItem;
    }

    public void resetHandlers() {
        runHandler = new DefaultRunHandler();
        exportHandler = new DefaultExportHandler();
    }

    public Sketch getSketch() {
        return sketch;
    }

    public SketchEditor getTextArea() {
        return (SketchEditor)tabs.getSelectedComponent();
    }

    public String getText() {
        return ((SketchEditor)tabs.getSelectedComponent()).getText();
    }

    public String getText(String filename) {
        int iot = tabs.indexOfTab(filename);
        if (iot == -1) {
            return "";
        }
        SketchEditor ed = (SketchEditor)tabs.getComponentAt(iot);
        return ed.getText();
    }

    public String getText(int start, int stop) {
        return ((SketchEditor)tabs.getSelectedComponent()).getText(start, stop - start);
    }

    public void setText(String what) {
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        ed.beginAtomicEdit();
        ed.setText(what);
        ed.endAtomicEdit();
    }

    public void insertText(String what) {
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        ed.beginAtomicEdit();
        int caret = ed.getCaretPosition();
        ed.insert(what, caret);
        ed.endAtomicEdit();
    }

    public String getSelectedText() {
        return ((SketchEditor)tabs.getSelectedComponent()).getSelectedText();
    }


    public void setSelectedText(String what) {
        ((SketchEditor)tabs.getSelectedComponent()).setSelectedText(what);
    }

    public void setSelection(int start, int stop) {
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        if (start < 0) start = 0;
        if (start > ed.getDocumentLength()) start = ed.getDocumentLength();
        if (stop < 0) stop = 0;
        if (stop > ed.getDocumentLength()) start = ed.getDocumentLength();

        ed.select(start, stop);
    }

    public int getCaretOffset() {
        return ((SketchEditor)tabs.getSelectedComponent()).getCaretPosition();
    }

    public boolean isSelectionActive() {
        return ((SketchEditor)tabs.getSelectedComponent()).isSelectionActive();
    }

    public int getSelectionStart() {
        return ((SketchEditor)tabs.getSelectedComponent()).getSelectionStart();
    }

    public int getSelectionStop() {
        return ((SketchEditor)tabs.getSelectedComponent()).getSelectionStop();
    }

    public String getLineText(int line) {
        return ((SketchEditor)tabs.getSelectedComponent()).getLineText(line);
    }

    public void setLineText(int line, String what) {
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        ed.beginAtomicEdit();
        ed.select(ed.getLineStartOffset(line), ed.getLineEndOffset(line));
        ed.setSelectedText(what);
        ed.endAtomicEdit();
    }

    public int getLineStartOffset(int line) {
        return ((SketchEditor)tabs.getSelectedComponent()).getLineStartOffset(line);
    }

    public int getLineStopOffset(int line) {
        return ((SketchEditor)tabs.getSelectedComponent()).getLineEndOffset(line);
    }

    public int getLineCount() {
        return ((SketchEditor)tabs.getSelectedComponent()).getLineCount();
    }

    public void handleCut() {
        ((SketchEditor)tabs.getSelectedComponent()).cut();
    }

    public void handleCopy() {
        ((SketchEditor)tabs.getSelectedComponent()).copy();
    }

    public void handlePaste() {
        ((SketchEditor)tabs.getSelectedComponent()).paste();
    }

    public void handleSelectAll() {
        ((SketchEditor)tabs.getSelectedComponent()).selectAll();
    }

    protected void handleCommentUncomment() {
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        ed.beginAtomicEdit();

        int startLine = ed.getSelectionStartLine();
        int stopLine = ed.getSelectionStopLine();

        int lastLineStart = ed.getLineStartOffset(stopLine);
        int selectionStop = ed.getSelectionStop();

        if (selectionStop == lastLineStart) {
            if (ed.isSelectionActive()) {
                stopLine--;
            }
        }

        int length = ed.getDocumentLength();
        boolean commented = true;
        for (int i = startLine; commented && (i <= stopLine); i++) {
            int pos = ed.getLineStartOffset(i);
            if (pos + 2 > length) {
                commented = false;
            } else {
                String begin = ed.getText(pos, 2);
                commented = begin.equals("//");
            }
        }

        for (int line = startLine; line <= stopLine; line++) {
            int location = ed.getLineStartOffset(line);
            if (commented) {
                ed.select(location, location+2);
                if (ed.getSelectedText().equals("//")) {
                    ed.setSelectedText("");
                }
            } else {
                ed.select(location, location);
                ed.setSelectedText("//");
            }
        }
        ed.select(ed.getLineStartOffset(startLine),
        ed.getLineEndOffset(stopLine) - 1);
        ed.endAtomicEdit();
    }


    public void increaseIndent() {
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        ed.beginAtomicEdit();
        int startLine;
        int stopLine;
        if (!ed.isSelectionActive()) {
            startLine = stopLine = ed.getCaretLineNumber();    
        } else {
            startLine = ed.getSelectionStartLine();
            stopLine = ed.getSelectionStopLine();
        }
        ed.selectLines(startLine, stopLine);
        String text = ed.getSelectedText();
        String lines[] = text.split("\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append("\t");
            out.append(line);
            out.append("\n");
        }
        ed.setSelectedText(out.toString());
        ed.selectLines(startLine, stopLine-1);
        ed.endAtomicEdit();
    }
            
    public void decreaseIndent() {
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        ed.beginAtomicEdit();
        int startLine;
        int stopLine;
        if (!ed.isSelectionActive()) {
            startLine = stopLine = ed.getCaretLineNumber();    
        } else {
            startLine = ed.getSelectionStartLine();
            stopLine = ed.getSelectionStopLine();
        }
        ed.selectLines(startLine, stopLine);
        String text = ed.getSelectedText();
        String lines[] = text.split("\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("\t")) {
                line = line.substring(1);
            } else if (line.startsWith("  ")) {
                line = line.substring(2);
            } 
            out.append(line);
            out.append("\n");
        }
        ed.setSelectedText(out.toString());
        ed.selectLines(startLine, stopLine-1);
        ed.endAtomicEdit();
    }
            
    public void handleRun() {
        status.progress(Translate.t("Compiling sketch..."));

        if (Base.preferences.getBoolean("console.auto_clear")) {
            console.clear();
        }

        new Thread(runHandler, "Compiler").start();
    }

    class DefaultRunHandler implements Runnable {
        public void run() {
            try {
                if(sketch.build()) {
                    reportSize();
                }
            } catch (Exception e) {
                status.unprogress();
                Base.error(e);
            }

            status.unprogress();
        }
    }

    protected boolean checkModified() {
        if (!sketch.isModified()) return true;

        String prompt = Translate.t("Save changes to %1?", sketch.getName());

        if (!Base.isMacOS()) {
            int result =
                JOptionPane.showConfirmDialog(this, prompt, Translate.t("Close"),
                                              JOptionPane.YES_NO_CANCEL_OPTION,
                                              JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                return handleSave();
            } else if (result == JOptionPane.NO_OPTION) {
                return true;
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            } else {
                return false;
            }
        } else {
            JOptionPane pane = new JOptionPane("<html> " +
                    "<head> <style type=\"text/css\">"+
                    "b { font: 13pt \"Lucida Grande\" }"+
                    "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                    "</style> </head>" +
                    "<b>" + Translate.t("Do you want to save changes to this sketch before closing?") + "</b>" +
                    "<p>" + Translate.t("If you don't save, your changes will be lost."),
                    JOptionPane.QUESTION_MESSAGE);

            String[] options = new String[] {
                Translate.t("Save"), Translate.t("Cancel"), Translate.t("Don't Save")
            };
            pane.setOptions(options);
            pane.setInitialValue(options[0]);
            pane.putClientProperty("Quaqua.OptionPane.destructiveOption", new Integer(2));

            JDialog dialog = pane.createDialog(this, null);
            dialog.setVisible(true);

            Object result = pane.getValue();
            if (result == options[0]) {
                return handleSave();
            } else if (result == options[2]) {
                return true;
            } else {
                return false;
            }
        }
    }

    public SketchEditor addTab(File file) {
        String fileName = file.getName();
        SketchEditor newEditor = new SketchEditor(file);
        if (sketch != null) {
            if (fileName.endsWith(".ino") || fileName.endsWith(".pde")) {
                createExtraTokens(newEditor);
            }
        }
        tabs.add(fileName, newEditor);
        return newEditor;
    }

    public boolean handleSave() {
        if (sketch.isUntitled() || sketch.isReadOnly()) {
            boolean ret = handleSaveAs();
            return ret;
        } else {
            boolean ret = handleSave2();
            return ret;
        }
    }

    protected boolean handleSave2() {
        statusNotice(Translate.t("Saving..."));
        boolean saved = false;
        try {
            saved = sketch.save();
            if (saved) {
                statusNotice(Translate.t("Done Saving."));
                Base.updateMRU(sketch.getFolder());
            } else {
                statusEmpty();
            }
        } catch (Exception e) {
            Base.error(e);
        }
        return saved;
    }

    public boolean handleSaveAs() {
        statusNotice(Translate.t("Saving..."));
        try {
            if (sketch.saveAs()) {
                statusNotice(Translate.t("Done Saving."));
                sketch.cleanBuild();
            } else {
                statusNotice(Translate.t("Save Canceled."));
                return false;
            }
        } catch (Exception e) {
            Base.error(e);
        }

        Base.updateMRU(sketch.getFolder());
        return true;
    }
  
  
    public boolean serialPrompt() {
        int count = serialMenu.getItemCount();
        Object[] names = new Object[count];
        for (int i = 0; i < count; i++) {
            names[i] = ((JCheckBoxMenuItem)serialMenu.getItem(i)).getText();
        }

        String result = (String)
        JOptionPane.showInputDialog(this,
            Translate.w("Serial port %1 not found. Retry the upload with another serial port?", 30, "\n", serialPort),
            Translate.t("Serial port not found"),
            JOptionPane.PLAIN_MESSAGE,
            null,
            names,
            0);
        if (result == null) return false;
        selectSerialPort(result);
        return true;
    }


    synchronized public void handleExport() {
        console.clear();
        status.progress(Translate.t("Starting Upload..."));

        new Thread(exportHandler, "Uploader").start();
    }

    class DefaultExportHandler implements Runnable {
        public void run() {
            boolean success = sketch.upload();
            if (success) {
                reportSize();
            }
            status.unprogress();
        }
    }

    public void handlePageSetup() {
        if (printerJob == null) {
            printerJob = PrinterJob.getPrinterJob();
        }
        if (pageFormat == null) {
            pageFormat = printerJob.defaultPage();
        }
        pageFormat = printerJob.pageDialog(pageFormat);
    }

    public void handlePrint() {
        JTextArea tempBuffer = new JTextArea();

        MessageFormat header = new MessageFormat(getSelectedTabName());
        MessageFormat footer = new MessageFormat("Page {0}");

        tempBuffer.setText(getText());
        tempBuffer.setTabSize(4);
        tempBuffer.setFont(new Font("Monospaced", Font.PLAIN, 6));
        try {
            tempBuffer.print(header, footer);
        } catch (Exception e) {
            Base.error(e);
        }
    }


    public void statusError(Exception e) {
        Base.error(e);
    }
    public void statusError(String what) {
        status.error(what);
    }

  /**
   * Show a notice message in the editor status bar.
   */
  public void statusNotice(String msg) {
    status.notice(msg);
  }


  /**
   * Clear the status area.
   */
  public void statusEmpty() {
    statusNotice(EMPTY);
  }

    public void importLibrary(String name) {
        String[] bits = name.split("::");
        HashMap<String, Library>libs = Base.getLibraryCollection(bits[0]);
        if (libs == null) {
            return;
        }
        Library lib = libs.get(bits[1]);
        if (lib == null) {
            return;
        }
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        ed.scrollTo(0);
        ed.beginAtomicEdit();
        ed.setCaretPosition(0);
        ed.insert(lib.getInclude());
        ed.endAtomicEdit();
    }

    public void populateMenus() {
        rebuildExamplesMenu();
        rebuildImportMenu();
        rebuildHelpMenu();
        rebuildOptionsMenu();
    }

    public void rebuildImportMenu() {
        if (importMenu == null) return;
        importMenu.removeAll();

        JMenuItem item = new JMenuItem(Translate.t("Add Library..."));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleAddLibrary();
            }
        });
        importMenu.add(item);
  
        item = new JMenuItem(Translate.t("Rescan Libraries"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.gatherLibraries();
                rebuildImportMenu();
            }
        });
        importMenu.add(item);
  
        importMenu.addSeparator();


        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importLibrary(e.getActionCommand());
            }
        };

        HashMap<String, Library> globalLibraries = Base.getLibraryCollection("global");
        if (globalLibraries != null) {
            if (globalLibraries.size() > 0) {
                JMenu globalMenu = new JMenu(Translate.t("Standard"));
                String[] entries = (String[]) globalLibraries.keySet().toArray(new String[0]);
                for (String entry : entries) {
                    item = new JMenuItem(entry);
                    item.addActionListener(listener);
                    item.setActionCommand("global::" + entry);
                    globalMenu.add(item);
                }
                importMenu.add(globalMenu);
            }
        }

        if (core != null) {
            HashMap<String, Library> coreLibraries = Base.getLibraryCollection(core.getName());
            if (coreLibraries != null) {
                if (coreLibraries.size() > 0) {
                    JMenu coreMenu = new JMenu(core.getName());
                    String[] entries = (String[]) coreLibraries.keySet().toArray(new String[0]);
                    for (String entry : entries) {
                        item = new JMenuItem(entry);
                        item.addActionListener(listener);
                        item.setActionCommand(core.getName() + "::" + entry);
                        coreMenu.add(item);
                    }
                    importMenu.add(coreMenu);
                }
            }
        }

        HashMap<String, Library> contributedLibraries = Base.getLibraryCollection("sketchbook");
        if (contributedLibraries != null) {
            if (contributedLibraries.size() > 0) {
                int menuSize = 0;
                JMenu contributedMenu = new JMenu(Translate.t("Contributed"));
                JMenu addTo = contributedMenu;
                String[] entries = (String[]) contributedLibraries.keySet().toArray(new String[0]);
                Arrays.sort(entries);
                for (String entry : entries) {
                    item = new JMenuItem(entry);
                    item.addActionListener(listener);
                    item.setActionCommand("sketchbook::" + entry);
                    addTo.add(item);
                    menuSize++;
                    if (menuSize == 20) {
                        JMenu newMenu = new JMenu(Translate.t("More..."));
                        addTo.add(newMenu);
                        addTo = newMenu;
                        menuSize = 0;
                    }
                }
                importMenu.add(contributedMenu);
            }
        }
    }

    public void rebuildExamplesMenu() {

        if (examplesMenu == null) return;
        examplesMenu.removeAll();
//        addSketches(examplesMenu, Base.getExamplesFolder());

        if (core != null) {
            File coreExamples = core.getExamplesFolder();
            if (coreExamples != null) {
                if (coreExamples.isDirectory()) {
                    addSketches(examplesMenu, coreExamples);
                    examplesMenu.addSeparator();
                }
            }

            JMenu coreItem = new JMenu(core.get("name", core.getName()));
            examplesMenu.add(coreItem);
            File coreLibs = core.getLibraryFolder();
            if (coreLibs != null) {
                if (coreLibs.isDirectory()) {
                    addSketches(coreItem, coreLibs);
                }
            }
        }

        if (board != null) {
            File boardExamples = board.getExamplesFolder();
            if (boardExamples != null) {
                if (boardExamples.isDirectory()) {
                    JMenu boardItem = new JMenu(board.getLongName());
                    addSketches(boardItem, boardExamples);
                    if (boardItem.getMenuComponentCount() > 0) {
                        examplesMenu.add(boardItem);
                    }
                }
            }
        }

        JMenu contributedItem = new JMenu(Translate.t("Contributed"));
        examplesMenu.add(contributedItem);

        File sbLibs = new File(Base.getSketchbookFolder(),"libraries");
        if (sbLibs.isDirectory()) {
            addSketches(contributedItem, sbLibs);
        }
    }

    protected boolean addSketches(JMenu menu, File folder) {
        if (folder == null) return false;
        if (!folder.isDirectory()) return false;

        String[] list = folder.list();
        if (list == null) return false;

        Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String path = e.getActionCommand();
                if (new File(path).exists()) {
                    Base.createNewEditor(path);
                } else {
                    Base.showWarning(Translate.t("Sketch Does Not Exist"),
                                Translate.t("The selected sketch no longer exists.") + "\n" +
                                Translate.t("You may need to restart to update the sketchbook menu."), null);
                }
            }
        };

        boolean ifound = false;

        for (int i = 0; i < list.length; i++) {
            if ((list[i].charAt(0) == '.') || list[i].equals("CVS"))
                continue;

            File subfolder = new File(folder, list[i]);
            if (!subfolder.isDirectory())
                continue;

            File entry = new File(subfolder, list[i] + ".ino");
            if (!entry.exists() && (new File(subfolder, list[i] + ".pde")).exists()) {
                entry = new File(subfolder, list[i] + ".pde");
            }
           // if a .pde file of the same prefix as the folder exists..
            if (entry.exists()) {
                if (!Sketch.isSanitaryName(list[i])) {
                    String complaining =
                        Translate.t("The sketch %1 cannot be used.") + "\n" +
                        Translate.t("Sketch names must contain only basic letters and numbers") + "\n" +
                        Translate.t("(ASCII-only with no spaces, and it cannot start with a number).") + "\n" +
                        Translate.t("To get rid of this message, remove the sketch from") + "\n" +
                        entry.getAbsolutePath();
                    Base.showMessage(Translate.t("Ignoring sketch with bad name"), complaining);
                    continue;
                }

                JMenuItem item = new JMenuItem(list[i]);
                item.addActionListener(listener);
                item.setActionCommand(entry.getAbsolutePath());
                menu.add(item);
                ifound = true;

            } else {
                // don't create an extra menu level for a folder named "examples"
                if (subfolder.getName().equals("examples")) {
                    boolean found = addSketches(menu, subfolder);
                    if (found)
                        ifound = true;
                } else {
                    // not a sketch folder, but maybe a subfolder containing sketches
                    JMenu submenu = new JMenu(list[i]);
                    // needs to be separate var
                    // otherwise would set ifound to false
                    boolean found = addSketches(submenu, subfolder);
                    //boolean found = addSketches(submenu, subfolder);
                    if (found) {
                        menu.add(submenu);
                        ifound = true;
                    }
                }
            }
        }
        return ifound;  // actually ignored, but..
    }

    public void rebuildBootloadersMenu() {
        bootloadersMenu.removeAll();
        if (sketch == null) {
            return;
        }

        if (board == null) {
            return;
        }

        if (core == null) {
            return;
        }

        if (board.getBootloader() == null) {
            JMenuItem nbl = new JMenuItem("No Bootloader Available");
            nbl.setEnabled(false);
            bootloadersMenu.add(nbl);
            return;
        }

        HashMap<String, String> all = sketch.mergeAllProperties();

        String plist = all.get("bootloader.upload");
        if (plist == null) {
            JMenuItem nbl = new JMenuItem("No Programmer Available");
            nbl.setEnabled(false);
            bootloadersMenu.add(nbl);
            return;
        }
            
        String[] parr = plist.split("::");

        for (String progEntry : parr) {

            if (!isValidProgrammer(progEntry)) {
                continue;
            }
            String name = all.get("upload." + progEntry + ".name");
            AbstractAction action = new AbstractAction(name) {
                public void actionPerformed(ActionEvent actionevent) {
                    sketch.programBootloader((String) getValue("programmer"));
                }
            };
            action.putValue("programmer", progEntry);
            JMenuItem item = new JMenuItem(action);
            bootloadersMenu.add(item);
        }
    }
        

    public void rebuildProgrammersMenu() {
        programmersMenu.removeAll();
        if (sketch == null) {
            return;
        }

        HashMap<String, String> all = sketch.mergeAllProperties();

        String plist = all.get("sketch.upload");

        if (plist == null) {
            return;
        }

        String[] parr = plist.split("::");

        ButtonGroup group = new ButtonGroup();

        for (String progEntry : parr) {

            if (!isValidProgrammer(progEntry)) {
                continue;
            }

            String name = all.get("upload." + progEntry + ".name");
            AbstractAction action = new AbstractAction(name) {
                public void actionPerformed(ActionEvent actionevent) {
                    selectProgrammer((String) getValue("programmer"));
                }
            };
            action.putValue("programmer", progEntry);
            JMenuItem item = new JRadioButtonMenuItem(action);
            if (progEntry.equals(programmer)) {
                item.setSelected(true);
            }
            programmersMenu.add(item);
        }
    }

    public void rebuildBoardsMenu() {

        boardsMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        HashMap<String, JMenu> groupings;
        groupings = new HashMap<String, JMenu>();

        HashMap<String, TreeSet<Board>> blist = new HashMap<String, TreeSet<Board>>();
        ArrayList<String> groupNames = new ArrayList<String>();

        for (String thisBoard : Base.boards.keySet()) {
            Board b = Base.boards.get(thisBoard);
            String thisGroup = b.getGroup();
            if (blist.get(thisGroup) == null) {
                blist.put(thisGroup, new TreeSet<Board>());
                groupNames.add(thisGroup);
            }
            blist.get(thisGroup).add(b);
        }

        Collections.sort(groupNames);

        for (String groupName : groupNames) {
            JMenu groupMenu = new JMenu(groupName);
            boardsMenu.add(groupMenu);

            for (Board b : blist.get(groupName)) {
                AbstractAction action = new AbstractAction(b.getLongName()) {
                    public void actionPerformed(ActionEvent actionevent) {
                        selectBoard((String) getValue("board"));
                    }
                };
                action.putValue("board", b.getName());
                JMenuItem boardMenu = new JRadioButtonMenuItem(action);
                if (board != null) {
                    if (b.compareTo(board) == 0) {
                        boardMenu.setSelected(true);
                    }
                }
                groupMenu.add(boardMenu);
            }
        }
    }

    public void selectProgrammer(String p) {
        programmer = p;
        if (board != null) {
            Base.preferences.set("sketch.upload." + board.getName(), p);
        }
        updateLineStatus();
    }
    
    public void selectBoard(String b) {
        if (b == null) {
            selectBoard((Board)null);
            return;
        }
        Board brd = Base.boards.get(b);
        if (brd == null) {
            Base.showWarning(Translate.t("Invalid Board"),
                Translate.t("Unable to locate board '%1'", b),
                null
            );
            return;
        }
        selectBoard(brd);
    }

    public void updateLineStatus() {
        String boardName;
        String coreName;
        String portName;
        String programmerName;

        if (board != null) {
            boardName = board.getLongName();
        } else {
            boardName = "No board";
        }

        if (core != null) {
            coreName = core.getName();
        } else {
            coreName = "no core";
        }

        if (programmer != null) {
            programmerName = programmer;
        } else {
            programmerName = "no programmer";
        }

        portName = serialPort;

        lineStatus.setText(boardName + " (" + coreName + ") on " + portName + " using " + programmerName);
    }

    public void selectBoard(Board b) {
        if (b == null) {
            board = null;
            core = null;
            compiler = null;
            lineStatus.setText("No board on " + serialPort);
            return; 
        }

        board = b;

        String pc = Base.preferences.get("core." + board.getName());
        if (pc != null && pc != "") {
            core = Base.cores.get(pc);
        } else {
            core = null;
            String defaultCore = board.get("core");
            if (defaultCore != null) {
                core = Base.cores.get(defaultCore);
            } else {
                String entries[] = Base.cores.keySet().toArray(new String[0]);
                for (String entry : entries) {
                    if (Base.cores.get(entry).getFamily().equals(board.getFamily())) {
                        core = Base.cores.get(entry);
                        Base.preferences.set("core." + board.getName(), entry);
                        break;
                    }
                }
            }
        }

        if (core != null) {
            compiler = core.getCompiler();
        } else {
            compiler = null;
        }
        sketch.settings.put("board.root", board.getFolder().getAbsolutePath());
        Base.preferences.set("board", b.getName());
        rebuildBoardsMenu();
        rebuildCoresMenu();
        populateMenus();


        programmer = Base.preferences.get("sketch.upload." + board.getName());

        if (programmer == null) {
            HashMap<String, String> all = sketch.mergeAllProperties();
            String pstr = all.get("sketch.upload");
            if (pstr != null) { 
                String[] parr = pstr.split("::");
                programmer = parr[0];
            }
        }

        if (programmer != null) {
            if (!isValidProgrammer(programmer)) {
                programmer = null;
            }
        }

        rebuildProgrammersMenu();
        rebuildBootloadersMenu();

        for (int i = 0; i < tabs.getTabCount(); i++) {
            SketchEditor se = (SketchEditor)tabs.getComponentAt(i);
            String fileName = se.getFile().getName();

            if (fileName.endsWith(".ino") || fileName.endsWith(".pde")) {
                createExtraTokens((SketchEditor)tabs.getComponentAt(i));
            }
        }
        updateLineStatus();
    }

    public boolean isValidProgrammer(String progEntry) {
        HashMap<String, String> all = sketch.mergeAllProperties();
        String name = all.get("upload." + progEntry + ".name");
        String osCommand = all.get("upload." + progEntry + ".command." + Base.getOSName());
        String osArchCommand = all.get("upload." + progEntry + ".command." + Base.getOSFullName());
        String genericCommand = all.get("upload." + progEntry + ".command");
        String javaCommand = all.get("upload." + progEntry + ".command.java");

        if (name == null) {
            return false;
        }

        if (osCommand == null && osArchCommand == null && genericCommand == null && javaCommand == null) {
            return false;
        }

        return true;
    }

    public void rebuildCoresMenu()
    {
        ButtonGroup group = new ButtonGroup();
        coresMenu.removeAll();
        JMenuItem item;

        String[] entries = (String[]) Base.cores.keySet().toArray(new String[0]);

        if (board != null) {
            for (int i = 0; i < entries.length; i++) {
                Core c = Base.cores.get(entries[i]);
                String fam = c.getFamily();
                if (fam != null) {
                    if (fam.equals(board.getFamily())) {
                        item = new JRadioButtonMenuItem(c.get("name", entries[i]) + " (v" + c.getFullVersion() + ")");
                        item.setActionCommand(entries[i]);
                        if (c == core) {
                            item.setSelected(true);
                        }
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                selectCore(e.getActionCommand());
                            }
                        });
                        group.add(item);
                        coresMenu.add(item);
                    }
                }
            }
        }
    }

    public void selectCore(String c) {
        core = Base.cores.get(c);
        compiler = core.getCompiler();
        Base.preferences.set("core." + board.getName(), c);
        updateLineStatus();
//        rebuildBoardsMenu();
//        rebuildCoresMenu();
        rebuildProgrammersMenu();
        rebuildBootloadersMenu();
        populateMenus();
    }

    public void rebuildPluginsMenu()
    {
        toolsMenu.removeAll();
        JMenuItem item;

        addPluginsToMenu(toolsMenu, BasePlugin.MENU_PLUGIN_TOP);
        toolsMenu.addSeparator();
        addPluginsToMenu(toolsMenu, BasePlugin.MENU_PLUGIN_MAIN);
    }

    public void addPluginsToMenu(JMenu menu, int filterFlags) {
        JMenuItem item;
        String[] entries = (String[]) Base.plugins.keySet().toArray(new String[0]);

        HashMap<String, JMenuItem> menus = new HashMap<String, JMenuItem>();
        for (int i=0; i<entries.length; i++) {
            final Plugin t = Base.plugins.get(entries[i]);
            int flags = 0;
            try {
                flags = t.flags();
            } catch (Exception e) {
                flags = 0;
            }
            if ((flags & filterFlags) == filterFlags) {
                item = new JMenuItem(t.getMenuTitle());
                final Editor me = this;
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        launchPlugin(t);
                    }
                });
                try {
                    if (t.getShortcut() != 0) {
                        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                        modifiers |= t.getModifier();
                        item.setAccelerator(KeyStroke.getKeyStroke(t.getShortcut(), modifiers));
                    }
                } catch (Exception ignored) {};

                menus.put(t.getMenuTitle(), item);
            }
        }

        entries = (String[]) menus.keySet().toArray(new String[0]);
        Arrays.sort(entries);
        for (String entry : entries) {
            menu.add(menus.get(entry));
        }
    }

    public void rebuildMRUMenu() {
        sketchbookMenu.removeAll();

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String path = e.getActionCommand();
                if (new File(path).exists()) {
                    Base.createNewEditor(path);
                } else {
                    Base.showWarning("Sketch Does Not Exist",
                                "The selected sketch no longer exists.\n" +
                                "You may need to restart " + Base.theme.get("product.cap") + " to update\n" +
                                "the sketchbook menu.", null);
                }
            }
        };

        JMenuItem item;

        for (File m : Base.MRUList) {
            item = new JMenuItem(m.getName());
            item.addActionListener(listener);
            item.setActionCommand(m.getAbsolutePath());
            sketchbookMenu.add(item);
        }
    }

    public void nameCode(String name) {
        SketchFile s = sketch.getCodeByEditor((SketchEditor)tabs.getSelectedComponent());
        s.nameCode(name);
    }


    public void handleClose() {

        if (sketch.isModified()) {

            Object[] options = { Translate.t("Yes"), Translate.t("No"), Translate.t("Cancel") };
            String prompt = "Save changes to " + sketch.getName() + "?";
            int result = JOptionPane.showOptionDialog(this, prompt, "Save Changes",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null, options, options[0]);
            if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }

            if (result == JOptionPane.YES_OPTION) {
                if (!handleSave()) {
                    return;
                }
            }
        }

        System.gc();

        if (sketch.isUntitled()) {
            Base.removeDir(sketch.folder);
        }

        Base.removeDir(sketch.buildFolder);

        Dimension d = getSize();
        Base.preferences.setInteger("editor.window.width", d.width);
        Base.preferences.setInteger("editor.window.height", d.height);
        Point p = getLocation();
        Base.preferences.setInteger("editor.window.x", p.x);
        Base.preferences.setInteger("editor.window.y", p.y);

        Base.preferences.save();

        Base.handleClose(this);
    }

    public void handleStatusOk(int function, String data) {
        switch (function) {
            case ADD_NEW_FILE:
                if (!(
                    data.endsWith(".ino") || 
                    data.endsWith(".pde") || 
                    data.endsWith(".cpp") || 
                    data.endsWith(".c") || 
                    data.endsWith(".hh") || 
                    data.endsWith(".h") || 
                    data.endsWith(".S")
                )) {
                    Base.showWarning(Translate.t("Error Adding File"),Translate.w("Error: you can only add .ino, .pde, .c, .cpp, .h, .hh or .S files to a sketch", 40, "\n"), null);
                    return;
                }
                sketch.createBlankFile(data);
                File f = new File(sketch.getFolder(), data);
                if (!f.exists()) {
                    Base.showWarning(Translate.t("Error Adding File"),Translate.t("Error: could not create file"), null);
                    return;
                }
                addTab(f);
                break;
        }
    }

    public void message(String m) {
        sketch.message(m);
    }

    public void message(String m, int c) {
        sketch.message(m, c);
    }

    public void createExtraTokens(SketchEditor se) {
        RSyntaxTextArea ta = se.textArea;
        HashMap<String, Library> allLibraries = Base.getLibraryCollection("global");
        if (core != null) {
            allLibraries.putAll(Base.getLibraryCollection(core.getName()));
        }
        allLibraries.putAll(Base.getLibraryCollection("sketchbook"));

        ArduinoTokenMaker tm = (ArduinoTokenMaker)((RSyntaxDocument)ta.getDocument()).getSyntaxStyle();

        tm.clear();

        loadKeywordsFromFile(tm, Base.getContentFile("lib/keywords.txt"));
        if (core != null) {
            loadKeywordsFromFile(tm, new File(core.getFolder(), "keywords.txt"));
        }
        if (board != null) {
            loadKeywordsFromFile(tm, new File(board.getFolder(), "keywords.txt"));
        }

        for (Library lib : allLibraries.values()) {
            File libFolder = lib.getFolder();
            File keywords = new File(libFolder, "keywords.txt");
            if (keywords.exists()) {
                loadKeywordsFromFile(tm, keywords);
            }
        }
    }

    public void loadKeywordsFromFile(ArduinoTokenMaker tm, File keywords) {
        if (!keywords.exists()) {
            return;
        }
        try {
            FileInputStream fis;
            BufferedReader br;
            String line;

            fis = new FileInputStream(keywords);
            br = new BufferedReader(new InputStreamReader(fis, Charset
.forName("UTF-8")));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) continue;
                if (line.startsWith("//")) continue;
                line = line.replaceAll("\\s+", " ");
                String[] kv = line.split(" ");
                if (kv.length >= 2) {
                    String keyword = kv[0];
                    String type = kv[1];
                    if (type.equals("KEYWORD1")) {
                        tm.addKeyword(keyword, TokenTypes.RESERVED_WORD);
                    }
                    if (type.equals("KEYWORD2")) {
                        tm.addKeyword(keyword, TokenTypes.FUNCTION);
                    }
                    if (type.equals("KEYWORD3")) {
                        tm.addKeyword(keyword, TokenTypes.RESERVED_WORD);
                    }
                    if (type.equals("LITERAL1")) {
                        tm.addKeyword(keyword, TokenTypes.PREPROCESSOR);
                    }
                    if (type.equals("LITERAL2")) {
                        tm.addKeyword(keyword, TokenTypes.IDENTIFIER);
                    }
                }
            }
            br.close();
        } catch (Exception e) {
        }
    }

    public void openInternal(String path) {
        if (path == null) {
            return;
        }
        tabs.removeAll();
        sketch = new Sketch(this, new File(path));
        sketch.checkForSettings();
        rebuildImportMenu();
    }

    public String getTabName(int i) {
        return tabs.getTitleAt(i);
    }

    public String getSelectedTabName() {
        return getTabName(tabs.getSelectedIndex());
    }

    public void setTabName(int i, String name) {
        tabs.setTitleAt(i, name);
    }

    public int getTabByFile(SketchFile f) {
        return tabs.indexOfComponent(f.textArea);
    }

    public void selectTab(int tab) {
        tabs.setSelectedIndex(tab);
    }

    public SketchEditor getActiveTab() {
        return (SketchEditor) tabs.getSelectedComponent();
    }

    public void reportSize() {
        long maxFlash = 0;
        long maxRam = 0;
        long text = 0;
        long data = 0;
        long bss = 0;
        try {
            maxFlash = Integer.parseInt(board.get("upload.maximum_size", "0"));
        } catch (Exception e) {
            maxFlash = 0;
        }

        try {
            maxRam = Integer.parseInt(board.get("upload.maximum_ram", "0"));
        } catch (Exception e) {
            maxRam = 0;
        }

        long flashUsed = 0;
        long ramUsed = 0;
    
        try {
            Sizer sizer = new Sizer(this);
            sizer.computeSize();
            
            flashUsed = sizer.progSize();
            ramUsed = sizer.ramSize();
            text = sizer.textSize();
            data = sizer.dataSize();
            bss = sizer.bssSize();
            
        } catch (Exception e) {
            flashUsed = 0;
            ramUsed = 0;
        }

        message("Program Size:\n", 1);

        if (maxFlash > 0) {
            int flashPercent = (int)(flashUsed * 100 / maxFlash);
            message("  Flash: " + flashPercent + "% (" + flashUsed + " bytes out of " + maxFlash + " bytes max)\n", 1);
        } else {
            message("  Flash: " + flashUsed + " bytes\n", 1);
        }

        if (maxRam > 0) {
            int ramPercent = (int)(ramUsed * 100 / maxRam);
            message("    RAM: " + ramPercent + "% (" + ramUsed + " bytes out of " + maxRam + " bytes max)\n", 1);
        } else {
            message("    RAM: " + ramUsed + " bytes\n", 1);
        }
        if (Base.preferences.getBoolean("compiler.verbose")) {
            message("         (text: " + text + ", data: " + data + ", bss: " + bss + ")\n", 1);
        }
    }
    
    public void launchPlugin(Plugin p) {
        try {
            Plugin instance = p.getClass().newInstance();
            Base.pluginInstances.add(instance);
            System.gc();
            instance.setLoader(p.getLoader());
            instance.init(this);
            SwingUtilities.invokeLater(instance);
        } catch (Exception e) {
            message("Error launching plugin: " + e.getMessage() + "\n", 2);
        }
    }

    public void grabSerialPort() {
        try {
            Class[] cArg = new Class[1];
            cArg[0] = new String().getClass();

            for (Plugin p : Base.pluginInstances) {
                Method m = p.getClass().getMethod("releasePort", cArg);
                if (m != null) {
                    m.invoke(p, serialPort);
                }
            }
        } catch (Exception e) {
            message("Error grabbing serial port: " + e.getMessage() + "\n", 2);
        }
    }

    public void releaseSerialPort() {
        try {
            Class[] cArg = new Class[1];
            cArg[0] = new String().getClass();

            for (Plugin p : Base.pluginInstances) {
                Method m = p.getClass().getMethod("obtainPort", cArg);
                if (m != null) {
                    m.invoke(p, serialPort);
                }
            }
        } catch (Exception e) {
            message("Error releasing serial port: " + e.getMessage() + "\n", 2);
        }
    }
}

