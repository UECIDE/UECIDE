/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-09 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package uecide.app;

import uecide.plugin.*;
import uecide.app.debug.*;
import processing.core.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.JToolBar;
import java.nio.charset.*;


import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import gnu.io.*;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.modes.ArduinoTokenMaker;

/**
 * Main editor panel for the Processing Development Environment.
 */
@SuppressWarnings("serial")
public class Editor extends JFrame implements RunnerListener {

static Logger logger = Logger.getLogger(Base.class.getName());

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

    /**
    * true if this file has not yet been given a name by the user
    */
    PageFormat pageFormat;
    PrinterJob printerJob;

    // file, sketch, and tools menus for re-inserting items

    FindAndReplace findAndReplace;
    JMenu fileMenu;
    JMenu sketchMenu;
    JMenu hardwareMenu;
    JMenu toolsMenu;

    JLabel lineStatus;

    JToolBar toolbar;
    JMenu sketchbookMenu;
    JMenu examplesMenu;
    JMenu importMenu;

    JMenu boardsMenu;
    JMenu coresMenu;
    JMenu serialMenu;

    static SerialMenuListener serialMenuListener;
  
    //EditorHeader header;
    public EditorStatus status;
    EditorConsole console;

    JSplitPane splitPane;
    JPanel consolePanel;

    // currently opened program
    public Sketch sketch;

    // runtime information and window placement

    JMenuItem exportAppItem;
    JMenuItem saveMenuItem;
    JMenuItem saveAsMenuItem;

    boolean running;
    boolean uploading;

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

    public Board board = null;
    public Core core = null;

    static final int ADD_NEW_FILE = 1;
    static final int RENAME_FILE = 2;

    public void setConsoleFont(Font f) {
        console.setFont(f);
    }

    public Editor(String path) {
        super(Theme.get("product"));

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
        toolbar.setBackground(Theme.getColor("buttons.bgcolor"));
        toolbar.setFloatable(false);

        File themeFolder = Base.getContentFile("lib/theme");
        if (!themeFolder.exists()) {
            logger.debug("PANIC: Theme folder doesn't exist! " + themeFolder.getAbsolutePath());
            return;
        }

        File runIconFile = new File(themeFolder, "run.png");
        ImageIcon runButtonIcon = new ImageIcon(runIconFile.getAbsolutePath());

        runButton = new JButton(runButtonIcon);
        runButton.setToolTipText(Translate.t("Verify"));
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                    handleRun(true);
                } else {
                    handleRun(false);
                }
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
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                    handleExport(true);
                } else {
                    handleExport(false);
                }
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
                            t.init(me);
                            SwingUtilities.invokeLater(t);
                        }
                    });
                    toolbar.add(button);
                }
            } catch(Exception e) {
                e.printStackTrace();
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
        int dividerSize = Preferences.getInteger("editor.divider.size");
        if (dividerSize != 0) {
            splitPane.setDividerSize(dividerSize);
        }

        splitPane.setMinimumSize(new Dimension(600, 400));
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

        // All set, now show the window
        setSize(new Dimension(350, 550));
        setPreferredSize(new Dimension(350, 550));
        setMinimumSize(new Dimension(
            Preferences.getInteger("editor.window.width.min"),
            Preferences.getInteger("editor.window.height.min")
        ));
    
        if (Base.activeEditor != null) {
            Dimension oSize = Base.activeEditor.getSize();
            setSize(oSize.width, oSize.height);
            Point oPos = Base.activeEditor.getLocation();
            setLocation(oPos.x + 20, oPos.y + 20);
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
          String[] pieces = PApplet.splitTokens(data, "\r\n");
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
        e.printStackTrace();
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


  protected void setPlacement(int[] location) {
    setBounds(location[0], location[1], location[2], location[3]);
    if (location[4] != 0) {
      splitPane.setDividerLocation(location[4]);
    }
  }


  protected int[] getPlacement() {
    int[] location = new int[5];

    // Get the dimensions of the Frame
    Rectangle bounds = getBounds();
    location[0] = bounds.x;
    location[1] = bounds.y;
    location[2] = bounds.width;
    location[3] = bounds.height;

    // Get the current placement of the divider
    location[4] = splitPane.getDividerLocation();

    return location;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Read and apply new values from the preferences, either because
   * the app is just starting up, or the user just finished messing
   * with things in the Preferences window.
   */
  protected void applyPreferences() {

    // apply the setting for 'use external editor'
    boolean external = Preferences.getBoolean("editor.external");

    saveMenuItem.setEnabled(!external);
    saveAsMenuItem.setEnabled(!external);

    for (int i = 0; i < tabs.getTabCount(); i++) {
        SketchEditor ed = (SketchEditor) tabs.getComponentAt(i);
        ed.setEditable(!external);
        ed.setBackground( external ?
            Theme.getColor("editor.external.bgcolor") :
            Theme.getColor("editor.bgcolor") 
        );

        ed.setFont(Preferences.getFont("editor.font"));
        console.setFont(Preferences.getFont("console.font"));
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

    item = newJMenuItem(Translate.t("Upload to I/O Board"), 'U');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleExport(false);
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
          handleRun(false);
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

    item = newJMenuItemShift(Translate.t("New File..."), 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleNewFile();
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
    
    if (serialMenuListener == null)
      serialMenuListener  = new SerialMenuListener();
    if (serialMenu == null)
      serialMenu = new JMenu(Translate.t("Serial Port"));
    populateSerialMenu();
    menu.add(serialMenu);
    
    menu.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent e) {}
      public void menuDeselected(MenuEvent e) {}
      public void menuSelected(MenuEvent e) {
        populateSerialMenu();
      }
    });
    return menu;
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
    Preferences.set("serial.port", name);
    lineStatus.setText(board.getLongName() + " on " + Preferences.get("serial.port"));
  }


  protected void populateSerialMenu() {
    // getting list of ports

    JMenuItem rbMenuItem;
    
    serialMenu.removeAll();
    boolean empty = true;

    try
    {
      for (Enumeration enumeration = CommPortIdentifier.getPortIdentifiers(); enumeration.hasMoreElements();)
      {
        CommPortIdentifier commportidentifier = (CommPortIdentifier)enumeration.nextElement();
        if (commportidentifier.getPortType() == CommPortIdentifier.PORT_SERIAL)
        {
          String curr_port = commportidentifier.getName();
          rbMenuItem = new JCheckBoxMenuItem(curr_port, curr_port.equals(Preferences.get("serial.port")));
          rbMenuItem.addActionListener(serialMenuListener);
          //serialGroup.add(rbMenuItem);
          serialMenu.add(rbMenuItem);
          empty = false;
        }
      }
      if (!empty) {
        serialMenu.setEnabled(true);
      }

    }

    catch (Exception exception)
    {
      message(Translate.t("Error retrieving port list") + "\n", 2);
      exception.printStackTrace();
    }
	
    if (serialMenu.getItemCount() == 0) {
      serialMenu.setEnabled(false);
    }

    //serialMenu.addSeparator();
    //serialMenu.add(item);
  }


  protected JMenu buildHelpMenu() {
    // To deal with a Mac OS X 10.5 bug, add an extra space after the name
    // so that the OS doesn't try to insert its slow help menu.
    JMenu menu = new JMenu(Translate.t("Help"));
    JMenuItem item;

    if (Theme.get("links.gettingstarted.url") != null) {
        item = new JMenuItem(Translate.t("Getting Started"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Theme.get("links.gettingstarted.url"));
            }
          });
        menu.add(item);
    }

    if (Theme.get("links.environment.url") != null) {
        item = new JMenuItem(Translate.t("Environment"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Theme.get("links.environment.url"));
            }
          });
        menu.add(item);
    }

    if (Theme.get("links.troubleshooting.url") != null) {
        item = new JMenuItem(Translate.t("Troubleshooting"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Theme.get("links.troubleshooting.url"));
            }
          });
        menu.add(item);
    }


    if (Theme.get("links.reference.url") != null) {
        item = new JMenuItem(Translate.t("Reference"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Theme.get("links.reference.url"));
            }
          });
        menu.add(item);
    }

    if (Theme.get("links.faq.url") != null) {
        item = new JMenuItem(Translate.t("Frequently Asked Questions"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Theme.get("links.faq.url"));
            }
          });
        menu.add(item);
    }

    String linkName = Theme.get("links.homepage.name");
    if (linkName != null) {
        item = new JMenuItem(linkName);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Theme.get("links.homepage.url"));
            }
        });
        menu.add(item);
    }

    linkName = Theme.get("links.forums.name");
    if (linkName != null) {
        item = new JMenuItem(linkName);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Base.openURL(Theme.get("links.forums.url"));
            }
        });
        menu.add(item);
    }

    // macosx already has its own about menu
    if (!Base.isMacOS()) {
      menu.addSeparator();
      item = new JMenuItem(Translate.t("About %1", Theme.get("product.cap")));
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Base.handleAbout();
          }
        });
      menu.add(item);
    }

    item = new JMenuItem(Translate.t("System Information"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Base.handleSystemInfo();
        }
    });
    menu.add(item);

    return menu;
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

    menu.addSeparator();

    // TODO "cut" and "copy" should really only be enabled
    // if some text is currently selected
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
/*
    item = newJMenuItemShift("Copy for Forum", 'C');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
//          SwingUtilities.invokeLater(new Runnable() {
//              public void run() {
//          new DiscourseFormat(Editor.this, false).show();
//              }
//            });
        }
      });
    menu.add(item);

    item = newJMenuItemAlt("Copy as HTML", 'C');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
//          SwingUtilities.invokeLater(new Runnable() {
//              public void run() {
//          new DiscourseFormat(Editor.this, true).show();
//              }
//            });
        }
      });
    menu.add(item);
*/
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
          handleIndentOutdent(true);
        }
    });
    menu.add(item);

    item = newJMenuItem(Translate.t("Decrease Indent"), '[');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleIndentOutdent(false);
        }
    });
    menu.add(item);

    menu.addSeparator();

    item = newJMenuItem(Translate.t("Find..."), 'F');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            findAndReplace = new FindAndReplace(me);
        }
      });
    menu.add(item);

    return menu;
  }


  /**
   * A software engineer, somewhere, needs to have his abstraction
   * taken away. In some countries they jail or beat people for writing
   * the sort of API that would require a five line helper function
   * just to set the command key for a menu item.
   */
  static public JMenuItem newJMenuItem(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Like newJMenuItem() but adds shift as a modifier for the key command.
   */
  static public JMenuItem newJMenuItemShift(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    modifiers |= ActionEvent.SHIFT_MASK;
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Same as newJMenuItem(), but adds the ALT (on Linux and Windows)
   * or OPTION (on Mac OS X) key as a modifier.
   */
  static public JMenuItem newJMenuItemAlt(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    //int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    //menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, SHORTCUT_ALT_KEY_MASK));
    return menuItem;
  }



  // these will be done in a more generic way soon, more like:
  // setHandler("action name", Runnable);
  // but for the time being, working out the kinks of how many things to
  // abstract from the editor in this fashion.


  public void setHandlers(Runnable runHandler, Runnable presentHandler,
                          Runnable stopHandler,
                          Runnable exportHandler, Runnable exportAppHandler) {
    this.runHandler = runHandler;
    this.presentHandler = presentHandler;
    this.stopHandler = stopHandler;
    this.exportHandler = exportHandler;
    this.exportAppHandler = exportAppHandler;
  }


  public void resetHandlers() {
    runHandler = new DefaultRunHandler();
//-    stopHandler = new DefaultStopHandler();
    exportHandler = new DefaultExportHandler();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Gets the current sketch object.
   */
  public Sketch getSketch() {
    return sketch;
  }


  /**
   * Get the JEditTextArea object for use (not recommended). This should only
   * be used in obscure cases that really need to hack the internals of the
   * JEditTextArea. Most tools should only interface via the get/set functions
   * found in this class. This will maintain compatibility with future releases,
   * which will not use JEditTextArea.
   */
  public SketchEditor getTextArea() {
    return (SketchEditor)tabs.getSelectedComponent();
  }


  /**
   * Get the contents of the current buffer. Used by the Sketch class.
   */
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


  /**
   * Get a range of text from the current buffer.
   */
  public String getText(int start, int stop) {
    return ((SketchEditor)tabs.getSelectedComponent()).getText(start, stop - start);
  }


  /**
   * Replace the entire contents of the front-most tab.
   */
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
    // make sure that a tool isn't asking for a bad location
    SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
    start = PApplet.constrain(start, 0, ed.getDocumentLength());
    stop = PApplet.constrain(stop, 0, ed.getDocumentLength());

    ed.select(start, stop);
  }


  /**
   * Get the position (character offset) of the caret. With text selected,
   * this will be the last character actually selected, no matter the direction
   * of the selection. That is, if the user clicks and drags to select lines
   * 7 up to 4, then the caret position will be somewhere on line four.
   */
  public int getCaretOffset() {
    return ((SketchEditor)tabs.getSelectedComponent()).getCaretPosition();
  }


  /**
   * True if some text is currently selected.
   */
  public boolean isSelectionActive() {
    return ((SketchEditor)tabs.getSelectedComponent()).isSelectionActive();
  }


  /**
   * Get the beginning point of the current selection.
   */
  public int getSelectionStart() {
    return ((SketchEditor)tabs.getSelectedComponent()).getSelectionStart();
  }


  /**
   * Get the end point of the current selection.
   */
  public int getSelectionStop() {
    return ((SketchEditor)tabs.getSelectedComponent()).getSelectionStop();
  }


  /**
   * Get text for a specified line.
   */
  public String getLineText(int line) {
    return ((SketchEditor)tabs.getSelectedComponent()).getLineText(line);
  }


  /**
   * Replace the text on a specified line.
   */
  public void setLineText(int line, String what) {
    SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
    ed.beginAtomicEdit();
    ed.select(ed.getLineStartOffset(line), ed.getLineEndOffset(line));
    ed.setSelectedText(what);
    ed.endAtomicEdit();
  }


  /**
   * Get character offset for the start of a given line of text.
   */
  public int getLineStartOffset(int line) {
    return ((SketchEditor)tabs.getSelectedComponent()).getLineStartOffset(line);
  }


  /**
   * Get character offset for end of a given line of text.
   */
  public int getLineStopOffset(int line) {
    return ((SketchEditor)tabs.getSelectedComponent()).getLineEndOffset(line);
  }


  /**
   * Get the number of lines in the currently displayed buffer.
   */
  public int getLineCount() {
    return ((SketchEditor)tabs.getSelectedComponent()).getLineCount();
  }


  /**
   * Implements Edit &rarr; Cut.
   */
  public void handleCut() {
    ((SketchEditor)tabs.getSelectedComponent()).cut();
  }


  /**
   * Implements Edit &rarr; Copy.
   */
  public void handleCopy() {
    ((SketchEditor)tabs.getSelectedComponent()).copy();
  }



  /**
   * Implements Edit &rarr; Paste.
   */
  public void handlePaste() {
    ((SketchEditor)tabs.getSelectedComponent()).paste();
  }


  /**
   * Implements Edit &rarr; Select All.
   */
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
    // If the selection ends at the beginning of the last line,
    // then don't (un)comment that line.
    if (selectionStop == lastLineStart) {
      // Though if there's no selection, don't do that
      if (ed.isSelectionActive()) {
        stopLine--;
      }
    }

    // If the text is empty, ignore the user.
    // Also ensure that all lines are commented (not just the first)
    // when determining whether to comment or uncomment.
    int length = ed.getDocumentLength();
    boolean commented = true;
    for (int i = startLine; commented && (i <= stopLine); i++) {
      int pos = ed.getLineStartOffset(i);
      if (pos + 2 > length) {
        commented = false;
      } else {
        // Check the first two characters to see if it's already a comment.
        String begin = ed.getText(pos, 2);
        commented = begin.equals("//");
      }
    }

    for (int line = startLine; line <= stopLine; line++) {
      int location = ed.getLineStartOffset(line);
      if (commented) {
        // remove a comment
        ed.select(location, location+2);
        if (ed.getSelectedText().equals("//")) {
          ed.setSelectedText("");
        }
      } else {
        // add a comment
        ed.select(location, location);
        ed.setSelectedText("//");
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    ed.select(ed.getLineStartOffset(startLine),
              ed.getLineEndOffset(stopLine) - 1);
    ed.endAtomicEdit();
  }


  protected void handleIndentOutdent(boolean indent) {
    int tabSize = Preferences.getInteger("editor.tabs.size");
    String tabString = "\t";

    SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
    ed.beginAtomicEdit();

    int startLine = ed.getSelectionStartLine();
    int stopLine = ed.getSelectionStopLine();

    // If the selection ends at the beginning of the last line,
    // then don't (un)comment that line.
    int lastLineStart = ed.getLineStartOffset(stopLine);
    int selectionStop = ed.getSelectionStop();
    if (selectionStop == lastLineStart) {
      // Though if there's no selection, don't do that
      if (ed.isSelectionActive()) {
        stopLine--;
      }
    }

    for (int line = startLine; line <= stopLine; line++) {
      int location = ed.getLineStartOffset(line);

      if (indent) {
        ed.select(location, location);
        ed.setSelectedText(tabString);

      } else {  // outdent
        ed.select(location, location + tabSize);
        // Don't eat code if it's not indented
        if (ed.getSelectedText().equals(tabString)) {
          ed.setSelectedText("");
        }
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    ed.select(ed.getLineStartOffset(startLine),
              ed.getLineEndOffset(stopLine) - 1);
    ed.endAtomicEdit();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Implements Sketch &rarr; Run.
   * @param verbose Set true to run with verbose output.
   */
  public void handleRun(final boolean verbose) {
    running = true;
    board.setVerbose(verbose);
    sketch.runInVerboseMode = verbose;
    status.progress(Translate.t("Compiling sketch..."));

    // clear the console on each run, unless the user doesn't want to
    if (Preferences.getBoolean("console.auto_clear")) {
      console.clear();
    }

    // Cannot use invokeLater() here, otherwise it gets
    // placed on the event thread and causes a hang--bad idea all around.
    new Thread(runHandler, "Compiler").start();
  }

  // DAM: in Arduino, this is compile
  class DefaultRunHandler implements Runnable {
    public void run() {
      try {
        sketch.prepare();
        if(sketch.build()) {
            statusNotice(Translate.t("Done compiling."));
        } else {
            statusNotice(Translate.t("Compilation failed."));
        }
      } catch (Exception e) {
        status.unprogress();
        statusError(e);
      }

      status.unprogress();
    }
  }

  class DefaultStopHandler implements Runnable {
    public void run() {
      try {
        // DAM: we should try to kill the compilation or upload process here.
      } catch (Exception e) {
        statusError(e);
      }
    }
  }


  /**
   * Check if the sketch is modified and ask user to save changes.
   * @return false if canceling the close/quit operation
   */
  protected boolean checkModified() {
    if (!sketch.isModified()) return true;

    // As of Processing 1.0.10, this always happens immediately.
    // http://dev.processing.org/bugs/show_bug.cgi?id=1456

    String prompt = Translate.t("Save changes to %1?", sketch.getName());

    if (!Base.isMacOS()) {
      int result =
        JOptionPane.showConfirmDialog(this, prompt, Translate.t("Close"),
                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        return handleSave();

      } else if (result == JOptionPane.NO_OPTION) {
        return true;  // ok to continue

      } else if (result == JOptionPane.CANCEL_OPTION) {
        return false;

      } else {
        return false;
      }

    } else {
      // This code is disabled unless Java 1.5 is being used on Mac OS X
      // because of a Java bug that prevents the initial value of the
      // dialog from being set properly (at least on my MacBook Pro).
      // The bug causes the "Don't Save" option to be the highlighted,
      // blinking, default. This sucks. But I'll tell you what doesn't
      // suck--workarounds for the Mac and Apple's snobby attitude about it!
      // I think it's nifty that they treat their developers like dirt.

      // Pane formatting adapted from the quaqua guide
      // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
      JOptionPane pane =
        new JOptionPane("<html> " +
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

      // highlight the safest option ala apple hig
      pane.setInitialValue(options[0]);

      // on macosx, setting the destructive property places this option
      // away from the others at the lefthand side
      pane.putClientProperty("Quaqua.OptionPane.destructiveOption",
                             new Integer(2));

      JDialog dialog = pane.createDialog(this, null);
      dialog.setVisible(true);

      Object result = pane.getValue();
      if (result == options[0]) {  // save (and close/quit)
        return handleSave();

      } else if (result == options[2]) {  // don't save (still close/quit)
        return true;

      } else {  // cancel?
        return false;
      }
    }
  }

    public SketchEditor addTab(File file) {
        String fileName = file.getName();
        SketchEditor newEditor = new SketchEditor(file);
        if (sketch != null) {
            if (sketch.importToLibraryTable != null) {
                if (fileName.endsWith(".ino") || fileName.endsWith(".pde")) {
                    createExtraTokens(newEditor);
                }
            }
        }
        tabs.add(fileName, newEditor);
        return newEditor;
    }


  /**
   * Actually handle the save command. If 'immediately' is set to false,
   * this will happen in another thread so that the message area
   * will update and the save button will stay highlighted while the
   * save is happening. If 'immediately' is true, then it will happen
   * immediately. This is used during a quit, because invokeLater()
   * won't run properly while a quit is happening. This fixes
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=276">Bug 276</A>.
   */
    public boolean handleSave() {
        if (sketch.isUntitled()) {
            boolean ret = handleSaveAs();
            if (ret) {
                Base.updateMRU(sketch.getFolder());
            }
            return ret;
        } else {
            boolean ret = handleSave2();
            if (ret) {
                Base.updateMRU(sketch.getFolder());
            }
            return ret;
        }
    }


  protected boolean handleSave2() {
    statusNotice(Translate.t("Saving..."));
    boolean saved = false;
    try {
      saved = sketch.save();
      if (saved)
        statusNotice(Translate.t("Done Saving."));
      else
        statusEmpty();
    } catch (Exception e) {
      // show the error as a message in the window
      statusError(e);
    }
    return saved;
  }


  public boolean handleSaveAs() {
        System.out.println("Saving AS");
    statusNotice(Translate.t("Saving..."));
    try {
      if (sketch.saveAs()) {
        statusNotice(Translate.t("Done Saving."));
      } else {
        statusNotice(Translate.t("Save Canceled."));
        return false;
      }
    } catch (Exception e) {
      // show the error as a message in the window
      statusError(e);

    } finally {
      // make sure the toolbar button deactivates
      //toolbar.deactivate(EditorToolbar.SAVE);
    }

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
                                  Translate.w("Serial port %1 not found. Retry the upload with another serial port?", 30, "\n", Preferences.get("serial.port")),
                                  Translate.t("Serial port not found"),
                                  JOptionPane.PLAIN_MESSAGE,
                                  null,
                                  names,
                                  0);
    if (result == null) return false;
    selectSerialPort(result);
    return true;
  }


  /**
   * Called by Sketch &rarr; Export.
   * Handles calling the export() function on sketch, and
   * queues all the gui status stuff that comes along with it.
   * <p/>
   * Made synchronized to (hopefully) avoid problems of people
   * hitting export twice, quickly, and horking things up.
   */
  /**
   * Handles calling the export() function on sketch, and
   * queues all the gui status stuff that comes along with it.
   *
   * Made synchronized to (hopefully) avoid problems of people
   * hitting export twice, quickly, and horking things up.
   */
  synchronized public void handleExport(boolean verbose) {
    console.clear();
    status.progress(Translate.t("Uploading to I/O Board..."));
    board.setVerbose(verbose);
    sketch.runInVerboseMode = verbose;

    new Thread(exportHandler, "Uploader").start();
  }

  // DAM: in Arduino, this is upload
  class DefaultExportHandler implements Runnable {
    public void run() {

        uploading = true;
          
        boolean success = sketch.upload();
        if (success) {
          statusNotice(Translate.t("Done uploading."));
        } else {
          // error message will already be visible
        }
      status.unprogress();
      uploading = false;
      //toolbar.clear();
      //toolbar.deactivate(EditorToolbar.EXPORT);
    }
  }

  /**
   * Handler for File &rarr; Page Setup.
   */
  public void handlePageSetup() {
    //printerJob = null;
    if (printerJob == null) {
      printerJob = PrinterJob.getPrinterJob();
    }
    if (pageFormat == null) {
      pageFormat = printerJob.defaultPage();
    }
    pageFormat = printerJob.pageDialog(pageFormat);
  }


  /**
   * Handler for File &rarr; Print.
   */
  public void handlePrint() {
    statusNotice(Translate.t("Printing..."));
    //printerJob = null;
    if (printerJob == null) {
      printerJob = PrinterJob.getPrinterJob();
    }
    if (pageFormat != null) {
      printerJob.setPrintable(getTextArea().textArea, pageFormat);
    } else {
      printerJob.setPrintable(getTextArea().textArea);
    }
    // set the name of the job to the code name
    printerJob.setJobName(sketch.getCodeByEditor((SketchEditor)tabs.getSelectedComponent()).file.getName());

    if (printerJob.printDialog()) {
      try {
        printerJob.print();
        statusNotice(Translate.t("Done printing."));

      } catch (PrinterException pe) {
        statusError(Translate.t("Error while printing."));
        pe.printStackTrace();
      }
    } else {
      statusNotice(Translate.t("Printing canceled."));
    }
    //printerJob = null;  // clear this out?
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Show an error int the status bar.
   */
  public void statusError(String what) {
    status.error(what);
    //new Exception("deactivating RUN").printStackTrace();
    //toolbar.deactivate(EditorToolbar.RUN);
  }


  /**
   * Show an exception in the editor status bar.
   */
  public void statusError(Exception e) {
    e.printStackTrace();

    SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();

    if (e instanceof RunnerException) {
      RunnerException re = (RunnerException) e;
      if (re.hasCodeIndex()) {
        tabs.setSelectedIndex(re.getCodeIndex());
      }
      if (re.hasCodeLine()) {
        int line = re.getCodeLine();
        // subtract one from the end so that the \n ain't included
        if (line >= ed.getLineCount()) {
          // The error is at the end of this current chunk of code,
          // so the last line needs to be selected.
          line = ed.getLineCount() - 1;
          if (ed.getLineText(line).length() == 0) {
            // The last line may be zero length, meaning nothing to select.
            // If so, back up one more line.
            line--;
          }
        }
        logger.debug("Editor: line: " + line);
        logger.debug("Editor: ed.getLineCount()" + ed.getLineCount());
        if (line < 0 || line >= ed.getLineCount()) {

          message("Bad error line: " + line + "\n", 2);
        } else {
          ed.select(ed.getLineStartOffset(line),
                          ed.getLineEndOffset(line) - 1);
        }
      }
    }

    // Since this will catch all Exception types, spend some time figuring
    // out which kind and try to give a better error message to the user.
    String mess = e.getMessage();
    if (mess != null) {
      String javaLang = "java.lang.";
      if (mess.indexOf(javaLang) == 0) {
        mess = mess.substring(javaLang.length());
      }
      String rxString = "RuntimeException: ";
      if (mess.indexOf(rxString) == 0) {
        mess = mess.substring(rxString.length());
      }
      statusError(mess);
    }
//    e.printStackTrace();
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
        SketchEditor ed = (SketchEditor)tabs.getSelectedComponent();
        ed.scrollTo(0);
        ed.beginAtomicEdit();
        ed.setCaretPosition(0);
        ed.insert("#include <");
        ed.insert(name);
        ed.insert(">\n");   
        ed.endAtomicEdit();
    }

    public void populateMenus() {
        rebuildExamplesMenu();
        rebuildImportMenu();
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
        importMenu.addSeparator();

        HashMap<String, File> globalLibraries = Base.getLibraryCollection("global");
        HashMap<String, File> coreLibraries = Base.getLibraryCollection(core.getName());
        HashMap<String, File> contributedLibraries = Base.getLibraryCollection("sketchbook");

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importLibrary(e.getActionCommand());
            }
        };

        if (globalLibraries.size() > 0) {
            JMenu globalMenu = new JMenu(Translate.t("Standard"));
            String[] entries = (String[]) globalLibraries.keySet().toArray(new String[0]);
            for (String entry : entries) {
                item = new JMenuItem(entry);
                item.addActionListener(listener);
                item.setActionCommand(entry);
                globalMenu.add(item);
            }
            importMenu.add(globalMenu);
        }

        if (coreLibraries.size() > 0) {
            JMenu coreMenu = new JMenu(core.getName());
            String[] entries = (String[]) coreLibraries.keySet().toArray(new String[0]);
            for (String entry : entries) {
                item = new JMenuItem(entry);
                item.addActionListener(listener);
                item.setActionCommand(entry);
                coreMenu.add(item);
            }
            importMenu.add(coreMenu);
        }

        if (contributedLibraries.size() > 0) {
            JMenu contributedMenu = new JMenu(Translate.t("Contributed"));
            String[] entries = (String[]) contributedLibraries.keySet().toArray(new String[0]);
            for (String entry : entries) {
                item = new JMenuItem(entry);
                item.addActionListener(listener);
                item.setActionCommand(entry);
                contributedMenu.add(item);
            }
            importMenu.add(contributedMenu);
        }

        // reset the table mapping imports to libraries
        sketch.importToLibraryTable = new HashMap<String, File>();
        sketch.importToLibraryTable.putAll(globalLibraries);
        sketch.importToLibraryTable.putAll(coreLibraries);
        sketch.importToLibraryTable.putAll(contributedLibraries);
        System.err.println("ITLT size: " + sketch.importToLibraryTable.size());
    }

    public void rebuildExamplesMenu() {
        File coreExamples = new File(core.getFolder(), "examples");
        File coreLibs = new File(core.getFolder(), "libraries");
        File sbLibs = new File(Base.getSketchbookFolder(),"libraries");

        if (examplesMenu == null) return;
        examplesMenu.removeAll();
        addSketches(examplesMenu, Base.getExamplesFolder());

        JMenu coreItem = new JMenu(core.get("name", core.getName()));
        examplesMenu.add(coreItem);
        if (coreExamples.isDirectory()) {
            examplesMenu.addSeparator();
            addSketches(examplesMenu, coreExamples);
        }

        if (coreLibs.isDirectory()) {
            addSketches(coreItem, coreLibs);
        }

        JMenu contributedItem = new JMenu(Translate.t("Contributed"));
        examplesMenu.add(contributedItem);

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

    public void rebuildBoardsMenu() {

        boardsMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        HashMap<String, JMenu> groupings;
        groupings = new HashMap<String, JMenu>();

        JMenuItem addboard = new JMenuItem(Translate.t("Add Boards..."));
        addboard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleAddBoards();
            }
        });

        boardsMenu.add(addboard);
        boardsMenu.addSeparator();

        for (Board thisboard : Base.boards.values()) {
            AbstractAction action = new AbstractAction(thisboard.getLongName()) {
                public void actionPerformed(ActionEvent actionevent) {
                    selectBoard((String) getValue("board"));
                }
            };
            action.putValue("board", thisboard.getName());
            JMenuItem item = new JRadioButtonMenuItem(action);
            if (thisboard.equals(board)) {
                item.setSelected(true);
            }
            group.add(item);
            if (thisboard.getGroup() != null) {
                if (groupings.get(thisboard.getGroup()) == null) {
                    groupings.put(thisboard.getGroup(), new JMenu(thisboard.getGroup()));
                }
                groupings.get(thisboard.getGroup()).add(item);
            } else {
                boardsMenu.add(item);
            }
            for (String grp : groupings.keySet()) {
                boardsMenu.add(groupings.get(grp));
            }
        }
    }
    
    public void selectBoard(String b) {
        selectBoard(Base.boards.get(b));
    }

    public void selectBoard(Board b) {
        board = b;
        core = board.getCore();
        sketch.settings.put("board.root", board.getFolder().getAbsolutePath());
        lineStatus.setText(b.getLongName() + " on " + Preferences.get("serial.port"));
        rebuildBoardsMenu();
        Preferences.set("board", b.getName());
        populateMenus();

        for (int i = 0; i < tabs.getTabCount(); i++) {
            SketchEditor se = (SketchEditor)tabs.getComponentAt(i);
            String fileName = se.getFile().getName();

            if (fileName.endsWith(".ino") || fileName.endsWith(".pde")) {
                createExtraTokens((SketchEditor)tabs.getComponentAt(i));
            }
        }
    }

    public void rebuildCoresMenu()
    {
        coresMenu.removeAll();
        JMenuItem item = new JMenuItem(Translate.t("Add Core..."));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleAddCore();
            }
        });
        coresMenu.add(item);
        coresMenu.addSeparator();
        item = new JMenuItem(Translate.t("Installed Cores") + ":");
        coresMenu.add(item);

        String[] entries = (String[]) Base.cores.keySet().toArray(new String[0]);

        for (int i = 0; i < entries.length; i++) {
            Core c = Base.cores.get(entries[i]);
            item = new JMenuItem("  " + c.get("name", entries[i]));
            coresMenu.add(item);
        }
    }

    public void rebuildPluginsMenu()
    {
        toolsMenu.removeAll();
        JMenuItem item;

        item = new JMenuItem(Translate.t("Add Plugin..."));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleInstallPlugin();
            }
        });
        toolsMenu.add(item);
        toolsMenu.addSeparator();

        String[] entries = (String[]) Base.plugins.keySet().toArray(new String[0]);

        HashMap<String, JMenuItem> menus = new HashMap<String, JMenuItem>();
        for (int i=0; i<entries.length; i++) {
            final Plugin t = Base.plugins.get(entries[i]);
            int flags = 0;
            try {
                flags = t.flags();
            } catch (Exception e) {
                flags = BasePlugin.MENU;
            }
            if ((flags & BasePlugin.MENU) != 0) {
                item = new JMenuItem(t.getMenuTitle());
                final Editor me = this;
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        t.init(me);
                        SwingUtilities.invokeLater(t);
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
            toolsMenu.add(menus.get(entry));
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
                                "You may need to restart " + Theme.get("product.cap") + " to update\n" +
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

        Base.handleClose(this);
    }

    public void handleStatusOk(int function, String data) {
        switch (function) {
            case ADD_NEW_FILE:
                if (!(data.endsWith(".cpp") || data.endsWith(".c") || data.endsWith(".h") || data.endsWith(".S"))) {
                    Base.showWarning(Translate.t("Error Adding File"),Translate.w("Error: you can only add .c, .cpp, .h or .S files to a sketch", 40, "\n"), null);
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
        String[] entries = (String[]) sketch.importToLibraryTable.keySet().toArray(new String[0]);

        ArduinoTokenMaker tm = (ArduinoTokenMaker)((RSyntaxDocument)ta.getDocument()).getSyntaxStyle();

        tm.clear();

        loadKeywordsFromFile(tm, Base.getContentFile("lib/keywords.txt"));
        loadKeywordsFromFile(tm, new File(core.getFolder(), "keywords.txt"));

        for (String entry : entries) {
            File libFolder = sketch.importToLibraryTable.get(entry);
            File keywords = new File(libFolder, "keywords.txt");
            loadKeywordsFromFile(tm, keywords);
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
}

