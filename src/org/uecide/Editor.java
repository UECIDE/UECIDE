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

import org.uecide.plugin.*;
import org.uecide.editors.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import java.util.*;
import java.util.Timer;
import java.util.regex.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.border.*;
import java.lang.reflect.*;
import javax.imageio.*;

import java.awt.datatransfer.*;

import org.uecide.Compiler;

import java.beans.*;

import java.util.jar.*;
import java.util.zip.*;

import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

public class Editor extends JFrame {

    Box mainDecorationContainer;

    JSplitPane topBottomSplit;
    JSplitPane leftRightSplit;
    JSplitPane sidebarSplit;

    JTree sketchContentTree;
    JTree sketchFilesTree;

    Sketch loadedSketch;

    JMenuBar menuBar;

    JMenu fileMenu;
    JMenu editMenu;
    JMenu sketchMenu;
    JMenu hardwareMenu;
    JMenu toolsMenu;
    JMenu helpMenu;
    JMenu serialPortsMenu;
    JMenu discoveredBoardsMenu;
    JMenu optionsMenu;
    JMenu programmersSubmenu; 

    JToolBar toolbar;
    JToolBar treeToolBar;

    JPanel treePanel;
    JPanel editorPanel;
    JPanel statusBar;
    JPanel projectPanel;
    JPanel sidebarPanel;
    JPanel filesPanel;
    JPanel consolePanel;

    Console console;
    Console output;

    JTabbedPane editorTabs;
    JTabbedPane projectTabs;
    JTabbedPane sidebarTabs;
    JTabbedPane consoleTabs;

    JScrollPane treeScroll;
    JScrollPane filesTreeScroll;

    DefaultMutableTreeNode treeRoot;
    DefaultMutableTreeNode treeSource;
    DefaultMutableTreeNode treeHeaders;
    DefaultMutableTreeNode treeLibraries;
    DefaultMutableTreeNode treeOutput;
    DefaultMutableTreeNode treeDocs;
    DefaultMutableTreeNode treeBinaries;
    DefaultTreeModel treeModel;

    DefaultMutableTreeNode filesTreeRoot;
    DefaultTreeModel filesTreeModel;

    JShadowScrollPane consoleScroll;
    JShadowScrollPane outputScroll;

    JProgressBar statusProgress;
    JLabel statusText;

    JButton abortButton;
    JButton runButton;
    JButton programButton;

    JScrollPane sidebarScroll;

    public static ArrayList<Editor>editorList = new ArrayList<Editor>();

    ArrayList<Plugin> plugins = new ArrayList<Plugin>();

    Thread compilationThread = null;

    class FlaggedList {
        String name;
        int color;

        public static final int Red = 1;
        public static final int Green = 2;
        public static final int Yellow = 3;
        public static final int Blue = 4;

        public FlaggedList(int c, String n) {
            color = c;
            name = n;
        }

        public String toString() {
            return name;
        }
     
        public int getColor() {
            return color;
        }
    }

    class DefaultRunHandler implements Runnable {
        boolean upload = false;
        public DefaultRunHandler(boolean doUpload) {
            upload = doUpload;
        }
        public void run() {
            runButton.setEnabled(false);
            programButton.setEnabled(false);
            runButton.setVisible(false);
            abortButton.setVisible(true);

            try {
                if(loadedSketch.build()) {
                    //        reportSize();
                    if(upload) {
                        loadedSketch.upload();
                    }
                }
            } catch(Exception e) {
                error(e);
            }

            runButton.setEnabled(true);
            programButton.setEnabled(true);
            runButton.setVisible(true);
            abortButton.setVisible(false);
        }
    }

    class LibCompileRunHandler implements Runnable {
        Library library;
        public LibCompileRunHandler(Library lib) {
            library = lib;
        }
        public void run() {
            runButton.setEnabled(false);
            programButton.setEnabled(false);
            runButton.setVisible(false);
            abortButton.setVisible(true);

            try {
                loadedSketch.precompileLibrary(library);
            } catch(Exception e) {
                error(e);
            }

            runButton.setEnabled(true);
            programButton.setEnabled(true);
            runButton.setVisible(true);
            abortButton.setVisible(false);
        }
    }

    public Editor(Sketch s) {
        super();
        Editor.registerEditor(this);
        loadedSketch = s;
//String crash = null;
//if (crash.equals("")) { // crash; 
//}
        for(Class<?> plugin : Base.plugins.values()) {
            try {
                Constructor<?> ctor = plugin.getConstructor(Editor.class);
                Plugin p = (Plugin)(ctor.newInstance(new Object[] { this }));
                plugins.add(p);
            } catch(Exception e) {
//                e.printStackTrace();
            }
        }

        this.setLayout(new BorderLayout());

        Base.setIcon(this);

        treePanel = new JPanel();
        projectPanel = new JPanel();
        sidebarPanel = new JPanel();
        filesPanel = new JPanel();
        editorPanel = new JPanel();
        consolePanel = new JPanel();
        statusBar = new JPanel();

        treePanel.setLayout(new BorderLayout());
        projectPanel.setLayout(new BorderLayout());
        sidebarPanel.setLayout(new BorderLayout());
        filesPanel.setLayout(new BorderLayout());
        editorPanel.setLayout(new BorderLayout());
        consolePanel.setLayout(new BorderLayout());
        statusBar.setLayout(new BorderLayout());

        editorTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        projectTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        sidebarTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        consoleTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

        editorTabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                for (int i = 0; i < getTabCount(); i++) {
                    TabLabel l = getTabLabel(i);
                    if (l != null) {
                        l.changeState(i == editorTabs.getSelectedIndex());
                    }
                }
                updateMenus();
            }
        });

        editorPanel.add(editorTabs, BorderLayout.CENTER);

        int width = Preferences.getInteger("editor.window.width");
        int height = Preferences.getInteger("editor.window.height");

        leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectPanel, editorPanel);
        sidebarSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftRightSplit, sidebarPanel);
        topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sidebarSplit, consolePanel);


// Uncomment these two lines to force a NullPointer exception to test
// the creash reporting system

//String forcedCrash = null;
//System.err.println(forcedCrash.length());

        sidebarSplit.setOneTouchExpandable(true);
        leftRightSplit.setOneTouchExpandable(true);
        topBottomSplit.setOneTouchExpandable(true);

        leftRightSplit.setContinuousLayout(true);
        topBottomSplit.setContinuousLayout(true);

        leftRightSplit.setResizeWeight(0.1D);
        topBottomSplit.setResizeWeight(0.7D);
        sidebarSplit.setResizeWeight(0.9D);

//        int dividerSize = Preferences.getInteger("editor.layout.split_sidebar");
//        if (dividerSize < 10) {
//            dividerSize = getWidth() - 200;
//        }
//        sidebarSplit.setDividerLocation(dividerSize);
//
//        dividerSize = Preferences.getInteger("editor.layout.split_console");
//        topBottomSplit.setDividerLocation(dividerSize);
//
//        dividerSize = Preferences.getInteger("editor.layout.split_tree");
//        leftRightSplit.setDividerLocation(dividerSize);

        add(topBottomSplit, BorderLayout.CENTER);

        final Editor me = this;

        consoleScroll = new JShadowScrollPane(
            Base.getTheme().getInteger("console.shadow.top"),
            Base.getTheme().getInteger("console.shadow.bottom")
        );

        outputScroll = new JShadowScrollPane(
            Base.getTheme().getInteger("console.shadow.top"),
            Base.getTheme().getInteger("console.shadow.bottom")
        );

        console = new Console();
        output = new Console();

        console.setURLClickListener(this);

        consoleScroll.setViewportView(console);
        outputScroll.setViewportView(output);

//        consolePanel.add(consoleScroll);

        toolbar = new JToolBar();
        treeToolBar = new JToolBar();

        treeToolBar.setFloatable(false);
        toolbar.setFloatable(false);
        
        statusText = new JLabel("");
        statusProgress = new JProgressBar();

        JPanel sp = new JPanel();

        sp.add(statusText);

        statusBar.add(sp, BorderLayout.WEST);
        statusBar.add(statusProgress, BorderLayout.EAST);

        Font labelFont = statusText.getFont();
        statusText.setFont(new Font(labelFont.getName(), Font.PLAIN, 12));

        String tbPos = Preferences.get("editor.toolbars.position");

        if((tbPos == null) || tbPos.equals("f") || tbPos.equals("n")) {
            this.add(toolbar, BorderLayout.NORTH);
            toolbar.setOrientation(JToolBar.HORIZONTAL);
        } else if(tbPos.equals("s")) {
            this.add(toolbar, BorderLayout.SOUTH);
            toolbar.setOrientation(JToolBar.HORIZONTAL);
        } else if(tbPos.equals("e")) {
            this.add(toolbar, BorderLayout.EAST);
            toolbar.setOrientation(JToolBar.VERTICAL);
        } else if(tbPos.equals("w")) {
            this.add(toolbar, BorderLayout.WEST);
            toolbar.setOrientation(JToolBar.VERTICAL);
        } else {
            this.add(toolbar, BorderLayout.NORTH);
            toolbar.setOrientation(JToolBar.HORIZONTAL);
        }

        this.add(statusBar, BorderLayout.SOUTH);

        JButton refreshButton = Editor.addToolbarButton(treeToolBar, "actions", "refresh", "Refresh Project Tree", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!compilerRunning()) {
                    loadedSketch.rescanFileTree();
                    updateTree();
                }
            }
        });
        treeToolBar.add(refreshButton);

        JButton projectSearchButton = Editor.addToolbarButton(treeToolBar, "actions", "search", "Search entire project", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ProjectSearch(Editor.this);
            }
        });
        treeToolBar.add(projectSearchButton);

        treeScroll = new JScrollPane();
        treePanel.add(treeToolBar, BorderLayout.NORTH);
        treePanel.add(treeScroll, BorderLayout.CENTER);
        filesTreeScroll = new JScrollPane();
        filesPanel.add(filesTreeScroll, BorderLayout.CENTER);
        initTreeStructure();

        projectPanel.add(projectTabs, BorderLayout.CENTER);

        projectTabs.add(treePanel, Base.i18n.string("tab.project"));
        projectTabs.add(filesPanel, Base.i18n.string("tab.files"));

        addPanelsToTabs(projectTabs, Plugin.TABS_PROJECT);

        sidebarPanel.add(sidebarTabs, BorderLayout.CENTER);

        addPanelsToTabs(sidebarTabs, Plugin.TABS_SIDEBAR);

        consolePanel.add(consoleTabs, BorderLayout.CENTER);

        consoleTabs.add(consoleScroll, Base.i18n.string("tab.console"));
        consoleTabs.add(outputScroll, Base.i18n.string("tab.output"));

        addPanelsToTabs(consoleTabs, Plugin.TABS_CONSOLE);

//        rotateTabLabels();
        

        updateToolbar();

        toolbar.addSeparator();

        menuBar = new JMenuBar();
        Base.setFont(menuBar, "menu.bar");

        fileMenu = new JMenu(Base.i18n.string("menu.file"));
        Base.setFont(fileMenu, "menu.bar");
        menuBar.add(fileMenu);

        editMenu = new JMenu(Base.i18n.string("menu.edit"));
        Base.setFont(editMenu, "menu.bar");
        menuBar.add(editMenu);

        sketchMenu = new JMenu(Base.i18n.string("menu.sketch"));
        Base.setFont(sketchMenu, "menu.bar");
        menuBar.add(sketchMenu);

        hardwareMenu = new JMenu(Base.i18n.string("menu.hardware"));
        Base.setFont(hardwareMenu, "menu.bar");
        menuBar.add(hardwareMenu);

        toolsMenu = new JMenu(Base.i18n.string("menu.tools"));
        Base.setFont(toolsMenu, "menu.bar");
        menuBar.add(toolsMenu);

        helpMenu = new JMenu(Base.i18n.string("menu.help"));
        Base.setFont(helpMenu, "menu.bar");
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                askCloseWindow();
            }
        });
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.pack();

        setSize(width, height);
        setLocation(Preferences.getInteger("editor.window.x"), Preferences.getInteger("editor.window.y"));
        setProgress(0);
        updateAll();

        addComponentListener(new ComponentListener() {
            public void componentMoved(ComponentEvent e) {
                Point windowPos = e.getComponent().getLocation(null);
                Preferences.setInteger("editor.window.x", windowPos.x);
                Preferences.setInteger("editor.window.y", windowPos.y);
            }
            public void componentResized(ComponentEvent e) {
                Dimension windowSize = e.getComponent().getSize(null);
                Preferences.setInteger("editor.window.width", windowSize.width);
                Preferences.setInteger("editor.window.height", windowSize.height);
            }
            public void componentHidden(ComponentEvent e) {
            }
            public void componentShown(ComponentEvent e) {
            }
        });

        openOrSelectFile(loadedSketch.getMainFile());
//        dividerSize = Preferences.getInteger("editor.layout.split_console");
//        topBottomSplit.setDividerLocation(dividerSize);

//        dividerSize = Preferences.getInteger("editor.layout.split_tree");
//        leftRightSplit.setDividerLocation(dividerSize);

        // We want to do this last as the previous SETs trigger this change listener.

//        sidebarSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
//            new PropertyChangeListener() {
//                public void propertyChange(PropertyChangeEvent e) {
//                    int pos = (Integer)(e.getNewValue());
//                    Preferences.setInteger("editor.layout.split_sidebar", pos);
//                }
//            }
//        );
//
//        leftRightSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
//            new PropertyChangeListener() {
//                public void propertyChange(PropertyChangeEvent e) {
//                    int pos = (Integer)(e.getNewValue());
//                    Preferences.setInteger("editor.layout.split_tree", pos);
//                }
//            }
//        );

//        topBottomSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
//            new PropertyChangeListener() {
//                public void propertyChange(PropertyChangeEvent e) {
//                    int pos = (Integer)(e.getNewValue());
//                    Preferences.setInteger("editor.layout.split_console", pos);
//                }
//            }
//        );

        for (int i = 0; toolbar.getComponentAtIndex(i) != null; i++) {
            Component c = toolbar.getComponentAtIndex(i);
            if (c instanceof JButton) {
                JButton b = (JButton)c;
                b.setBorderPainted(false);
            }
        }
        s.attachToEditor(this);
        this.setVisible(true);

    }
   
    public void rotateTabLabels() {
        for (int i = 0; i < projectTabs.getTabCount(); i++) {
            String c = projectTabs.getTitleAt(i);
            JLabel l = new JLabel(c);
            l.setUI(new VerticalLabelUI(false));
            projectTabs.setTabComponentAt(i, l);
        }
        for (int i = 0; i < sidebarTabs.getTabCount(); i++) {
            String c = sidebarTabs.getTitleAt(i);
            JLabel l = new JLabel(c);
            l.setUI(new VerticalLabelUI(true));
            sidebarTabs.setTabComponentAt(i, l);
        }
    }

    public void updateToolbar() {
        toolbar.removeAll();
        abortButton = Editor.addToolbarButton(toolbar, "actions", "cancel", Base.i18n.string("toolbar.abort"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                abortCompilation();
            }
        });
        abortButton.setVisible(false);
        runButton = Editor.addToolbarButton(toolbar, "actions", "run", Base.i18n.string("toolbar.run"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    loadedSketch.purgeCache();
                    loadedSketch.purgeBuildFiles();
                }

                compile();
            }
        });

        programButton = Editor.addToolbarButton(toolbar, "actions", "program", Base.i18n.string("toolbar.program"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    loadedSketch.purgeCache();
                    loadedSketch.purgeBuildFiles();
                }
                program();
            }
        });

        toolbar.addSeparator();

        Editor.addToolbarButton(toolbar, "actions", "new", Base.i18n.string("toolbar.new"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleNew();
            }
        });

        Editor.addToolbarButton(toolbar, "actions", "open", Base.i18n.string("toolbar.open"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleOpenPrompt();
            }
        });

        Editor.addToolbarButton(toolbar, "actions", "save", Base.i18n.string("toolbar.save"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAllTabs();
            }
        });

        toolbar.addSeparator();

        addPluginsToToolbar(toolbar, Plugin.TOOLBAR_EDITOR);

    }

    class FileCellRenderer implements TreeCellRenderer {
        DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            Color textColor = Base.getTheme().getColor("editor.fgcolor");

            if((value != null) && (value instanceof DefaultMutableTreeNode)) {
                JPanel container = new JPanel();

                container.setLayout(new BorderLayout());
                ImageIcon icon = null;
                JLabel text = null;
                JProgressBar bar = null;

                UIDefaults defaults = javax.swing.UIManager.getDefaults();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                Border noBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
                Border paddingBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);

                container.setBorder(noBorder);

                if (userObject instanceof FlaggedList) {
                    FlaggedList ent = (FlaggedList)userObject;

                    text = new JLabel(ent.toString());
                    if (ent.getColor() == FlaggedList.Red) {
                        icon = Base.getIcon("flags", "fixme", 16);
                    } else if (ent.getColor() == FlaggedList.Green) {
                        icon = Base.getIcon("flags", "note", 16);
                    } else if (ent.getColor() == FlaggedList.Yellow) {
                        icon = Base.getIcon("flags", "todo", 16);
                    } else if (ent.getColor() == FlaggedList.Blue) {
                        icon = Base.getIcon("flags", "info", 16);
                    }

                    Font f = text.getFont();
                    text.setFont(new Font(f.getFamily(), Font.PLAIN, f.getSize() - 2));
                } else if (userObject instanceof TodoEntry) {
                    TodoEntry ent = (TodoEntry)userObject;

                    text = new JLabel(ent.toString());
                    icon = Base.getIcon("bookmarks", "todo", 16);

                    Font f = text.getFont();
                    text.setFont(new Font(f.getFamily(), Font.PLAIN, f.getSize() - 2));
                } else if (userObject instanceof FunctionBookmark) {
                    FunctionBookmark bm = (FunctionBookmark)userObject;
                    text = new JLabel(bm.formatted());
                    icon = Base.getIcon("bookmarks", "function", 16);

                    Font f = text.getFont();
                    text.setFont(new Font(f.getFamily(), Font.PLAIN, f.getSize() - 2));
                } else if (userObject instanceof File) {
                    File file = (File)userObject;
                    text = new JLabel(file.getName());

                    if(file.isDirectory()) {
                        if(expanded) {
                            icon = Base.getIcon("bookmarks", "folder-open", 16);
                        } else {
                            icon = Base.getIcon("bookmarks", "folder", 16);
                        }
                    } else {
                        OverlayIcon oicon = new OverlayIcon(Base.getIcon("mime", FileType.getIcon(file), 16));

                        for(Plugin plugin : plugins) {
                            try {
                                ImageIcon fi = plugin.getFileIconOverlay(file);

                                if(fi != null) {
                                    oicon.add(fi);
                                }
                            } catch(AbstractMethodError e) {
                            } catch(Exception e) {
                            }
                        }

                        icon = (ImageIcon)oicon;
                    }
                } else if (userObject instanceof Library) {
                    Library lib = (Library)userObject;
                    int pct = lib.getCompiledPercent();

                    if(loadedSketch.libraryIsCompiled(lib) && (pct >= 100 || pct <= 0)) {
                        icon = Base.getIcon("bookmarks", "library-good", 16);
                    } else {
                        if (pct >= 50) {
                            icon = Base.getIcon("bookmarks", "library-semi", 16);
                        } else {
                            icon = Base.getIcon("bookmarks", "library-bad", 16);
                        }
                    }

                    if(pct > 0 && pct < 100) {
                        bar = new JProgressBar();
                        bar.setString(lib.getName());
                        Dimension d = bar.getSize();
                        d.width = 80;
                        bar.setPreferredSize(d);
                        bar.setStringPainted(true);
                        bar.setValue(pct);
                    } else {
                        text = new JLabel(lib.getName());
                    }
                } else if (userObject instanceof Sketch) {
                    Sketch so = (Sketch)userObject;
                    text = new JLabel(so.getName());
                    icon = so.getIcon(16);

                    if(icon == null) {
                        icon = Base.loadIconFromResource("icon16.png");
                    }
                } else {
                    text = new JLabel(userObject.toString());

                    if(expanded) {
                        icon = Base.getIcon("bookmarks", "folder-open", 16);
                    } else {
                        icon = Base.getIcon("bookmarks", "folder", 16);
                    }
                }
                container.setOpaque(true);

                if (text != null) {
                    text.setBorder(paddingBorder);
                    text.setOpaque(true);
                    container.add(text, BorderLayout.CENTER);
                    if (selected) {
                        Color bg = defaults.getColor("List.selectionBackground");
                        Color fg = defaults.getColor("List.selectionForeground");
                        text.setBackground(bg);
                        text.setForeground(fg);
                    } else {
                        Color bg = defaults.getColor("List.background");
                        Color fg = defaults.getColor("List.foreground");
                        text.setBackground(bg);
                        text.setForeground(fg);
                    }

                }
                if (icon != null) {
                    JLabel i = new JLabel(icon);
                    i.setOpaque(true);
                    container.add(i, BorderLayout.WEST);
                    if (selected) {
                        Color bg = defaults.getColor("List.selectionBackground");
                        Color fg = defaults.getColor("List.selectionForeground");
                        i.setBackground(bg);
                        i.setForeground(fg);
                    } else {
                        Color bg = defaults.getColor("List.background");
                        Color fg = defaults.getColor("List.foreground");
                        i.setBackground(bg);
                        i.setForeground(fg);
                    }
                }

                if (bar != null) {
                    container.add(bar, BorderLayout.CENTER);
                    bar.setOpaque(true);
                    if (selected) {
                        Color bg = defaults.getColor("List.selectionBackground");
                        Color fg = defaults.getColor("List.selectionForeground");
                        bar.setBackground(bg);
                        bar.setForeground(fg);
                    } else {
                        Color bg = defaults.getColor("List.background");
                        Color fg = defaults.getColor("List.foreground");
                        bar.setBackground(bg);
                        bar.setForeground(fg);
                    }
                }
                return container;
            }

            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    }

    public void addFileTreeToNode(DefaultMutableTreeNode treenode, File dir) {
        if(!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] buildFiles = dir.listFiles();

        if (buildFiles != null) {
            Arrays.sort(buildFiles);

            for(File file : buildFiles) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(file.getName());
                node.setUserObject(file);

                if(file.isDirectory()) {
                    addFileTreeToNode(node, file);
                }

                treenode.add(node);
            }
        }
    }


    public void initTreeStructure() {
        treeRoot = new DefaultMutableTreeNode(loadedSketch.getName());
        treeRoot.setUserObject(loadedSketch);
        treeModel = new DefaultTreeModel(treeRoot);
        sketchContentTree = new JTree(treeModel);
        treeSource = new DefaultMutableTreeNode(Base.i18n.string("tree.source"));
        treeHeaders = new DefaultMutableTreeNode(Base.i18n.string("tree.headers"));
        treeLibraries = new DefaultMutableTreeNode(Base.i18n.string("tree.libraries"));
        treeBinaries = new DefaultMutableTreeNode(Base.i18n.string("tree.binaries"));
        treeOutput = new DefaultMutableTreeNode(Base.i18n.string("tree.output"));
        treeDocs = new DefaultMutableTreeNode(Base.i18n.string("tree.docs"));
        TreeCellRenderer renderer = new FileCellRenderer();
        sketchContentTree.setCellRenderer(renderer);
        treeRoot.add(treeSource);
        treeRoot.add(treeHeaders);
        treeRoot.add(treeLibraries);
        treeRoot.add(treeBinaries);
        treeRoot.add(treeOutput);
        treeRoot.add(treeDocs);

        sketchContentTree.expandPath(new TreePath(treeRoot.getPath()));
        sketchContentTree.expandPath(new TreePath(treeSource.getPath()));


        treeScroll.setViewportView(sketchContentTree);
        treeScroll.setOpaque(true);
        sketchContentTree.setOpaque(true);
        sketchContentTree.addMouseListener(new TreeMouseListener());

        filesTreeRoot = new DefaultMutableTreeNode(loadedSketch.getName());
        filesTreeRoot.setUserObject(loadedSketch.getFolder());
        filesTreeModel = new DefaultTreeModel(filesTreeRoot);
        sketchFilesTree = new JTree(filesTreeModel);
        filesTreeScroll.setViewportView(sketchFilesTree);
        sketchFilesTree.setCellRenderer(renderer);
        sketchFilesTree.addMouseListener(new FileTreeMouseListener());
        sketchFilesTree.setDragEnabled(true);
        sketchFilesTree.setDropMode(DropMode.ON_OR_INSERT);
        sketchFilesTree.setTransferHandler(new TreeTransferHandler());
        sketchFilesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
    }

    class TreeTransferHandler extends TransferHandler {
        DataFlavor nodesFlavor;
        DataFlavor nixFileDataFlavor;
        DataFlavor[] flavors = new DataFlavor[1];
        DefaultMutableTreeNode[] nodesToRemove;

        public TreeTransferHandler() {
            try {
                String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + DefaultMutableTreeNode[].class.getName() + "\"";
                nodesFlavor = new DataFlavor(mimeType);
                nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
                flavors[0] = nodesFlavor;
            } catch(ClassNotFoundException e) {
                error(e);
            }
        }

        public boolean canImport(TransferHandler.TransferSupport support) {
            if(!support.isDrop()) {
                return false;
            }

            support.setShowDropLocation(true);

            if(support.isDataFlavorSupported(nodesFlavor)) {
                JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
                JTree tree = (JTree)support.getComponent();
                int dropRow = tree.getRowForPath(dl.getPath());

                int[] selRows = tree.getSelectionRows();

                for(int i = 0; i < selRows.length; i++) {
                    if(selRows[i] == dropRow) {
                        return false;
                    }
                }

                TreePath dest = dl.getPath();
                DefaultMutableTreeNode target = (DefaultMutableTreeNode)dest.getLastPathComponent();
                TreePath path = tree.getPathForRow(selRows[0]);
                DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode)path.getLastPathComponent();

                if(firstNode.getChildCount() > 0 && target.getLevel() < firstNode.getLevel()) {
                    return false;
                }

                return true;
            }

            try {
                if(support.isDataFlavorSupported(nixFileDataFlavor)) {
                    return true;
                }
            } catch(Exception e) {
                //error(e);
            }

            return false;
        }

        private boolean haveCompleteNode(JTree tree) {
            int[] selRows = tree.getSelectionRows();
            TreePath path = tree.getPathForRow(selRows[0]);
            DefaultMutableTreeNode first = (DefaultMutableTreeNode)path.getLastPathComponent();
            int childCount = first.getChildCount();

            if(childCount > 0 && selRows.length == 1) {
                return false;
            }

            for(int i = 1; i < selRows.length; i++) {
                path = tree.getPathForRow(selRows[i]);
                DefaultMutableTreeNode next = (DefaultMutableTreeNode)path.getLastPathComponent();

                if(first.isNodeChild(next)) {
                    if(childCount > selRows.length - 1) {
                        return false;
                    }
                }
            }

            return true;
        }

        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree)c;
            TreePath[] paths = tree.getSelectionPaths();

            if(paths != null) {
                // Make up a node array of copies for transfer and
                // another for/of the nodes that will be removed in
                // exportDone after a successful drop.
                ArrayList<DefaultMutableTreeNode> copies =
                    new ArrayList<DefaultMutableTreeNode>();
                ArrayList<DefaultMutableTreeNode> toRemove =
                    new ArrayList<DefaultMutableTreeNode>();
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)paths[0].getLastPathComponent();
                DefaultMutableTreeNode copy = copy(node);
                copies.add(copy);
                toRemove.add(node);

                for(int i = 1; i < paths.length; i++) {
                    DefaultMutableTreeNode next =
                        (DefaultMutableTreeNode)paths[i].getLastPathComponent();

                    // Do not allow higher level nodes to be added to list.
                    if(next.getLevel() < node.getLevel()) {
                        break;
                    } else if(next.getLevel() > node.getLevel()) {  // child node
                        copy.add(copy(next));
                        // node already contains child
                    } else {                                        // sibling
                        copies.add(copy(next));
                        toRemove.add(next);
                    }
                }

                DefaultMutableTreeNode[] nodes =
                    copies.toArray(new DefaultMutableTreeNode[copies.size()]);
                nodesToRemove =
                    toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
                return new NodesTransferable(nodes);
            }

            return null;
        }

        /** Defensive copy used in createTransferable. */
        private DefaultMutableTreeNode copy(TreeNode node) {
            return new DefaultMutableTreeNode(node);
        }

        protected void exportDone(JComponent source, Transferable data, int action) {
            if((action & MOVE) == MOVE) {
                JTree tree = (JTree)source;
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

                // Remove nodes saved in nodesToRemove in createTransferable.
                for(int i = 0; i < nodesToRemove.length; i++) {
                    if(nodesToRemove[i].getParent() != null) {
                        model.removeNodeFromParent(nodesToRemove[i]);
                    }
                }
            }
        }

        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        public boolean importData(TransferHandler.TransferSupport support) {
            try {
                if(!canImport(support)) {
                    return false;
                }

                if(support.isDataFlavorSupported(nodesFlavor)) {
                    // Extract transfer data.
                    DefaultMutableTreeNode[] nodes = null;

                    try {
                        Transferable t = support.getTransferable();
                        nodes = (DefaultMutableTreeNode[])t.getTransferData(nodesFlavor);
                    } catch(UnsupportedFlavorException ufe) {
                        System.out.println("UnsupportedFlavor: " + ufe.getMessage());
                    } catch(java.io.IOException ioe) {
                        System.out.println("I/O error: " + ioe.getMessage());
                    }

                    // Get drop location info.

                    JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
                    int childIndex = dl.getChildIndex();
                    TreePath dest = dl.getPath();
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)dest.getLastPathComponent();

                    // You can't drop files into another file, so if it's not a directory
                    // that we're dropping onto then move up a level.
                    File destinationFile = (File)parent.getUserObject();

                    if(!destinationFile.isDirectory()) {
                        parent = (DefaultMutableTreeNode)parent.getParent();
                        destinationFile = destinationFile.getParentFile();
                    }

                    File originalParent = ((File)((DefaultMutableTreeNode)nodes[0].getUserObject()).getUserObject()).getParentFile();

                    if(destinationFile.equals(originalParent)) {
                        return false;
                    }

                    JTree tree = (JTree)support.getComponent();
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    // Configure for drop mode.
                    int index = childIndex;    // DropMode.INSERT

                    if(childIndex == -1) {     // DropMode.ON
                        index = parent.getChildCount();
                    }

                    // Add data to model.
                    for(int i = 0; i < nodes.length; i++) {
                        File nodeFile = (File)((DefaultMutableTreeNode)nodes[i].getUserObject()).getUserObject();
                        File newFile = new File(destinationFile, nodeFile.getName());

                        // If the file is open in a tab then change the file referenced by the tab
                        // to the new path name.
                        int tab = getTabByFile(nodeFile);

                        if(tab >= 0) {
                            setTabFile(tab, newFile);
                        }

                        // If the file being moved was a "top level" file then move it, otherwise ignore it.
                        if(nodeFile.getParentFile().equals(originalParent)) {
                            nodeFile.renameTo(newFile);
                        }


                        model.insertNodeInto(nodes[i], parent, index++);
                    }
                } else if(support.isDataFlavorSupported(nixFileDataFlavor)) {
                    Transferable t = support.getTransferable();
                    String data = (String)t.getTransferData(nixFileDataFlavor);

                    JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
                    TreePath dest = dl.getPath();
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)dest.getLastPathComponent();

                    File destinationFile = (File)parent.getUserObject();

                    if(!destinationFile.isDirectory()) {
                        parent = (DefaultMutableTreeNode)parent.getParent();
                        destinationFile = destinationFile.getParentFile();
                    }

                    for(StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
                        String token = st.nextToken().trim();

                        if(token.startsWith("#") || token.isEmpty()) {
                            // comment line, by RFC 2483
                            continue;
                        }

                        try {
                            File file = new File(new URI(token));
                            // store this somewhere
                            File destfile = new File(destinationFile, file.getName());

                            if(file.isDirectory()) {
                                Base.copyDir(file, destfile);
                            } else {
                                Base.copyFile(file, destfile);
                            }
                        } catch(Exception e) {
                        }
                    }
                }

                loadedSketch.rescanFileTree();
                updateTree();
            } catch(Exception ee) {
//                ee.printStackTrace();
            }

            return true;
        }

        public String toString() {
            return getClass().getName();
        }

        public class NodesTransferable implements Transferable {
            DefaultMutableTreeNode[] nodes;

            public NodesTransferable(DefaultMutableTreeNode[] nodes) {
                this.nodes = nodes;
            }

            public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
                if(!isDataFlavorSupported(flavor))
                    throw new UnsupportedFlavorException(flavor);

                return nodes;
            }

            public DataFlavor[] getTransferDataFlavors() {
                return flavors;
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return nodesFlavor.equals(flavor);
            }
        }
    }

    public boolean mergeTrees(DefaultMutableTreeNode dst, DefaultMutableTreeNode src) {
        // First go through and remove any nodes in dst that aren't in src.
        boolean removedNodes = false;
        int currentNode = 0;
        boolean hasChanged = false;

        while(currentNode < dst.getChildCount()) {
            boolean found = false;
            DefaultMutableTreeNode foundNode = null;

            for(int i = 0; i < src.getChildCount(); i++) {
                if(src.getChildAt(i).toString().equals(dst.getChildAt(currentNode).toString())) {
                    foundNode = (DefaultMutableTreeNode)src.getChildAt(i);
                    src.remove(i);
                    found = true;
                    break;
                }
            }

            if(!found) {
                removedNodes = true;
                dst.remove(currentNode);
                hasChanged = true;
                continue;
            }

            // Copy across the user object.
            DefaultMutableTreeNode dnode = (DefaultMutableTreeNode)dst.getChildAt(currentNode);
            Object sob = foundNode.getUserObject();
            Object dob = dnode.getUserObject();

            if(sob != dob) {
                dnode.setUserObject(sob);
            }

            if (mergeTrees(dnode, foundNode)) {
                hasChanged = true;
            }
            currentNode++;
        }

        // Now copy across any new objects.

        boolean addedNodes = false;

        while(src.getChildCount() > 0) {
            dst.add((DefaultMutableTreeNode)src.getChildAt(0));
            hasChanged = true;
            addedNodes = true;
        }

        return hasChanged;
    }

    public void updateKeywords() {
        HashMap<String, Integer>keywordList = loadedSketch.getKeywords();
        for (int i = 0; i < getTabCount(); i++) {
            EditorBase eb = getTab(i);
            if (eb != null) {
                eb.clearKeywords();
                for (Map.Entry<String, Integer>kw : keywordList.entrySet()) {
                    eb.addKeyword(kw.getKey(), kw.getValue());
                }
                eb.repaint();
            }
        }
    }

    public void updateSourceTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                    
                TreePath[] saved = saveTreeState(sketchContentTree);
                if (saved == null) {
                    return;
                }

                DefaultMutableTreeNode ntc = new DefaultMutableTreeNode();
                treeSource.removeAllChildren();
                DefaultMutableTreeNode node;

                File[] flist = loadedSketch.sketchFiles.toArray(new File[0]);

                Arrays.sort(flist);

                for(File f : flist) {
                    int type = FileType.getType(f);

                    switch(type) {
                        case FileType.CSOURCE:
                        case FileType.CPPSOURCE:
                        case FileType.ASMSOURCE:
                        case FileType.SKETCH:
                            node = new DefaultMutableTreeNode(f.getName());
                            node.setUserObject(f);
                            treeSource.add(node);
                            HashMap<Integer, String> funcs = loadedSketch.getFunctionsForFile(f);

                            if(funcs != null) {
                                for(int line : funcs.keySet()) {
                                    FunctionBookmark b = new FunctionBookmark(f, line, funcs.get(line));
                                    DefaultMutableTreeNode fe = new DefaultMutableTreeNode(b);
                                    node.add(fe);
                                }
                            }
                            EditorBase eb = null;
                            int tab = getTabByFile(f);
                            if (tab != -1) {
                                eb = getTab(tab);
                                eb.removeFlagGroup(0x2000);
                            }

                            ArrayList<TodoEntry> todo = loadedSketch.todo(f);
                            if (todo != null && todo.size() > 0) {

                                FlaggedList noteList = new FlaggedList(FlaggedList.Green, Base.i18n.string("tree.notes"));
                                FlaggedList todoList = new FlaggedList(FlaggedList.Yellow, Base.i18n.string("tree.todo"));
                                FlaggedList fixmeList = new FlaggedList(FlaggedList.Red, Base.i18n.string("tree.fixme"));

                                DefaultMutableTreeNode noteEntries = new DefaultMutableTreeNode(noteList);
                                DefaultMutableTreeNode todoEntries = new DefaultMutableTreeNode(todoList);
                                DefaultMutableTreeNode fixmeEntries = new DefaultMutableTreeNode(fixmeList);

                                for (TodoEntry ent : todo) {
                                    DefaultMutableTreeNode tent = new DefaultMutableTreeNode(ent);
                                    if (ent.getType() == TodoEntry.Note) {
                                        noteEntries.add(tent);
                                        if (eb != null) {
                                            eb.flagLine(ent.getLine(), Base.getIcon("flags", "note", 16), 0x2000);
                                        }
                                    } else if (ent.getType() == TodoEntry.Todo) {
                                        todoEntries.add(tent);
                                        if (eb != null) {
                                            eb.flagLine(ent.getLine(), Base.getIcon("flags", "todo", 16), 0x2000);
                                        }
                                    } else if (ent.getType() == TodoEntry.Fixme) {
                                        fixmeEntries.add(tent);
                                        if (eb != null) {
                                            eb.flagLine(ent.getLine(), Base.getIcon("flags", "fixme", 16), 0x2000);
                                        }
                                    }
                                }
                                if (noteEntries.getChildCount() > 0) {
                                    node.add(noteEntries);
                                }
                                if (todoEntries.getChildCount() > 0) {
                                    node.add(todoEntries);
                                }
                                if (fixmeEntries.getChildCount() > 0) {
                                    node.add(fixmeEntries);
                                }
                            }

                            break;
                    }
                }

                treeModel.reload(sortTree(treeSource));
                restoreTreeState(sketchContentTree, saved);
            }
        });
    }

    public DefaultMutableTreeNode sortTree(DefaultMutableTreeNode root) {
        Enumeration e = root.depthFirstEnumeration();
        while(e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
            if(!node.isLeaf()) {
                sort2(node);   //selection sort
            }
        }
        return root;
    }

    public Comparator tnc = new Comparator() {
        public int compare(Object ao, Object bo) {
            DefaultMutableTreeNode a = (DefaultMutableTreeNode)ao;
            DefaultMutableTreeNode b = (DefaultMutableTreeNode)bo;
            //Sort the parent and child nodes separately:
            if ((a.getUserObject() instanceof FlaggedList) && !(b.getUserObject() instanceof FlaggedList)) {
                return -1;
            } else if (!(a.getUserObject() instanceof FlaggedList) && (b.getUserObject() instanceof FlaggedList)) {
                return 1;
            } else {
                String sa = a.getUserObject().toString();
                String sb = b.getUserObject().toString();
                return sa.compareToIgnoreCase(sb);
            }
        }
    };

    @SuppressWarnings("unchecked")
    public void sort2(DefaultMutableTreeNode parent) {
        int n = parent.getChildCount();
        for(int i=0;i< n-1;i++) {
            int min = i;
            for(int j=i+1;j< n;j++) {
                if(tnc.compare((DefaultMutableTreeNode)parent.getChildAt(min),
                               (DefaultMutableTreeNode)parent.getChildAt(j))>0) {
                    min = j;
                }
            }
            if(i!=min) {
                MutableTreeNode a = (MutableTreeNode)parent.getChildAt(i);
                MutableTreeNode b = (MutableTreeNode)parent.getChildAt(min);
                parent.insert(b, i);
                parent.insert(a, min);
            }
        }
    }

    public void updateDocsTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TreePath[] saved = saveTreeState(sketchContentTree);
                if (saved == null) {
                    return;
                }
                treeDocs.removeAllChildren();
                DefaultMutableTreeNode node;

                for(File f : loadedSketch.getFolder().listFiles()) {
                    int type = FileType.getType(f);

                    switch(type) {
                        case FileType.DOCUMENT:
                            node = new DefaultMutableTreeNode(f.getName());
                            node.setUserObject(f);
                            treeDocs.add(node);
                            break;
                    }
                }

        treeModel.nodeStructureChanged(treeDocs);
                restoreTreeState(sketchContentTree, saved);
            }
        });
    }

    public void refreshTreeModel() {
        TreePath[] saved = saveTreeState(sketchContentTree);
        if (saved == null) {
            return;
        }
        treeModel.nodeStructureChanged(treeSource);
        treeModel.nodeStructureChanged(treeHeaders);
        treeModel.nodeStructureChanged(treeLibraries);
        treeModel.nodeStructureChanged(treeOutput);
        treeModel.nodeStructureChanged(treeDocs);
        treeModel.nodeStructureChanged(treeBinaries);
        restoreTreeState(sketchContentTree, saved);
    }

    public void updateHeadersTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TreePath[] saved = saveTreeState(sketchContentTree);
                if (saved == null) {
                    return;
                }
                treeHeaders.removeAllChildren();
                DefaultMutableTreeNode node;

                for(File f : loadedSketch.sketchFiles) {
                    int type = FileType.getType(f);

                    switch(type) {
                        case FileType.HEADER:
                            node = new DefaultMutableTreeNode(f.getName());
                            node.setUserObject(f);
                            treeHeaders.add(node);
                            break;
                    }
                }

                treeModel.nodeStructureChanged(treeHeaders);
                restoreTreeState(sketchContentTree, saved);
            }
        });
    }

    public synchronized void updateLibrariesTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TreePath[] saved = saveTreeState(sketchContentTree);
                if (saved == null) {
                    return;
                }
//                treeLibraries.removeAllChildren();
                HashMap<String, Library>libList = loadedSketch.getLibraries();
                
                DefaultMutableTreeNode ntc = new DefaultMutableTreeNode();
                DefaultMutableTreeNode node;

                if(libList != null) {
                    ArrayList<String> libs = new ArrayList<String>();
                    libs.addAll(libList.keySet());
                    for(String libname : libs) {
                        node = new DefaultMutableTreeNode(libname);
                        node.setUserObject(libList.get(libname));
                        ntc.add(node);
                    }
                }

                boolean hasChanged = mergeTrees(treeLibraries, ntc);
                treeModel.reload(sortTree(treeLibraries));
                restoreTreeState(sketchContentTree, saved);
                if (hasChanged) {
                    populateOptionsMenu(optionsMenu);
                    optionsMenu.setEnabled(optionsMenu.getItemCount() > 0);
                }
            }
        });
    }

    public void updateBinariesTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TreePath[] saved = saveTreeState(sketchContentTree);
                if (saved == null) {
                    return;
                }
                treeBinaries.removeAllChildren();
                File bins = loadedSketch.getBinariesFolder();
                DefaultMutableTreeNode node;

                if(bins.exists() && bins.isDirectory()) {
                    File[] files = bins.listFiles();

                    for(File binFile : files) {
                        if(binFile.getName().startsWith(".")) {
                            continue;
                        }

                        node = new DefaultMutableTreeNode(binFile.getName());
                        node.setUserObject(binFile);
                        treeBinaries.add(node);
                    }
                }

        treeModel.nodeStructureChanged(treeBinaries);
                restoreTreeState(sketchContentTree, saved);
            }
        });
    }

    public void updateOutputTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TreePath[] saved = saveTreeState(sketchContentTree);
                if (saved == null) {
                    return;
                }
                treeOutput.removeAllChildren();
                addFileTreeToNode(treeOutput, loadedSketch.getBuildFolder());
                treeModel.nodeStructureChanged(treeOutput);


                restoreTreeState(sketchContentTree, saved);
            }
        });
    }

    public void updateFilesTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TreePath[] saved = saveTreeState(sketchFilesTree);
                if (saved == null) {
                    return;
                }
                filesTreeRoot.removeAllChildren();
                filesTreeRoot.setUserObject(loadedSketch.getFolder());
                addFileTreeToNode(filesTreeRoot, loadedSketch.getFolder());
                filesTreeModel.nodeStructureChanged(filesTreeRoot);
                restoreTreeState(sketchFilesTree, saved);
            }
        });
    }

    public void updateTree() {

        treeRoot.setUserObject(loadedSketch);

        updateSourceTree();
        updateHeadersTree();
        updateLibrariesTree();
        updateBinariesTree();
        updateOutputTree();
        updateDocsTree();
        updateFilesTree();
    }

    // This is the main routine for generating the context menus for the Project tree view.  For the
    // Files tree view see below.

    public class TreeMouseListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            ActionListener createNewAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    createNewSketchFile(e.getActionCommand());
                }
            };
            ActionListener importFileAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    importFile(e.getActionCommand());
                }
            };

            // Any click of any button will select whatever node is under the mouse at the time.
            int selRow = sketchContentTree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = sketchContentTree.getPathForLocation(e.getX(), e.getY());
            sketchContentTree.setSelectionPath(selPath);

            if(selPath == null) {
                return;
            }

            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)selPath.getLastPathComponent();
            Object userObject = selectedNode.getUserObject();

            if(e.getButton() == 1) {
                if(e.getClickCount() == 2) {
                    if(userObject instanceof FunctionBookmark) {
                        FunctionBookmark bm = (FunctionBookmark)userObject;
                        int tab = openOrSelectFile(bm.getFile());
                        EditorBase eb = getTab(tab);
                        eb.gotoLine(bm.getLine());
                        eb.requestFocus();
                    } else if (userObject instanceof TodoEntry) {
                        TodoEntry ent = (TodoEntry)userObject;
                        int tab = openOrSelectFile(ent.getFile());
                        EditorBase eb = getTab(tab);
                        eb.gotoLine(ent.getLine());
                        eb.requestFocus();
                    }
                }

            } else if(e.getButton() == 3) {
                // Now handle just the right mouse button.
                JPopupMenu menu = new JPopupMenu();
                DefaultMutableTreeNode o = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                DefaultMutableTreeNode p = (DefaultMutableTreeNode)o.getParent();

                // If the user object is a string then it must be one of the logical groups.  Work out
                // which one by what the string says.

                if(o.getUserObject() instanceof String) {
                    String s = (String)o.getUserObject();

                    if(s.equals(loadedSketch.getName())) {
                        JMenuItem openInOS = new JMenuItem("Open in OS");
                        openInOS.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                Base.openURL(loadedSketch.getFolder().getAbsolutePath());
                            }
                        });
                        Base.setFont(openInOS, "menu.entry");
                        menu.add(openInOS);

                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals(Base.i18n.string("tree.source"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.create.ino"));
                        item.setActionCommand("ino");
                        item.addActionListener(createNewAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.create.cpp"));
                        item.setActionCommand("cpp");
                        item.addActionListener(createNewAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.create.c"));
                        item.setActionCommand("c");
                        item.addActionListener(createNewAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.create.asm"));
                        item.setActionCommand("S");
                        item.addActionListener(createNewAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.import.source"));
                        item.setActionCommand("source");
                        item.addActionListener(importFileAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);

                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals(Base.i18n.string("tree.headers"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.create.header"));
                        item.setActionCommand("h");
                        item.addActionListener(createNewAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.import.header"));
                        item.setActionCommand("header");
                        item.addActionListener(importFileAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);

                        populateContextMenu(menu, Plugin.MENU_TREE_HEADERS | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_HEADERS | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_HEADERS | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals("Libraries")) {
                        populateContextMenu(menu, Plugin.MENU_TREE_LIBRARIES | Plugin.MENU_TOP, o);

                        populateLibrariesMenu(menu);

                        populateContextMenu(menu, Plugin.MENU_TREE_LIBRARIES | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_LIBRARIES | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals(Base.i18n.string("tree.binaries"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.import.binary"));
                        item.setActionCommand("binary");
                        item.addActionListener(importFileAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);

                        populateContextMenu(menu, Plugin.MENU_TREE_BINARIES | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_BINARIES | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_BINARIES | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals(Base.i18n.string("tree.output"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.purge.output"));
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                loadedSketch.purgeBuildFiles();
                                updateOutputTree();
                            }
                        });
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_BOTTOM, o);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals(Base.i18n.string("tree.docs"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.create.markdown"));
                        item.setActionCommand("md");
                        item.addActionListener(createNewAction);
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_BOTTOM, o);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    }

                    // Otherwise, if the user object is a File, then it must be one of the entries in one
                    // of the groups, except the libraries group.
                } else if(o.getUserObject() instanceof File) {
                    File thisFile = (File)o.getUserObject();

                    String ee = Preferences.get("editor.external.command");

                    if(ee != null && !ee.equals("")) {
                        JMenuItem openExternal = new JMenuItem(Base.i18n.string("menu.file.external"));
                        openExternal.setActionCommand(thisFile.getAbsolutePath());
                        openExternal.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                String cmd = Preferences.get("editor.external.command");
                                String fn = e.getActionCommand();
                                loadedSketch.getContext().set("filename", fn);
                                String c = loadedSketch.getContext().parseString(cmd);
                                Base.exec(c.split("::"));
                            }
                        });
                        Base.setFont(openExternal, "menu.entry");
                        menu.add(openExternal);
                    }

                    JMenuItem renameItem = new JMenuItem(Base.i18n.string("menu.file.rename"));
                    renameItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();

                            if(f.equals(mf)) {
                                alert(Base.i18n.string("err.mainfile.norename"));
                                return;
                            }

                            String newName = askForFilename(f.getName());

                            if(newName != null) {
                                File newFile = new File(f.getParentFile(), newName);
                                int tab = getTabByFile(f);
                                loadedSketch.renameFile(f, newFile);
                                f.renameTo(newFile);

                                if(tab >= 0) {
                                    setTabFile(tab, newFile);
                                }

                                updateTree();
                            }
                        }
                    });
                    renameItem.setActionCommand(thisFile.getAbsolutePath());
                    Base.setFont(renameItem, "menu.entry");
                    menu.add(renameItem);

                    JMenuItem deleteItem = new JMenuItem(Base.i18n.string("menu.file.delete"));
                    deleteItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();

                            if(f.equals(mf)) {
                                alert(Base.i18n.string("err.mainfile.nodelete"));
                                return;
                            }

                            if(twoOptionBox(
                                JOptionPane.QUESTION_MESSAGE, 
                                Base.i18n.string("msg.file.delete.title"),
                                Base.i18n.string("msg.file.delete.body", f.getName()),
                                Base.i18n.string("misc.yes"),
                                Base.i18n.string("misc.no")
                            ) == 0) {

                                loadedSketch.deleteFile(f);
                                int tab = getTabByFile(f);

                                if(f.isDirectory()) {
                                    Base.removeDir(f);
                                } else {
                                    f.delete();
                                }

                                if(tab >= 0) {
                                    editorTabs.remove(tab);
                                }

                                updateTree();
                            }
                        }
                    });
                    deleteItem.setActionCommand(thisFile.getAbsolutePath());
                    Base.setFont(deleteItem, "menu.entry");
                    menu.add(deleteItem);

                    populateContextMenu(menu, Plugin.MENU_TREE_FILE | Plugin.MENU_TOP, o);
                    menu.addSeparator();

                    JMenu infoMenu = new JMenu(Base.i18n.string("menu.file.info"));
                    JMenuItem filePath = new JMenuItem(thisFile.getAbsolutePath());
                    filePath.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            StringSelection sel = new StringSelection(e.getActionCommand());
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(sel, sel);
                            message(Base.i18n.string("msg.file.info.copied"));
                        }
                    });
                    filePath.setActionCommand(thisFile.getAbsolutePath());
                    infoMenu.add(filePath);
                    JMenuItem fileSize = new JMenuItem(Base.i18n.string("menu.file.info.size", thisFile.length()));
                    infoMenu.add(fileSize);
                    Base.setFont(infoMenu, "menu.entry");
                    menu.add(infoMenu);

                    populateContextMenu(menu, Plugin.MENU_TREE_FILE | Plugin.MENU_MID, o);

                    if(p.getUserObject() instanceof String) {
                        String ptext = (String)p.getUserObject();

                        if(ptext.equals(Base.i18n.string("tree.binaries"))) {
                            JMenuItem insertRef = new JMenuItem(Base.i18n.string("menu.file.reference"));
                            insertRef.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    String filename = e.getActionCommand();
                                    filename = filename.replaceAll("\\.", "_");
                                    int at = getActiveTab();

                                    if(at > -1) {
                                        EditorBase eb = getTab(at);
                                        eb.insertAtCursor("extern const char " + filename + "[] asm(\"_binary_objects_" + filename + "_start\");\n");
                                    }
                                }
                            });
                            insertRef.setActionCommand(thisFile.getName());
                            Base.setFont(insertRef, "menu.entry");
                            menu.add(insertRef);
                        }
                    }

                    populateContextMenu(menu, Plugin.MENU_TREE_FILE | Plugin.MENU_BOTTOM, o);

                    menu.show(sketchContentTree, e.getX(), e.getY());

                    // Otherwise it might be a library.  This generates the menu entries for a
                    // library object.

                } else if(o.getUserObject() instanceof Library) {
                    final Library lib = (Library)(o.getUserObject());
                    JMenuItem item = new JMenuItem(Base.i18n.string("menu.library.purge"));
                    item.setEnabled(loadedSketch.libraryIsCompiled(lib));
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            loadedSketch.purgeLibrary(lib);
                            updateLibrariesTree();
                        }
                    });
                    Base.setFont(item, "menu.entry");
                    menu.add(item);
                    item = new JMenuItem(Base.i18n.string("menu.library.recompile"));
                    item.setEnabled(!compilerRunning());
                    item.setActionCommand("yes");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            loadedSketch.purgeLibrary(lib);
                            clearConsole();

                            if(compilerRunning()) {
                                error(Base.i18n.string("err.compilerrunning"));
                                return;
                            }

                            LibCompileRunHandler runHandler = new LibCompileRunHandler(lib);
                            compilationThread = new Thread(runHandler, "Compiler");
                            compilationThread.start();
                        }
                    });
                    Base.setFont(item, "menu.entry");
                    menu.add(item);

                    if(lib.isLocal(loadedSketch.getFolder())) {
                        item = new JMenuItem(Base.i18n.string("menu.library.export"));
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                exportLocalLibrary(lib);
                            }
                        });
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                    } else {
                        item = new JMenuItem(Base.i18n.string("menu.library.localize"));
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                if(loadedSketch.parentIsProtected()) {
                                    error(Base.i18n.string("err.library.localize.example"));
                                    return;
                                }

                                File libs = new File(loadedSketch.getFolder(), "libraries");
                                libs.mkdirs();
                                File newLibDir = new File(libs, lib.getName());
                                Base.copyDir(lib.getFolder(), newLibDir);
                                loadedSketch.purgeLibrary(lib);
                                updateTree();
                            }
                        });
                        Base.setFont(item, "menu.entry");
                        menu.add(item);
                    }

                    item = new JMenuItem(Base.i18n.string("menu.library.open"));
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Base.openFolder(lib.getFolder());
                        }
                    });
                    menu.add(item);

                    menu.show(sketchContentTree, e.getX(), e.getY());
                }

                return;
            }

            if(selRow != -1) {
                if(e.getClickCount() == 2) {
                    DefaultMutableTreeNode o = (DefaultMutableTreeNode)selPath.getLastPathComponent();

                    if(o.getUserObject() instanceof File) {
                        File sf = (File)o.getUserObject();
                        openOrSelectFile(sf);
                    }
                }
            }
        }
    }

    public class FileTreeMouseListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            ActionListener createNewAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // createNewAnyFile(e.getActionCommand());
                }
            };
            ActionListener importFileAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // importAnyFile(e.getActionCommand());
                }
            };

            int selRow = sketchFilesTree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = sketchFilesTree.getPathForLocation(e.getX(), e.getY());
            sketchFilesTree.setSelectionPath(selPath);

            if(e.getButton() == 3) {
                DefaultMutableTreeNode o = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                DefaultMutableTreeNode p = (DefaultMutableTreeNode)o.getParent();

                if(o.getUserObject() instanceof File) {
                    File thisFile = (File)o.getUserObject();
                    JPopupMenu menu = new JPopupMenu();

                    if(thisFile.isDirectory()) {
                        JMenuItem openInOS = new JMenuItem(Base.i18n.string("menu.dir.open"));
                        openInOS.setActionCommand(thisFile.getAbsolutePath());
                        openInOS.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                Base.openURL(e.getActionCommand());
                            }
                        });
                        Base.setFont(openInOS, "menu.entry");
                        menu.add(openInOS);

                        JMenuItem mkdirItem = new JMenuItem(Base.i18n.string("menu.dir.create"));
                        mkdirItem.setActionCommand(thisFile.getAbsolutePath());
                        mkdirItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                String fn = e.getActionCommand();
                                File f = new File(fn);
                                String newName = askForFilename(null);

                                if(newName != null) {
                                    File newFile = new File(f, newName);
                                    newFile.mkdirs();
                                    updateTree();
                                }
                            }
                        });
                        Base.setFont(mkdirItem, "menu.entry");
                        menu.add(mkdirItem);
                        JMenuItem unzipItem = new JMenuItem(Base.i18n.string("menu.dir.unzip"));
                        unzipItem.setActionCommand(thisFile.getAbsolutePath());
                        unzipItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                findAndUnzipZipFile(e.getActionCommand());
                            }
                        });
                        Base.setFont(unzipItem, "menu.entry");
                        menu.add(unzipItem);
                    } else {
                        String ee = Preferences.get("editor.external.command");

                        if(ee != null && !ee.equals("")) {
                            JMenuItem openExternal = new JMenuItem(Base.i18n.string("menu.file.external"));
                            openExternal.setActionCommand(thisFile.getAbsolutePath());
                            openExternal.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    String cmd = Preferences.get("editor.external.command");
                                    String fn = e.getActionCommand();
                                    loadedSketch.getContext().set("filename", fn);
                                    String c = loadedSketch.getContext().parseString(cmd);
                                    Base.exec(c.split("::"));
                                }
                            });
                            Base.setFont(openExternal, "menu.entry");
                            menu.add(openExternal);
                        }
                    }

                    populateContextMenu(menu, Plugin.MENU_FILE_FILE | Plugin.MENU_TOP, o);

                    JMenuItem renameItem = new JMenuItem(Base.i18n.string("menu.file.rename"));
                    renameItem.setActionCommand(thisFile.getAbsolutePath());
                    renameItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();

                            if(f.equals(mf)) {
                                alert(Base.i18n.string("err.mainfile.norename"));
                                return;
                            }

                            String newName = askForFilename(f.getName());

                            if(newName != null) {
                                File newFile = new File(f.getParentFile(), newName);
                                int tab = getTabByFile(f);
                                loadedSketch.renameFile(f, newFile);
                                f.renameTo(newFile);

                                if(tab >= 0) {
                                    setTabFile(tab, newFile);
                                }

                                updateTree();
                            }
                        }
                    });
                    Base.setFont(renameItem, "menu.entry");
                    menu.add(renameItem);

                    JMenuItem deleteItem = new JMenuItem(Base.i18n.string("menu.file.delete"));
                    deleteItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();

                            if(f.equals(mf)) {
                                alert(Base.i18n.string("err.mainfile.nodelete"));
                                return;
                            }

                            if(twoOptionBox(
                                JOptionPane.QUESTION_MESSAGE, 
                                Base.i18n.string("msg.file.delete.title"),
                                Base.i18n.string("msg.file.delete.body", f.getName()),
                                Base.i18n.string("misc.yes"),
                                Base.i18n.string("misc.no")
                            ) == 0) {
                                loadedSketch.deleteFile(f);
                                int tab = getTabByFile(f);

                                if(f.isDirectory()) {
                                    Base.removeDir(f);
                                } else {
                                    f.delete();
                                }

                                if(tab >= 0) {
                                    editorTabs.remove(tab);
                                }

                                updateTree();
                            }
                        }
                    });
                    deleteItem.setActionCommand(thisFile.getAbsolutePath());

                    Base.setFont(deleteItem, "menu.entry");
                    menu.add(deleteItem);

                    menu.addSeparator();
                    populateContextMenu(menu, Plugin.MENU_FILE_FILE | Plugin.MENU_MID, o);

                    JMenu infoMenu = new JMenu(Base.i18n.string("menu.file.info"));
                    JMenuItem filePath = new JMenuItem(thisFile.getAbsolutePath());
                    filePath.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            StringSelection sel = new StringSelection(e.getActionCommand());
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(sel, sel);
                            message(Base.i18n.string("msg.file.info.copied"));
                        }
                    });
                    filePath.setActionCommand(thisFile.getAbsolutePath());
                    infoMenu.add(filePath);
                    JMenuItem fileSize = new JMenuItem(Base.i18n.string("menu.file.info.size", thisFile.length()));
                    infoMenu.add(fileSize);
                    Base.setFont(infoMenu, "menu.entry");
                    menu.add(infoMenu);

                    menu.addSeparator();
                    populateContextMenu(menu, Plugin.MENU_FILE_FILE | Plugin.MENU_BOTTOM, o);
                    menu.show(sketchFilesTree, e.getX(), e.getY());
                }

                return;
            }

            if(selRow != -1) {
                if(e.getClickCount() == 2) {
                    DefaultMutableTreeNode o = (DefaultMutableTreeNode)selPath.getLastPathComponent();

                    if(o.getUserObject() instanceof File) {
                        File sf = (File)o.getUserObject();
                        String cl = FileType.getEditor(sf);

                        if(cl != null) {
                            openOrSelectFile(sf);
                        }
                    }
                }
            }
        }
    }
    public Board getBoard() {
        if(loadedSketch == null) {
            return null;
        }

        return loadedSketch.getContext().getBoard();
    }

    public Core getCore() {
        if(loadedSketch == null) {
            return null;
        }

        return loadedSketch.getContext().getCore();
    }

    public Compiler getCompiler() {
        if(loadedSketch == null) {
            return null;
        }

        return loadedSketch.getContext().getCompiler();
    }

    public String getSketchName() {
        return loadedSketch.getName();
    }

    public Sketch getSketch() {
        return loadedSketch;
    }


    String mBuffer = "";
    public void messageStream(String msg) {
        console.append(msg, Console.BODY);
/*
        mBuffer += msg;
        int nlpos = mBuffer.lastIndexOf("\n");

        if(nlpos == -1) {
            return;
        }

        boolean eol = false;

        if(mBuffer.endsWith("\n")) {
            mBuffer = mBuffer.substring(0, mBuffer.length() - 1);
            eol = true;
        }

        String[] bits = mBuffer.split("\n");

        for(int i = 0; i < bits.length - 1; i++) {
            message(bits[i]);
        }

        if(eol) {
            mBuffer = "";
            message(bits[bits.length - 1]);
        } else {
            mBuffer = bits[bits.length - 1];
        }
*/
    }

    String wBuffer = "";
    public void warningStream(String msg) {
        console.append(msg, Console.WARNING);
/*
        wBuffer += msg;
        int nlpos = wBuffer.lastIndexOf("\n");

        if(nlpos == -1) {
            return;
        }

        boolean eol = false;

        if(wBuffer.endsWith("\n")) {
            wBuffer = wBuffer.substring(0, wBuffer.length() - 1);
            eol = true;
        }

        String[] bits = wBuffer.split("\n");

        for(int i = 0; i < bits.length - 1; i++) {
            warning(bits[i]);
        }

        if(eol) {
            wBuffer = "";
            warning(bits[bits.length - 1]);
        } else {
            wBuffer = bits[bits.length - 1];
        }
*/
    }

    String eBuffer = "";
    public void errorStream(String msg) {
        console.append(msg, Console.ERROR);
/*
        eBuffer += msg;
        int nlpos = eBuffer.lastIndexOf("\n");

        if(nlpos == -1) {
            return;
        }

        boolean eol = false;

        if(eBuffer.endsWith("\n")) {
            eBuffer = eBuffer.substring(0, eBuffer.length() - 1);
            eol = true;
        }

        String[] bits = eBuffer.split("\n");

        for(int i = 0; i < bits.length - 1; i++) {
            error(bits[i]);
        }

        if(eol) {
            eBuffer = "";
            error(bits[bits.length - 1]);
        } else {
            eBuffer = bits[bits.length - 1];
        }
*/
    }


    public void appendToConsole(String s) {
        console.append(s, Console.BODY);
    }
    
    public void link(String msg) {
        Debug.message(msg);

        if(msg == null) {
            return;
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.LINK);
    }

    public void parsedMessage(String msg) {
        console.appendParsed(msg);
    }

    public void command(String msg) {
        Debug.message(msg);

        if(msg == null) {
            return;
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        output.append(msg, Console.COMMAND);
    }

    public void heading(String msg) {
        Debug.message(msg);

        if(msg == null) {
            return;
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.HEADING);
    }

    public void bullet(String msg) {
        Debug.message(msg);

        if(msg == null) {
            return;
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.BULLET);
    }

    public void bullet2(String msg) {
        Debug.message(msg);

        if(msg == null) {
            return;
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.BULLET2);
    }

    public void bullet3(String msg) {
        Debug.message(msg);

        if(msg == null) {
            return;
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.BULLET3);
    }

    public void message(String msg) {
        Debug.message(msg);

        if(msg == null) {
            return;
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.BODY);
    }

    public void warning(String msg) {
        Debug.message(msg);

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.WARNING);
    }

    public void error(String msg) {
        Debug.message(msg);

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.ERROR);
    }

    public void error(Throwable e) {
        error(e.getMessage());
//        StringWriter sw = new StringWriter();
//        PrintWriter pw = new PrintWriter(sw);
//        e.printStackTrace(pw);
//        error(sw.toString());
    }


    public void clearConsole() {
        console.clear();
        output.clear();
    }

    public void setProgress(int x) {
        statusProgress.setValue(x);
    }

    public void setStatus() {
        StringBuilder sb = new StringBuilder();


        statusText.setText("<html>" + Base.i18n.string("status.html",
            loadedSketch.getContext().getBoard() != null ? loadedSketch.getContext().getBoard() : "None",
            loadedSketch.getContext().getCore() != null ? loadedSketch.getContext().getCore() : "None",
            loadedSketch.getContext().getCompiler() != null ? loadedSketch.getContext().getCompiler() : "None",
            loadedSketch.getDevice() != null ? loadedSketch.getDevice().getName() : "None"

        ) + "</html>");
    }

    public int openOrSelectFile(File sf) {
        int existing = getTabByFile(sf);

        if(existing == -1) {
            return openNewTab(sf);
        } else {
            selectTab(existing);
            return existing;
        }
    }

    public static JButton addToolbarButton(JToolBar tb, String path, String tooltip) {
        return addToolbarButton(tb, path, tooltip, (ActionListener)null);
    }

    public static JButton addToolbarButton(JToolBar tb, String path, String tooltip, ActionListener al) {
        ImageIcon buttonIcon = Base.loadIconFromResource(path);
        JButton button = new JButton(buttonIcon);
        button.setToolTipText(tooltip);

        if(al != null) {
            button.addActionListener(al);
        }

        tb.add(button);
        return button;
    }

    public static JButton addToolbarButton(JToolBar tb, String cat, String name, String tooltip) {
        return addToolbarButton(tb, cat, name, tooltip, (ActionListener)null);
    }

    public static JButton addToolbarButton(JToolBar tb, String cat, String name, String tooltip, ActionListener al) {
        ImageIcon buttonIcon = Base.getIcon(cat, name, 24);
        JButton button = new JButton(buttonIcon);
        button.setToolTipText(tooltip);

        if(al != null) {
            button.addActionListener(al);
        }

        tb.add(button);
        return button;
    }

    public boolean closeTab(Component c) {
        System.err.println(c.getClass());
        return false;
    }

    public boolean closeTab(int tab) {
        if(tab == -1) return false;

        if(editorTabs.getComponentAt(tab) instanceof EditorBase) {
            EditorBase eb = (EditorBase)editorTabs.getComponentAt(tab);

            if(eb.isModified()) {
                int option = threeOptionBox(
                                 JOptionPane.WARNING_MESSAGE,
                                 Base.i18n.string("msg.unsaved.title"),
                                 Base.i18n.string("msg.unsaved.body"),
                                 Base.i18n.string("misc.yes"),
                                 Base.i18n.string("misc.no"),
                                 Base.i18n.string("misc.cancel")
                             );

                if(option == 2) return false;

                if(option == 0) eb.save();
            }

        }

        editorTabs.remove(tab);
        return true;
    }

    public int openNewTab(File sf) {
        if (sf == null) {
            error("No file specified");
            return -1;
        }
        if(sf.exists() == false) {
            error(Base.i18n.string("err.notfound", sf.getName()));
            return -1;
        }

        try {
            String className = FileType.getEditor(sf.getName());

            if(className == null) {
                error(Base.i18n.string("err.badfile", sf.getName()));
                return -1;
            }

            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            Class<?> edClass = Class.forName(className);
            Constructor<?> edConst = edClass.getConstructor(Sketch.class, File.class, Editor.class);

            EditorBase ed = (EditorBase)edConst.newInstance(loadedSketch, sf, this);

            editorTabs.addTab(sf.getName(), (JPanel) ed);
            int tabno = editorTabs.getTabCount() - 1;

            TabLabel lab = new TabLabel(this, sf);
            editorTabs.setTabComponentAt(tabno, lab);

            selectTab(tabno);
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            ed.requestFocus();
            return tabno;
        } catch(Exception e) {
            Base.error(e);
        }

        return -1;
    }

    public void attachPanelAsTab(String name, JPanel panel) {
        TabLabel lab = new TabLabel(this, name);
        editorTabs.addTab(name, panel);
        int tabno = editorTabs.getTabCount() - 1;
        editorTabs.setTabComponentAt(tabno, lab);
        selectTab(tabno);
    }

    public EditorBase getTab(int i) {
        if(editorTabs.getComponentAt(i) instanceof EditorBase) {
            return (EditorBase)editorTabs.getComponentAt(i);
        } else {
            return null;
        }
    }

    public TabLabel getTabLabel(int i) {
        return (TabLabel)editorTabs.getTabComponentAt(i);
    }

    public String getTabName(int i) {
        return editorTabs.getTitleAt(i);
    }

    public TabLabel getSelectedTab() {
        int sel = editorTabs.getSelectedIndex();
        if (sel < 0) {
            return null;
        }
        return getTabLabel(sel);
    }

    public EditorBase getSelectedEditor() {
        int sel = editorTabs.getSelectedIndex();
        if (sel < 0) {
            return null;
        }
        return getTab(sel);
    }

    public String getSelectedTabName() {
        return getTabName(editorTabs.getSelectedIndex());
    }

    public void setTabName(int i, String name) {
        editorTabs.setTitleAt(i, name);
    }

    public void setTabFile(int i, File f) {
        TabLabel tl = getTabLabel(i);
        tl.setFile(f);
    }

    public int getTabCount() {
        if (editorTabs == null) {
            return 0;
        }
        return editorTabs.getTabCount();
    }

    public void setTabModified(int t, boolean m) {
        TabLabel tl = getTabLabel(t);

        if(tl != null) {
            tl.setModified(m);
        }
    }

    public int getTabByFile(File f) {
        if (f == null) {
            return -1;
        }
        if (editorTabs == null) {
            return -1;
        }
        for(int i = 0; i < editorTabs.getTabCount(); i++) {
            TabLabel l = (TabLabel)editorTabs.getTabComponentAt(i);
            if (l == null) {
                continue;
            }
            File cf = l.getFile();

            if(cf != null && cf.equals(f)) {
                return i;
            }
        }

        return -1;
    }

    public int getTabByLabel(TabLabel lab) {
        for(int i = 0; i < editorTabs.getTabCount(); i++) {
            TabLabel l = (TabLabel)editorTabs.getTabComponentAt(i);

            if(l == lab) {
                return i;
            }
        }

        return -1;
    }

    public int getActiveTab() {
        return editorTabs.getSelectedIndex();
    }

    public int getTabByEditor(EditorBase eb) {
        return editorTabs.indexOfComponent((Component)eb);
    }

    public void selectTab(int tab) {
        TabLabel tl = (TabLabel)editorTabs.getTabComponentAt(tab);

        if(tl == null) {
            return;
        }

        editorTabs.setSelectedIndex(tab);
    }

    public boolean isModified() {
        for(int i = 0; i < editorTabs.getTabCount(); i++) {
            if(editorTabs.getComponentAt(i) instanceof EditorBase) {
                EditorBase eb = (EditorBase)editorTabs.getComponentAt(i);

                if(eb.isModified()) {
                    return true;
                }
            }
        }

        return false;
    }

    public void saveAllTabs() {
        if(loadedSketch.isUntitled()) {
            saveAs();
            return;
        }

        if(loadedSketch.parentIsProtected()) {
            saveAs();
            return;
        }

        for(int i = 0; i < editorTabs.getTabCount(); i++) {
            if(editorTabs.getComponentAt(i) instanceof EditorBase) {
                EditorBase eb = (EditorBase)editorTabs.getComponentAt(i);

                if(eb.isModified()) {
                    eb.save();
                }
            }
        }

        refreshTreeModel();
    }

    public boolean closeAllTabs() {
        while(editorTabs.getTabCount() > 0) {
            if(!closeTab(0)) {
                return false;
            }
        }
        return true;
    }

    public void alert(String message) {
        JOptionPane.showMessageDialog(this, message, Base.i18n.string("misc.alert"), JOptionPane.WARNING_MESSAGE);
    }

    public int twoOptionBox(int type, String title, String message, String option0, String option1) {
        Object[] options = { option0, option1 };
        return JOptionPane.showOptionDialog(this, message, title, JOptionPane.YES_NO_OPTION, type, null, options, options[1]);
    }

    public int threeOptionBox(int type, String title, String message, String option0, String option1, String option2) {
        Object[] options = { option0, option1, option2 };
        return JOptionPane.showOptionDialog(this, message, title, JOptionPane.YES_NO_CANCEL_OPTION, type, null, options, options[2]);
    }

    public boolean askCloseWindow() {

        Preferences.save();

        if(editorList.size() == 1) {
            if(Base.isMacOS()) {
                Object[] options = { Base.i18n.string("misc.yes"), Base.i18n.string("misc.cancel") };

                int result = JOptionPane.showOptionDialog(this,
                             Base.i18n.string("msg.quit.body"),
                             Base.i18n.string("msg.quit.title"),
                             JOptionPane.YES_NO_OPTION,
                             JOptionPane.QUESTION_MESSAGE,
                             null,
                             options,
                             options[0]);

                if(result == JOptionPane.NO_OPTION ||
                        result == JOptionPane.CLOSED_OPTION
                  ) {
                    return false;
                }
            }
        }

        if (!closeAllTabs()) {
            return false;
        }

        Editor.unregisterEditor(this);
        this.dispose();

        if(Editor.shouldQuit()) {
            Preferences.save();
            System.exit(0);
        }

        return true;
    }

    public void addSketchesFromFolder(JMenu menu, File folder) {
        if(folder == null) return;

        if(menu == null) return;

        if(!folder.exists()) return;

        File[] files = folder.listFiles();
        Arrays.sort(files);

        for(File file : files) {
            if(file.isDirectory()) {
                File testFile1 = new File(file, file.getName() + ".ino");
                File testFile2 = new File(file, file.getName() + ".pde");

                if(testFile1.exists() || testFile2.exists()) {
                    JMenuItem item = new JMenuItem(file.getName());
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String path = e.getActionCommand();

                            if(new File(path).exists()) {
                                loadSketch(path);
                            } else {
                                error(Base.i18n.string("err.notfound", path));
                            }
                        }
                    });
                    item.setActionCommand(file.getAbsolutePath());
                    Base.setFont(item, "menu.entry");
                    menu.add(item);
                } else {
                    JMenu submenu = new JMenu(file.getName());
                    addSketchesFromFolder(submenu, file);

                    if(submenu.getItemCount() > 0) {
                        Base.setFont(submenu, "menu.entry");
                        menu.add(submenu);
                    }
                }
            }
        }
    }

    public JMenuItem createMenuEntry(String name, int shortcut, int mods, ActionListener action) {
        return createMenuEntry(name, shortcut, mods, action, null);
    }

    public JMenuItem createMenuEntry(String name, int shortcut, int mods, ActionListener action, String command) {
        JMenuItem menuItem = new JMenuItem(name);
        Base.setFont(menuItem, "menu.entry");
        if (action != null) {
            menuItem.addActionListener(action);
        }
        if (command != null) {
            menuItem.setActionCommand(command);
        }
        if (shortcut != 0) {
            int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
            menuItem.setAccelerator(KeyStroke.getKeyStroke(shortcut, modifiers | mods));
        }
        return menuItem;
    }

    public void updateMenus() {
        fileMenu.removeAll();
        editMenu.removeAll();
        sketchMenu.removeAll();
        hardwareMenu.removeAll();
        toolsMenu.removeAll();
        helpMenu.removeAll();

        JMenu submenu;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.new"), KeyEvent.VK_N, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleNew();
            }
        }));

        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.open"), KeyEvent.VK_O, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleOpenPrompt();
            }
        }));

        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_TOP);

        JMenu recentSketchesMenu = new JMenu(Base.i18n.string("menu.file.recent"));
        Base.setFont(recentSketchesMenu, "menu.entry");
        fileMenu.add(recentSketchesMenu);

        for(File m : Base.MRUList) {
            JMenuItem recentitem = createMenuEntry(m.getName(), 0, 0, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String path = e.getActionCommand();

                    if(new File(path).exists()) {
                        loadSketch(path);
                    } else {
                        error(Base.i18n.string("err.notfound", path));
                    }
                }
            });

            recentitem.setToolTipText(m.getAbsolutePath());
            recentitem.setActionCommand(m.getAbsolutePath());
            recentSketchesMenu.add(recentitem);
        }

        JMenu frequentSketchesMenu = new JMenu(Base.i18n.string("menu.file.frequent"));
        Base.setFont(frequentSketchesMenu, "menu.entry");
        fileMenu.add(frequentSketchesMenu);

        ArrayList<Integer> tmpArr = new ArrayList<Integer>();
        for (Integer m : Base.MCUList.values()) {
            if (tmpArr.indexOf(m) == -1) {
                tmpArr.add(m);
            }
        }

        Integer[] vals = tmpArr.toArray(new Integer[0]);
        Arrays.sort(vals);

        for (Integer hits : vals) {
            for(File m : Base.MCUList.keySet()) {
                if (Base.MCUList.get(m) == hits) {
                    JMenuItem recentitem = createMenuEntry(m.getName(), 0, 0, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String path = e.getActionCommand();

                            if(new File(path).exists()) {
                                loadSketch(path);
                            } else {
                                error(Base.i18n.string("err.notfound", path));
                            }
                        }
                    });

                    recentitem.setToolTipText(m.getAbsolutePath());
                    recentitem.setActionCommand(m.getAbsolutePath());
                    frequentSketchesMenu.add(recentitem, 0);
                }
            }
        }


        JMenu examplesMenu = new JMenu(Base.i18n.string("menu.file.examples"));

        JMenuItem emBrowse = new JMenuItem(Base.i18n.string("menu.file.examples.browse"));
        emBrowse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ExampleBrowser eb = new ExampleBrowser(Editor.this);
            }
        });
        examplesMenu.add(emBrowse);
        examplesMenu.addSeparator();


        if(loadedSketch.getContext().getCore() != null) {
            if (loadedSketch.getContext().getCompiler() != null) {
                addSketchesFromFolder(examplesMenu, loadedSketch.getContext().getCompiler().getExamplesFolder());
            }
            if (loadedSketch.getContext().getCore() != null) {
                addSketchesFromFolder(examplesMenu, loadedSketch.getContext().getCore().getExamplesFolder());
            }
            if (loadedSketch.getContext().getBoard() != null) {
                addSketchesFromFolder(examplesMenu, loadedSketch.getContext().getBoard().getExamplesFolder());
            }

            examplesMenu.addSeparator();


            TreeSet<String> catNames = Library.getLibraryCategories();

            if (loadedSketch.getContext().getCore() != null) {
                for(String group : catNames) {
                    TreeSet<Library>libs = Library.getLibraries(group, loadedSketch.getContext().getCore().getName());

                    if (libs == null) {
                        libs = new TreeSet<Library>();
                    }

                    if (loadedSketch.getContext().getCore().get("core.alias") != null) {
                        TreeSet<Library>aliasLibs = Library.getLibraries(group, loadedSketch.getContext().getCore().get("core.alias"));
                        if (aliasLibs != null && aliasLibs.size() > 0) {

                            for (Library l : aliasLibs) {
                                if (!libs.contains(l)) {
                                    libs.add(l);
                                }
                            }
                        }
                    }

                    if(libs != null && libs.size() > 0) {
                        JMenu top = new JMenu(Library.getCategoryName(group));
                        for(Library lib : libs) {
                            JMenu libMenu = new JMenu(lib.getName());
                            addSketchesFromFolder(libMenu, lib.getExamplesFolder());

                            if(libMenu.getItemCount() > 0) {
                                top.add(libMenu);
                            }
                        }

                        Base.setFont(top, "menu.entry");
                        examplesMenu.add(top);
                    }
                }
            }
        }

        Base.setFont(examplesMenu, "menu.entry");
        fileMenu.add(examplesMenu);

        fileMenu.addSeparator();
        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_MID);

        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.close"), KeyEvent.VK_W, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                askCloseWindow();
            }
        }));

        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.save"), KeyEvent.VK_S, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAllTabs();
            }
        }));

        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.saveas"), KeyEvent.VK_S, KeyEvent.SHIFT_MASK, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        }));

        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.export"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportSar();
            }
        }));

        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.import"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importSar();
            }
        }));

        fileMenu.addSeparator();

        JCheckBoxMenuItem cbitem = new JCheckBoxMenuItem(Base.i18n.string("menu.file.online"));
        cbitem.setState(Base.isOnline());
        cbitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JCheckBoxMenuItem s = (JCheckBoxMenuItem)e.getSource();
                if (s.getState()) {
                    Base.setOnlineMode();
                } else {    
                    Base.setOfflineMode();
                }
            }
        });
        fileMenu.add(cbitem);

        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.preferences"), KeyEvent.VK_MINUS, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Preferences prefs = new Preferences(Editor.this);
            }
        }));

        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_BOTTOM);


        fileMenu.add(createMenuEntry(Base.i18n.string("menu.file.quit"), KeyEvent.VK_Q, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(Editor.closeAllEditors()) {
                    Preferences.save();
                    System.exit(0);
                }
            }
        }));

        addMenuChunk(editMenu, Plugin.MENU_EDIT | Plugin.MENU_TOP);
        editMenu.addSeparator();
        addMenuChunk(editMenu, Plugin.MENU_EDIT | Plugin.MENU_MID);
        editMenu.addSeparator();
        addMenuChunk(editMenu, Plugin.MENU_EDIT | Plugin.MENU_BOTTOM);

        ActionListener createNewAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createNewSketchFile(e.getActionCommand());
            }
        };

        sketchMenu.add(createMenuEntry("Compile", KeyEvent.VK_R, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compile();
            }
        }));
        
        sketchMenu.add(createMenuEntry("Compile and Program", KeyEvent.VK_U, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                program();
            }
        }));
        
        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_TOP);
        sketchMenu.addSeparator();

        JMenu createSubmenu = new JMenu(Base.i18n.string("menu.sketch.create"));
        createSubmenu.add(createMenuEntry(Base.i18n.string("menu.create.ino"), 0, 0, createNewAction, "ino"));
        createSubmenu.add(createMenuEntry(Base.i18n.string("menu.create.cpp"), 0, 0, createNewAction, "cpp"));
        createSubmenu.add(createMenuEntry(Base.i18n.string("menu.create.c"), 0, 0, createNewAction, "c"));
        createSubmenu.add(createMenuEntry(Base.i18n.string("menu.create.header"), 0, 0, createNewAction, "h"));
        createSubmenu.add(createMenuEntry(Base.i18n.string("menu.create.asm"), 0, 0, createNewAction, "S"));
        createSubmenu.add(createMenuEntry(Base.i18n.string("menu.create.library"), 0, 0, createNewAction, "lib"));
        sketchMenu.add(createSubmenu);

        JMenu importSubmenu = new JMenu(Base.i18n.string("menu.sketch.import"));
        importSubmenu.add(createMenuEntry(Base.i18n.string("menu.sketch.import.source"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        }));
        importSubmenu.add(createMenuEntry(Base.i18n.string("menu.sketch.import.header"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        }));
        importSubmenu.add(createMenuEntry(Base.i18n.string("menu.sketch.import.binary"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        }));
        sketchMenu.add(importSubmenu);

        JMenu librariesSubmenu = new JMenu(Base.i18n.string("menu.sketch.import.libraries"));
        librariesSubmenu.add(createMenuEntry(Base.i18n.string("menu.sketch.import.libraries.install"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                installLibraryArchive();
            }
        }));

        librariesSubmenu.addSeparator();
        populateLibrariesMenu(librariesSubmenu);
        Base.setFont(librariesSubmenu, "menu.entry");
        sketchMenu.add(librariesSubmenu);


        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_MID);
        sketchMenu.addSeparator();
        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_BOTTOM);

        sketchMenu.add(createMenuEntry(Base.i18n.string("menu.sketch.properties"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new SketchProperties(Editor.this, loadedSketch);
            }
        }));

        String boardName = "";
        String coreName = "";
        String compilerName = "";
        String portName = "";

        Context ctx = loadedSketch.getContext();

        Board b = ctx.getBoard(); if (b != null) { boardName = " (" + b.getDescription() + ")"; }
        Core c = ctx.getCore(); if (c != null) { coreName = " (" + c.getDescription() + ")"; }
        Compiler o = ctx.getCompiler(); if (o != null) { compilerName = " (" + o.getDescription() + ")"; }
        CommunicationPort p = ctx.getDevice(); if (p != null) { portName = " (" + p.getName() + ")"; }

        JMenu boardsSubmenu = new JMenu(Base.i18n.string("menu.hardware.boards") + boardName);
        populateBoardsMenu(boardsSubmenu);
        Base.setFont(boardsSubmenu, "menu.entry");
        hardwareMenu.add(boardsSubmenu);

        JMenu coresSubmenu = new JMenu(Base.i18n.string("menu.hardware.cores") + coreName);
        populateCoresMenu(coresSubmenu);
        Base.setFont(coresSubmenu, "menu.entry");
        hardwareMenu.add(coresSubmenu);

        JMenu compilersSubmenu = new JMenu(Base.i18n.string("menu.hardware.compilers") + compilerName);
        populateCompilersMenu(compilersSubmenu);
        Base.setFont(compilersSubmenu, "menu.entry");
        hardwareMenu.add(compilersSubmenu);

        optionsMenu = new JMenu(Base.i18n.string("menu.hardware.options"));
        populateOptionsMenu(optionsMenu);
        optionsMenu.setEnabled(optionsMenu.getItemCount() > 0);
        Base.setFont(optionsMenu, "menu.entry");
        hardwareMenu.add(optionsMenu);

        serialPortsMenu = new JMenu(Base.i18n.string("menu.hardware.devices") + portName);
        populateSerialMenu(serialPortsMenu);
        serialPortsMenu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                Serial.updatePortList();
                populateSerialMenu(serialPortsMenu);
            }
            public void menuCanceled(MenuEvent e) {
            }
            public void menuDeselected(MenuEvent e) {
            }
        });
        Base.setFont(serialPortsMenu, "menu.entry");
        hardwareMenu.add(serialPortsMenu);

        programmersSubmenu = new JMenu(Base.i18n.string("menu.hardware.programmers"));
        populateProgrammersMenu(programmersSubmenu);

        Base.setFont(programmersSubmenu, "menu.entry");
        hardwareMenu.add(programmersSubmenu);

        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_TOP);
        hardwareMenu.addSeparator();
        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_MID);
        hardwareMenu.addSeparator();
        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_BOTTOM);

        toolsMenu.add(createMenuEntry(Base.i18n.string("menu.tools.pm"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PluginManager pm = new PluginManager();
                pm.openWindow(Editor.this);
            }
        }));

        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_TOP);
        toolsMenu.addSeparator();
        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_MID);

        toolsMenu.add(createMenuEntry(Base.i18n.string("menu.tools.sm"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ServiceManager.open(Editor.this);
            }
        }));

        toolsMenu.addSeparator();
        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_BOTTOM);

        PropertyFile pf = loadedSketch.getContext().getMerged();
        PropertyFile tools = pf.getChildren("tool");
        String[] toolsKeys = tools.childKeys();
        for (String k : toolsKeys) {
            String name = tools.get(k + ".name");
            JMenuItem item = new JMenuItem(name);
            item.setActionCommand("tool." + k);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String baseKey = e.getActionCommand();
                    loadedSketch.getContext().executeKey(baseKey + ".script");
                }
            });
            toolsMenu.add(item);
        }

        helpMenu.add(createMenuEntry(Base.i18n.string("menu.help.about"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleAbout();
            }
        }));

        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_TOP);
        helpMenu.addSeparator();

        PropertyFile links = Base.theme.getChildren("links");

        for(String link : links.childKeys()) {
            helpMenu.add(createMenuEntry(links.get(link + ".name"), 0, 0, (new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String link = e.getActionCommand();
                    Base.openURL(link);
                }
            }), links.get(link + ".url")));
        }

        links = loadedSketch.getContext().getMerged().getChildren("links");

        for(String link : links.childKeys()) {
            helpMenu.add(createMenuEntry(links.get(link + ".name"), 0, 0, (new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String link = e.getActionCommand();
                    Base.openURL(link);
                }
            }), links.get(link + ".url")));
        }

        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_MID);
        helpMenu.addSeparator();
        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_BOTTOM);
        JMenu debugSubmenu = new JMenu(Base.i18n.string("menu.help.debug"));

        debugSubmenu.add(createMenuEntry(Base.i18n.string("menu.help.debug.console"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.show();
            }
        }));

        debugSubmenu.add(createMenuEntry(Base.i18n.string("menu.help.debug.rebuild"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.cleanAndScanAllSettings();
            }
        }));

        debugSubmenu.add(createMenuEntry(Base.i18n.string("menu.help.debug.purge"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadedSketch.purgeCache();
            }
        }));

        debugSubmenu.add(createMenuEntry(Base.i18n.string("menu.help.debug.opendata"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.openURL(Base.getDataFolder().getAbsolutePath());
            }
        }));

        Base.setFont(debugSubmenu, "menu.entry");
        helpMenu.add(debugSubmenu);

    }

    public synchronized void populateOptionsMenu(JMenu menu) {
        TreeMap<String, String> opts = loadedSketch.getOptionGroups();

        menu.removeAll();

        for(String opt : opts.keySet()) {
            JMenu submenu = new JMenu(opts.get(opt));
            TreeMap<String, String>optvals = loadedSketch.getOptionNames(opt);
            ButtonGroup thisGroup = new ButtonGroup();

            for(String key : optvals.keySet()) {
                JMenuItem item = new JRadioButtonMenuItem(optvals.get(key));
                thisGroup.add(item);
                item.setActionCommand(opt + "=" + key);
                item.setSelected(loadedSketch.getOption(opt).equals(key));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String data = e.getActionCommand();
                        int idx = data.indexOf("=");
                        String key = data.substring(0, idx);
                        String value = data.substring(idx + 1);
                        loadedSketch.setOption(key, value);
                    }
                });

                Base.setFont(item, "menu.entry");
                submenu.add(item);
            }

            Base.setFont(submenu, "menu.entry");
            menu.add(submenu);
        }
    }

    class JSerialMenuItem extends JRadioButtonMenuItem {
        CommunicationPort port = null;
        String name = null;

        public JSerialMenuItem(CommunicationPort p) {
            super(p.getName());
            port = p;
        }

        public CommunicationPort getPort() {
            return port;
        }
    }

    public void populateSerialMenu(JMenu menu) {
        menu.removeAll();
        ButtonGroup portGroup = new ButtonGroup();

        for (CommunicationPort port : Base.communicationPorts) {
            JMenuItem item = new JSerialMenuItem(port);
            portGroup.add(item);
            item.setSelected(port == loadedSketch.getDevice()); // .toString().equals(loadedSketch.getSerialPort()));

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JSerialMenuItem i = (JSerialMenuItem)(e.getSource());
                    loadedSketch.setDevice(i.getPort());
                    if (i.getPort().getBoard() != null) {
                        loadedSketch.setBoard(i.getPort().getBoard());
                        updateAll();
                    }
                }
            });
            Base.setFont(item, "menu.entry");
            menu.add(item);
        }


    }

    public String[] getBoardGroups() {
        ArrayList<String> out = new ArrayList<String>();

        for(Board board : Base.boards.values()) {
            String group = board.get("group");

            if(out.indexOf(group) == -1) {
                out.add(group);
            }
        }

        String[] groupList = out.toArray(new String[0]);
        Arrays.sort(groupList);
        return groupList;
    }

    ButtonGroup boardMenuButtonGroup;

    public void populateProgrammersMenu(JMenu menu) {
        menu.removeAll();
        ButtonGroup progGroup = new ButtonGroup();

        Board board = loadedSketch.getContext().getBoard();
        ArrayList<Programmer> progList = new ArrayList<Programmer>();

        for(Programmer prog : Base.programmers.values()) {
            if(prog.worksWith(board)) {
                progList.add(prog);
            }
        }

        Programmer[] progs = progList.toArray(new Programmer[0]);
        Arrays.sort(progs);

        for(Programmer prog : progs) {
            JMenuItem item = new JRadioButtonMenuItem(loadedSketch.getContext().parseString(prog.toString()));
            progGroup.add(item);

            if(loadedSketch.getContext().getProgrammer() != null) {
                item.setSelected(loadedSketch.getContext().getProgrammer().equals(prog));
            }

            ImageIcon i = prog.getIcon(16);

            if(i != null) {
                item.setIcon(i);
            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setProgrammer(e.getActionCommand());
                }
            });
            item.setActionCommand(prog.getName());
            Base.setFont(item, "menu.entry");
            menu.add(item);
        }
    }

    public void populateBoardsMenu(JMenu menu) {
        boardMenuButtonGroup = new ButtonGroup();
        String[] groups = getBoardGroups();

        if(groups == null) {
            return;
        }

        for(String group : groups) {
            JMenu groupmenu = new JMenu(group);
            fillGroupMenu(groupmenu, group);

            if(groupmenu.getItemCount() > 0) {
                Base.setFont(groupmenu, "menu.entry");
                menu.add(groupmenu);
            }
        }
    }


    public void fillGroupMenu(JMenu menu, String group) {
        ArrayList<Board> boards = new ArrayList<Board>();

        for(Board board : Base.boards.values()) {
            if(board.get("group").equals(group)) {
                boards.add(board);
            }
        }

        Board[] boardList = boards.toArray(new Board[0]);
        Arrays.sort(boardList);

        for(Board board : boardList) {
            JMenuItem item = new JRadioButtonMenuItem(board.getDescription());
            boardMenuButtonGroup.add(item);

            if(loadedSketch.getContext().getBoard() != null) {
                if(loadedSketch.getContext().getBoard().equals(board)) {
                    item.setSelected(true);
                }
            }

            ImageIcon i = board.getIcon(16);

            if(i == null) {
                Core c = board.getCore();

                if(c != null) {
                    i = c.getIcon(16);
                }
            }

            if(i != null) {
                item.setIcon(i);
            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setBoard(e.getActionCommand());
                }
            });
            item.setActionCommand(board.getName());
            Base.setFont(item, "menu.entry");
            menu.add(item);
        }
    }

    public void populateCoresMenu(JMenu menu) {
        ButtonGroup coreGroup = new ButtonGroup();
        Board board = loadedSketch.getContext().getBoard();
        ArrayList<Core> coreList = new ArrayList<Core>();

        for(Core core : Base.cores.values()) {
            if(core.worksWith(board)) {
                coreList.add(core);
            }
        }

        Core[] cores = coreList.toArray(new Core[0]);
        Arrays.sort(cores);

        for(Core core : cores) {
            JMenuItem item = new JRadioButtonMenuItem(core.toString());
            coreGroup.add(item);

            if(loadedSketch.getContext().getCore() != null) {
                item.setSelected(loadedSketch.getContext().getCore().equals(core));
            }

            ImageIcon i = core.getIcon(16);

            if(i != null) {
                item.setIcon(i);
            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setCore(e.getActionCommand());
                }
            });
            item.setActionCommand(core.getName());
            Base.setFont(item, "menu.entry");
            menu.add(item);
        }

    }

    public void populateCompilersMenu(JMenu menu) {
        ButtonGroup compilerGroup = new ButtonGroup();
        Core core = loadedSketch.getContext().getCore();
        ArrayList<Compiler> compilerList = new ArrayList<Compiler>();

        if(core == null) {
            return;
        }

        for(Compiler compiler : Base.compilers.values()) {
            if(compiler.worksWith(core)) {
                compilerList.add(compiler);
            }
        }

        Compiler[] compilers = compilerList.toArray(new Compiler[0]);
        Arrays.sort(compilers);

        for(Compiler compiler : compilers) {
            JMenuItem item = new JRadioButtonMenuItem(compiler.getDescription());
            compilerGroup.add(item);
            item.setSelected(loadedSketch.getContext().getCompiler().equals(compiler));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setCompiler(e.getActionCommand());
                }
            });
            item.setActionCommand(compiler.getName());
            Base.setFont(item, "menu.entry");
            menu.add(item);
        }
    }

    public void addMenuChunk(JPopupMenu menu, int filterFlags) {
        int tab = getActiveTab();

        if(tab > -1) {
            EditorBase eb = getTab(tab);

            if(eb != null) {
                eb.populateMenu(menu, filterFlags);
            }
        }

        addPluginsToMenu(menu, filterFlags);
    }

    public void addMenuChunk(JMenu menu, int filterFlags) {
        int tab = getActiveTab();

        if(tab > -1) {
            EditorBase eb = getTab(tab);

            if(eb != null) {
                eb.populateMenu(menu, filterFlags);
            }
        }

        addPluginsToMenu(menu, filterFlags);
    }


    public void populateContextMenu(JPopupMenu menu, int filterFlags, DefaultMutableTreeNode node) {
        for(Plugin plugin : plugins) {
            try {
                plugin.populateContextMenu(menu, filterFlags, node);
            } catch(AbstractMethodError e) {
            } catch(Exception e) {
//                error(e);
            }
        }
    }

    public void addPluginsToMenu(JPopupMenu menu, int filterFlags) {
        for(Plugin plugin : plugins) {
            try {
                plugin.populateMenu(menu, filterFlags);
            } catch(AbstractMethodError e) {
            } catch(Exception e) {
            }
        }
    }

    public void addPluginsToMenu(JMenu menu, int filterFlags) {
        for(Plugin plugin : plugins) {
            try {
                plugin.populateMenu(menu, filterFlags);
            } catch(AbstractMethodError e) {
            } catch(Exception e) {
            }
        }
    }

    public void addPluginsToToolbar(JToolBar tb, int filterFlags) {
        for(final Plugin plugin : plugins) {
            try {
                plugin.addToolbarButtons(tb, filterFlags);
            } catch(AbstractMethodError e) {
            } catch(Exception e) {
//                error(e);
            }
        }
    }

    public void updateAll() {
        updateMenus();
        updateTree();
        setStatus();
    }

//    public String getSerialPort() {
//        return loadedSketch.getSerialPort();
//    }

    public void message(String s, int c) {
        if(c == 2) {
            error(s);
        } else {
            message(s);
        }
    }

    public boolean willDoImport;

    public void importSar() {
        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        javax.swing.filechooser.FileFilter filter = new SarFileFilter();
        fc.setFileFilter(filter);

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.importsar");
            if (loc == null) {
                loc = Base.getSketchbookFolder();
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(Base.getSketchbookFolder());
        }

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            try {
                File sarFile = fc.getSelectedFile();

                if (Preferences.getBoolean("editor.save.remloc")) {
                    Preferences.setFile("editor.locations.importsar", sarFile.getParentFile());
                }

                JarFile sarfile = new JarFile(sarFile);
                Manifest manifest = sarfile.getManifest();
                Attributes manifestContents = manifest.getMainAttributes();

                String sketchName = manifestContents.getValue("Sketch-Name");
                String author = manifestContents.getValue("Author");
                String libs = manifestContents.getValue("Libraries");
                String brd = manifestContents.getValue("Board");
                String cr = manifestContents.getValue("Core");
                String archived = manifestContents.getValue("Archived");

                final JDialog dialog = new JDialog(this, "Import SAR", true);

                EmptyBorder bdr = new EmptyBorder(4, 4, 4, 4);
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBorder(bdr);

                JTextField sketchNameBox = new JTextField(sketchName);

                GridBagConstraints c = new GridBagConstraints();
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridwidth = 1;
                c.gridheight = 1;
                c.gridx = 0;
                c.gridy = 0;
                c.weightx = 1.0;

                JLabel label = new JLabel("Sketch Name:");
                label.setBorder(bdr);
                panel.add(label, c);
                c.gridx = 1;
                panel.add(sketchNameBox, c);
                c.gridx = 0;
                c.gridy++;

                label = new JLabel("Author:");
                label.setBorder(bdr);
                panel.add(label, c);
                c.gridx = 1;
                label = new JLabel(author);
                label.setBorder(bdr);
                panel.add(label, c);
                c.gridx = 0;
                c.gridy++;

                label = new JLabel("Board:");
                label.setBorder(bdr);
                panel.add(label, c);
                c.gridx = 1;
                label = new JLabel(brd);
                label.setBorder(bdr);
                panel.add(label, c);
                c.gridx = 0;
                c.gridy++;
                label = new JLabel("Core:");
                label.setBorder(bdr);
                panel.add(label, c);
                c.gridx = 1;
                label = new JLabel(cr);
                label.setBorder(bdr);
                panel.add(label, c);
                c.gridx = 0;
                c.gridy++;

                c.gridwidth = 2;
                label = new JLabel("Libraries:");
                label.setBorder(bdr);
                panel.add(label, c);
                c.gridwidth = 1;
                c.gridy++;

                HashMap<String, JComboBox> libcheck = new HashMap<String, JComboBox>();
                String[] libarr = libs.split(" ");
                c.gridx = 1;
                c.gridwidth = 2;

                ArrayList<String> dests = new ArrayList<String>();
                dests.add("Do not import");
                dests.add("Import to sketch folder");

                for(String group : Library.getLibraryCategories()) {
                    if(group.startsWith("cat:")) {
                        dests.add(Library.getCategoryName(group));
                    }
                }

                for(String l : libarr) {
                    label = new JLabel(l + ":");
                    c.gridx = 0;
                    panel.add(label, c);


                    JComboBox cb = new JComboBox(dests.toArray(new String[0]));
                    c.gridx = 1;
                    panel.add(cb, c);
                    libcheck.put(l, cb);

                    c.gridy++;
                }

                c.gridx = 0;
                c.gridwidth = 1;
                final Editor me = this;

                JButton cancel = new JButton(Base.i18n.string("misc.cancel"));
                cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        me.willDoImport = false;
                        dialog.dispose();
                    }
                });
                JButton impt = new JButton(Base.i18n.string("misc.import"));
                impt.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        me.willDoImport = true;
                        dialog.dispose();
                    }
                });

                panel.add(cancel, c);
                c.gridx = 1;
                panel.add(impt, c);

                dialog.setContentPane(panel);
                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);

                if(willDoImport) {
                    String oldSketchName = sketchName;
                    sketchName = sketchNameBox.getText();
                    File targetDir = new File(Base.getSketchbookFolder(), sketchName);
                    message("Importing to " + targetDir.getAbsolutePath());
                    int n = 0;

                    if(targetDir.exists()) {
                        Object[] options = { "Yes", "No" };
                        n = JOptionPane.showOptionDialog(this,
                                                         "The sketch " + sketchName + " already exists.\n" +
                                                         "Do you want to overwrite it?",
                                                         "Sketch Exists",
                                                         JOptionPane.YES_NO_OPTION,
                                                         JOptionPane.QUESTION_MESSAGE,
                                                         null,
                                                         options,
                                                         options[1]);

                        if(n == 0) {
                            Base.removeDir(targetDir);
                        }
                    }

                    if(n == 0) {
                        message("Starting import...");
                        byte[] buffer = new byte[1024];
                        ZipInputStream zis = new ZipInputStream(new FileInputStream(sarFile));
                        ZipEntry ze = zis.getNextEntry();

                        while(ze != null) {
                            String fileName = ze.getName();
                            message("  " + fileName + " ->");
                            String spl[] = fileName.split("/");

                            File newFile = null;

                            if(spl[0].equals("META-INF")) {
                                ze = zis.getNextEntry();
                                continue;
                            }

                            if(spl[0].equals(oldSketchName)) {
                                spl[0] = sketchName;
                            }

                            if(fileName.startsWith(".")) {
                                ze = zis.getNextEntry();
                                continue;
                            }

                            if(spl[0].equals("libraries")) {
                                if(spl.length > 1) {
                                    if(libcheck.get(spl[1]) == null) {
                                        // This is a library we don't know about - ignore it
                                        ze = zis.getNextEntry();
                                        warning("    (ignore)");
                                        continue;
                                    }

                                    JComboBox cb = libcheck.get(spl[1]);

                                    if(cb.getSelectedIndex() == 0) {
                                        // The library isn't selected for import
                                        ze = zis.getNextEntry();
                                        message("    (skip)");
                                        continue;
                                    }

                                    // We now need to re-hash the file name.  Remove the
                                    // "libraries" from the front, and replace it with the
                                    // path of the selected target.

                                    File libTarget = null;

                                    if(cb.getSelectedIndex() == 1) {
                                        // Sketch folder
                                        File sf = new File(Base.getSketchbookFolder(), sketchName);
                                        libTarget = new File(sf, "libraries");
                                    } else {
                                        String dname = (String)cb.getSelectedItem();
                                        String cat = null;

                                        for(String k : Library.getLibraryCategories()) {
                                            if(Library.getCategoryName(k).equals(dname)) {
                                                cat = k;
                                            }
                                        }

                                        libTarget = Library.getCategoryLocation(cat);
                                    }

                                    if(libTarget != null) {
                                        newFile = libTarget;

                                        for(int i = 1; i < spl.length; i++) {
                                            newFile = new File(newFile, spl[i]);
                                        }
                                    }
                                } else {
                                    ze = zis.getNextEntry();
                                    continue;
                                }
                            } else {
                                newFile = Base.getSketchbookFolder();

                                for(int i = 0; i < spl.length; i++) {
                                    if(spl[i].equals(oldSketchName)) {
                                        spl[i] = sketchName;
                                    }

                                    int di = spl[i].lastIndexOf(".");

                                    if(di >= 0) {
                                        String s = spl[i].substring(0, di);
                                        String e = spl[i].substring(di + 1);
                                        warning(s);
                                        warning(e);

                                        if(s.equals(oldSketchName)) {
                                            spl[i] = sketchName + "." + e;
                                        }
                                    }

                                    newFile = new File(newFile, spl[i]);
                                }
                            }

                            if(newFile == null) {
                                ze = zis.getNextEntry();
                                error("    (broken)");
                                continue;
                            }

                            message("    " + newFile.getAbsolutePath());

                            newFile.getParentFile().mkdirs();

                            if(ze.isDirectory()) {
                                newFile.mkdirs();
                            } else {
                                FileOutputStream fos = new FileOutputStream(newFile);
                                int len;

                                while((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }

                                fos.close();

                                if(!newFile.exists()) {
                                    error("FAILED MAKING FILE");
                                }
                            }

                            ze = zis.getNextEntry();
                        }

                        zis.closeEntry();
                        zis.close();
                        Base.gatherLibraries();
                        //editor.rebuildImportMenu();
                        Base.createNewEditor(new File(Base.getSketchbookFolder(), sketchName).getAbsolutePath());
                    }
                }
            } catch(Exception e) {
                Base.error(e);
            }
        }
    }

    public void exportSar() {

        File newFile = new File(Base.getSketchbookFolder(), loadedSketch.getName() + ".sar");
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(newFile);
        javax.swing.filechooser.FileFilter filter = new SarFileFilter();
        fc.setFileFilter(filter);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.exportsar");
            if (loc == null) {
                loc = Base.getSketchbookFolder();
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(Base.getSketchbookFolder());
        }

        int rv = fc.showSaveDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            newFile = fc.getSelectedFile();
            if (Preferences.getBoolean("editor.save.remloc")) {
                Preferences.setFile("editor.locations.exportsar", newFile.getParentFile());
            }
            loadedSketch.generateSarFile(newFile);
        }
    }

    public void saveAs() {
        JFileChooser fc = new JFileChooser();
        javax.swing.filechooser.FileFilter filter = new SketchFolderFilter();
        fc.setFileFilter(filter);

        javax.swing.filechooser.FileView view = new SketchFileView();
        fc.setFileView(view);

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.savesketch");
            if (loc == null) {
                loc = Base.getSketchbookFolder();
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(Base.getSketchbookFolder());
        }
        
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        int rv = fc.showSaveDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            File newFile = fc.getSelectedFile();

            if(newFile.exists()) {
                int n = twoOptionBox(
                    JOptionPane.WARNING_MESSAGE,
                    Base.i18n.string("msg.overwrite.title"),
                    Base.i18n.string("msg.overwrite.body", newFile.getName()),
                    Base.i18n.string("misc.yes"),
                    Base.i18n.string("misc.no")
                );

                if(n != 0) {
                    return;
                }

                newFile.delete();
            }

            if (Preferences.getBoolean("editor.save.remloc")) {
                Preferences.setFile("editor.locations.savesketch", newFile.getParentFile());
            }
            loadedSketch.saveAs(newFile);
        }
    }

    public boolean validName(String name) {
        final String validCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-";

        if(name == null) return false;

        for(int i = 0; i < name.length(); i++) {
            if(validCharacters.indexOf(name.charAt(i)) == -1) {
                return false;
            }
        }

        return true;
    }

    public void createNewSketchFile(String extension) {
        String name = (String)JOptionPane.showInputDialog(
            this,
            Base.i18n.string("msg.create.body"),
            Base.i18n.string("msg.create.title", extension),
            JOptionPane.PLAIN_MESSAGE
        );

        while(!validName(name)) {
            if(name == null) {
                return;
            }

            JOptionPane.showMessageDialog(this,
                Base.i18n.string("err.invalid.body"),
                Base.i18n.string("err.invalid.title"),
                JOptionPane.ERROR_MESSAGE
            );
            name = (String)JOptionPane.showInputDialog(
                this,
                Base.i18n.string("msg.create.body"),
                Base.i18n.string("msg.create.title", extension),
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                name
            );
        }

        loadedSketch.createNewFile(name + "." + extension);
    }

    File importFileDefaultDir = null;

    public void importFile(String type) {
        JFileChooser fc = new JFileChooser();
        javax.swing.filechooser.FileFilter filter;

        if(type == "source") {
            filter = new SourceFileFilter();
            fc.setFileFilter(filter);
        } else if(type == "header") {
            filter = new HeaderFileFilter();
            fc.setFileFilter(filter);
        }

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.importfile");
            if (loc == null) {
                loc = new File(System.getProperty("user.dir"));
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }
        
        int r = fc.showOpenDialog(this);

        if(r == JFileChooser.APPROVE_OPTION) {
            File src = fc.getSelectedFile();
            if (Preferences.getBoolean("editor.save.remloc")) {
                Preferences.setFile("editor.locations.importfile", src.getParentFile());
            }

            if(!src.exists()) {
                JOptionPane.showMessageDialog(this, 
                    Base.i18n.string("err.notfound.title"),
                    Base.i18n.string("err.notfound", src.getName()), 
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            File dest = null;

            if(type == "source" || type == "header") {
                dest = new File(loadedSketch.getFolder(), src.getName());
            } else if(type == "binary") {
                dest = new File(loadedSketch.getBinariesFolder(), src.getName());
            } else {
                return;
            }

            File dp = dest.getParentFile();

            if(!dp.exists()) {
                dp.mkdirs();
            }

            Base.copyFile(src, dest);

            if(type == "source" || type == "header") {
                loadedSketch.loadFile(dest);
            }

            updateTree();
        }
    }

    ActionListener insertIncludeAction = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String lib = e.getActionCommand();
            insertStringAtStart(loadedSketch.getMainFile(), "#include <" + lib + ".h>\n");
//            loadedSketch.addLibraryToImportList(lib);
            updateLibrariesTree();
        }
    };

    void insertStringAtStart(File f, String s) {
        openOrSelectFile(f);
        int tab = getTabByFile(f);

        if(tab == -1) {
            return;
        }

        EditorBase eb = getTab(tab);
        eb.insertAtStart(s);
    }

    public void populateLibrariesMenu(JComponent menu) {
        JMenuItem item;

        Core thisCore = loadedSketch.getContext().getCore();

        if(thisCore == null) {
            return;
        }

        TreeMap<String, TreeSet<Library>> libs = Library.getFilteredLibraries(thisCore.getName());

        for(String group : libs.keySet()) {
            String groupName = Library.getCategoryName(group);
            JMenu libsMenu = new JMenu(groupName);

            for(Library lib : libs.get(group)) {
                item = new JMenuItem(lib.getName());
                item.addActionListener(insertIncludeAction);
                item.setActionCommand(lib.getName());
                libsMenu.add(item);
            }

            Base.setFont(libsMenu, "menu.entry");
            menu.add(libsMenu);
        }
    }

    public void releasePort(String portName) {
        for(Plugin plugin : plugins) {
            try {
                plugin.releasePort(portName);
            } catch(Exception e) {
//                Base.error(e);
            }
        }
    }

    public void launchPlugin(Class<?> pluginClass) {
        for(Plugin plugin : plugins) {
            if(plugin.getClass() == pluginClass) {
                Debug.message("Launching plugin " + plugin.getClass());
                plugin.launch();
            }
        }
    }

    public static void registerEditor(Editor e) {
        synchronized (editorList) {
            editorList.remove(e); // Just in case...?
            editorList.add(e);
        }
    }

    public static void unregisterEditor(Editor e) {
        synchronized (editorList) {
            editorList.remove(e);
        }
    }

    public static boolean shouldQuit() {
        return (editorList.size() == 0);
    }

    public static void rawBroadcast(String msg) {
        for(Editor e : editorList) {
            e.appendToConsole(msg);
        }
    }

    public static void broadcast(String msg) {
        for(Editor e : editorList) {
            e.message(msg);
        }
    }

    public static void lockAll() {
        for (Editor e : editorList) {
            e.lock();
        }
    }

    public static void unlockAll() {
        for (Editor e : editorList) {
            e.unlock();
        }
    }

    public void lock() {
        startUpdateBlocker();
        setEnabled(false);
    }

    public void unlock() {
        setEnabled(true);
        stopUpdateBlocker();
    }

    public static void bulletAll(String msg) {
        for (Editor e : editorList) {
            e.bullet(msg);
        }
    }

    public static void bullet2All(String msg) {
        for (Editor e : editorList) {
            e.bullet2(msg);
        }
    }

    public static void updateAllEditors() {
        for(Editor e : editorList) {
            e.updateAll();
        }
    }

    public static void selectAllEditorProgrammers() {
        for(Editor e : editorList) {
            e.reselectEditorProgrammer();
        }
    }

    public static void selectAllEditorBoards() {
        for(Editor e : editorList) {
            e.reselectEditorBoard();
        }
    }

    public void reselectEditorProgrammer() {
//        String eb = loadedSketch.getBoardName();
//
//        if(eb != null) {
//            loadedSketch.setBoard(eb);
//        }
    }

    public void reselectEditorBoard() {
        String eb = loadedSketch.getBoardName();

        if(eb != null) {
            loadedSketch.setBoard(eb);
        }
    }

    public static boolean closeAllEditors() {
        ArrayList<Editor>localList = new ArrayList<Editor>();
        localList.addAll(editorList);
        for(Editor e : localList) {
            if(e.askCloseWindow() == false) {
                return false;
            }
        }

        return true;
    }

    public void handleOpenPrompt() {
        // get the frontmost window frame for placing file dialog

        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        javax.swing.filechooser.FileFilter filter = new SketchFolderFilter();
        fc.setFileFilter(filter);

        javax.swing.filechooser.FileView view = new SketchFileView();
        fc.setFileView(view);

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.opensketch");
            if (loc == null) {
                loc = Base.getSketchbookFolder();
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(Base.getSketchbookFolder());
        }

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            // Convert the DefaultShellFolder into a File object via a string representation
            // of the path
            File f = new File(fc.getSelectedFile().getAbsolutePath());
            if (Preferences.getBoolean("editor.save.remloc")) {
                Preferences.setFile("editor.locations.opensketch", f.getParentFile());
            }
            loadSketch(f);
        }
    }

    public void loadSketch(String f) {
        loadSketch(new File(f));
    }

    public void loadSketch(File f) {
        if(loadedSketch.isUntitled() && !isModified()) {
            closeAllTabs();
            loadedSketch = new Sketch(f, this);
            loadedSketch.attachToEditor(this);
            filesTreeRoot.setUserObject(loadedSketch.getFolder());
            treeRoot.setUserObject(loadedSketch);
            updateAll();
//            treeModel.nodeStructureChanged(treeRoot);
            openOrSelectFile(loadedSketch.getMainFile());
            loadedSketch.loadConfig();
        } else {
            Base.createNewEditor(f.getPath());
        }

        fireEvent(UEvent.SKETCH_OPEN);
    }

    public static void updateLookAndFeel() {
        for(Editor e : editorList) {
            SwingUtilities.updateComponentTreeUI(e);
//            e.rotateTabLabels();
            e.refreshScrolls();
        }
    }

    public void refreshScrolls() {
        consoleScroll.setShadow(
            Base.getTheme().getInteger("console.shadow.top"),
            Base.getTheme().getInteger("console.shadow.bottom")
        );
    }

    public static void releasePorts(String n) {
        for(Editor e : editorList) {
            e.releasePort(n);
        }
    }

    public static void refreshAllEditors() {
        for(Editor e : editorList) {
            e.refreshEditors();
            e.updateToolbar();
            e.updateConsole();
        }
    }

    public void updateConsole() {
        console.updateStyleSettings();
    }

    public void refreshEditors() {
        int ntabs = editorTabs.getTabCount();

        for(int i = 0; i < ntabs; i++) {
            EditorBase eb = getTab(i);
            eb.refreshSettings();
        }
    }

    public void handleAbout() {
        About a = new About(this);
    }

    public void installLibraryArchive() {
        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if(f.getName().toLowerCase().endsWith(".zip")) {
                    return true;
                }

                if(f.isDirectory()) {
                    return true;
                }

                return false;
            }

            public String getDescription() {
                return Base.i18n.string("filters.zip");
            }
        });

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.importlib");
            if (loc == null) {
                loc = Base.getSketchbookFolder();
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(Base.getSketchbookFolder());
        }

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            String[] entries = getZipEntries(fc.getSelectedFile());
            if (fc.getSelectedFile() == null) {
                return;
            }
            if (Preferences.getBoolean("editor.save.remloc")) {
                Preferences.setFile("editor.locations.importlib", fc.getSelectedFile().getParentFile());
            }
            message(Base.i18n.string("msg.analyzing", fc.getSelectedFile().getName()));

            // In a library we expect certain things to be in certain places.  That's only
            // logical, or the IDE won't be able to find them!  People that don't package
            // their libraries up properly are just clueless.  Still, we have to handle all
            // sorts of idiocy here, so this is going to be quite a complex operation.

            TreeMap<String, String> foundLibs = new TreeMap<String, String>();

            // First we'll do a quick check to see if it's a properly packaged archive.
            // We expect to have at least one header file (xxx.h) in one folder (xxx/)

            for(String entry : entries) {
                if(entry.endsWith(".h")) {

                    String[] parts = entry.split("/");

                    if(parts.length >= 2) {
                        String name = parts[parts.length - 1];
                        String parent = parts[parts.length - 2];

                        String possibleLibraryName = name.substring(0, name.lastIndexOf("."));
                        String folder = "";

                        for(int i = 0; i < parts.length - 1; i++) {
                            if(folder.equals("") == false) {
                                folder += "/";
                            }

                            folder += parts[i];
                        }

                        if(parent.equals(possibleLibraryName) || parent.contains(possibleLibraryName)) {
                            // this looks like a valid archive at this point.
                            message(Base.i18n.string("msg.analyzing.found", possibleLibraryName, folder));
                            foundLibs.put(possibleLibraryName, folder);
                        }
                    }
                }
            }

            if(foundLibs.size() > 0) {
                ArrayList<LibCatObject> cats = new ArrayList<LibCatObject>();

                for(String group : Library.getLibraryCategories()) {
                    if(group.startsWith("cat:")) {
                        LibCatObject ob = new LibCatObject(group, Library.getCategoryName(group));
                        cats.add(ob);
                    }
                }

                LibCatObject[] catarr = cats.toArray(new LibCatObject[cats.size()]);

                LibCatObject loc = (LibCatObject)JOptionPane.showInputDialog(this, 
                    Base.i18n.string("msg.libdest.body"),
                    Base.i18n.string("msg.libdest.title"), 
                    JOptionPane.PLAIN_MESSAGE,
                    null, catarr, null);

                if(loc != null) {
                    File installPath = Library.getCategoryLocation(loc.getKey());
                    message("Installing to " + installPath.getAbsolutePath());

                    for(String lib : foundLibs.keySet()) {
                        message(Base.i18n.string("misc.analyzing.install", lib));
                        // First make a list of remapped files...
                        TreeMap<String, File> translatedFiles = new TreeMap<String, File>();

                        File libDir = new File(installPath, lib);

                        String prefix = foundLibs.get(lib) + "/";

                        for(String entry : entries) {
                            if(entry.startsWith(prefix)) {
                                String filePart = entry.substring(prefix.length());
                                File destFile = new File(libDir, filePart);
                                translatedFiles.put(entry, destFile);
                                message(entry + " -> " + destFile.getAbsolutePath());
                            }
                        }

                        extractZipMapped(fc.getSelectedFile(), translatedFiles);

                    }

                    message(Base.i18n.string("misc.analyzing.updating.start"));
                    Base.gatherLibraries();
                    Editor.updateAllEditors();
                    message(Base.i18n.string("misc.analyzing.updating.done"));
                }
            } else {
                error(Base.i18n.string("err.analyzing.notfound.error"));
                warning(Base.i18n.string("err.analyzing.notfound.warning"));
            }
        }
    }

    public void extractZipMapped(File zipFile, TreeMap<String, File>mapping) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();

            while(ze != null) {
                File outputFile = mapping.get(ze.getName());

                if(outputFile != null) {
                    File p = outputFile.getParentFile();

                    if(!p.exists()) {
                        p.mkdirs();
                    }

                    byte[] buffer = new byte[1024];
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    int len;

                    while((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    fos.close();
                    outputFile.setExecutable(true, false);
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
        } catch(Exception e) {
            Base.error(e);
            return;
        }
    }

    public class LibCatObject {
        public String key = null;
        public String name = null;
        public LibCatObject(String k, String n) {
            key = k;
            name = n;
        }
        public String toString() {
            return name;
        }
        public String getKey() {
            return key;
        }
    }

    public String[] getZipEntries(File zipFile) {
        ArrayList<String> files = new ArrayList<String>();

        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();

            while(ze != null) {
                if(!ze.isDirectory()) {
                    files.add(ze.getName());
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
        } catch(Exception e) {
            Base.error(e);
            return null;
        }

        String[] out = files.toArray(new String[0]);
        Arrays.sort(out);

        return out;
    }

    public String askForFilename(String oldName) {
        if(oldName == null) {
            oldName = "";
        }

        String newName = (String)JOptionPane.showInputDialog(
            this,
            Base.i18n.string("msg.askfile.body"),
            Base.i18n.string("msg.askfile.title"),
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            oldName);
        return newName;
    }

    public void findAndUnzipZipFile(String dest) {
        File destFolder = new File(dest);

        if(!destFolder.isDirectory()) {
            return;
        }

        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if(f.getName().toLowerCase().endsWith(".zip")) {
                    return true;
                }

                if(f.isDirectory()) {
                    return true;
                }

                return false;
            }

            public String getDescription() {
                return Base.i18n.string("filters.zip");
            }
        });

        if (Preferences.getBoolean("editor.save.remloc")) {
            File loc = Preferences.getFile("editor.locations.extractzip");
            if (loc == null) {
                loc = Base.getSketchbookFolder();
            }
            fc.setCurrentDirectory(loc);
        } else {
            fc.setCurrentDirectory(Base.getSketchbookFolder());
        }

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            File zipFile = fc.getSelectedFile();
            if (Preferences.getBoolean("editor.save.remloc")) {
                Preferences.setFile("editor.locations.extractzip", zipFile.getParentFile());
            }

            try {
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry ze = zis.getNextEntry();

                while(ze != null) {
                    File extractTo = new File(destFolder, ze.getName());

                    if(ze.isDirectory()) {
                        extractTo.mkdirs();
                    } else {
                        File p = extractTo.getParentFile();

                        if(!p.exists()) {
                            p.mkdirs();
                        }

                        byte[] buffer = new byte[1024];
                        FileOutputStream fos = new FileOutputStream(extractTo);
                        int len;

                        while((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                        fos.close();
                        extractTo.setExecutable(true, false);
                    }

                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
                zis.close();
            } catch(Exception e) {
                Base.error(e);
            }

            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        updateTree();
    }

    public TreePath[] getPaths(JTree tree) {
        if (tree == null) {
            return null;
        }
        if (tree.getModel() == null) {
            return null;
        }
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        ArrayList<TreePath> list = new ArrayList<TreePath>();
        getPaths(tree, new TreePath(root), list);

        return (TreePath[]) list.toArray(new TreePath[list.size()]);
    }

    public void getPaths(JTree tree, TreePath parent, ArrayList<TreePath> list) {
        list.add(parent);
        TreeNode node = (TreeNode) parent.getLastPathComponent();

        if(node.getChildCount() >= 0) {
            for(Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                getPaths(tree, path, list);
            }
        }
    }

    public TreePath[] saveTreeState(JTree tree) {
        TreePath[] allPaths = getPaths(tree);
        if (allPaths == null) {
            return null;
        }
        ArrayList<TreePath> openPaths = new ArrayList<TreePath>();

        for(TreePath path : allPaths) {
            if(tree.isExpanded(path)) {
                openPaths.add(path);
            }
        }

        return (TreePath[]) openPaths.toArray(new TreePath[openPaths.size()]);
    }

    public TreePath findPathByStrings(JTree tree, TreePath path) {
        Object[] pathComponents = path.getPath();
        TreePath[] allPaths = getPaths(tree);
        if (allPaths == null) {
            return null;
        }

        for(TreePath testPath : allPaths) {
            Object[] testComponents = testPath.getPath();

            if(testComponents.length != pathComponents.length) {
                continue;
            }

            boolean match = true;

            for(int i = 0; i < pathComponents.length; i++) {
                Object po = pathComponents[i];
                Object to = testComponents[i];

                if(!po.toString().equals(to.toString())) {
                    match = false;
                    break;
                }
            }

            if(match) {
                return testPath;
            }
        }

        return null;
    }

    public void restoreTreeState(JTree tree, TreePath[] savedState) {
        TreePath[] allPaths = getPaths(tree);
        if (allPaths == null) {
            return;
        }

        for(TreePath path : savedState) {
            TreePath foundPath = findPathByStrings(tree, path);

            if(foundPath != null) {
                tree.expandPath(foundPath);
            }
        }

        /*
                    DefaultMutableTreeNode oldnode = (DefaultMutableTreeNode)path.getLastPathComponent();
                    if (oldnode.getUserObject() instanceof File) {
                        File oldFile = (File)oldnode.getUserObject();
                        for (TreePath np : allPaths) {
                            DefaultMutableTreeNode newnode = (DefaultMutableTreeNode)np.getLastPathComponent();
                            File newFile = (File)newnode.getUserObject();
                            if (newFile.equals(oldFile)) {
                                tree.expandPath(np);
                            }
                        }
                    } else if (oldnode.getUserObject() instanceof String) {
                        String oldFile = oldnode.getUserObject().toString();
                        for (TreePath np : allPaths) {
                            DefaultMutableTreeNode newnode = (DefaultMutableTreeNode)np.getLastPathComponent();
                            String newFile = newnode.getUserObject().toString();
                            if (newFile.equals(oldFile)) {
                                tree.expandPath(np);
                            }
                        }
                    }
                }
        */
    }

    public void compile() {
        clearConsole();

        if(compilerRunning()) {
            error(Base.i18n.string("err.compilerrunning"));
            return;
        }

        if(Preferences.getBoolean("editor.save.auto")) {
            if(!loadedSketch.parentIsProtected() && !loadedSketch.isUntitled()) {
                saveAllTabs();
            }
        }

        DefaultRunHandler runHandler = new DefaultRunHandler(false);
        compilationThread = new Thread(runHandler, "Compiler");
        compilationThread.start();
    }

    public void program() {
        clearConsole();

        if(compilerRunning()) {
            error(Base.i18n.string("err.compilerrunning"));
            return;
        }

        if(Preferences.getBoolean("editor.save.automatic")) {
            if(!loadedSketch.parentIsProtected() && !loadedSketch.isUntitled()) {
                saveAllTabs();
            }
        }

        DefaultRunHandler runHandler = new DefaultRunHandler(true);
        compilationThread = new Thread(runHandler, "Compiler");
        compilationThread.start();
    }

    public boolean compilerRunning() {
        if(compilationThread == null) {
            return false;
        }

        return compilationThread.isAlive();
    }

    public void abortCompilation() {
        if(!compilerRunning()) {
            return;
        }

        loadedSketch.requestTermination();

        while(compilerRunning()) {
            continue;
        }

        runButton.setEnabled(true);
        programButton.setEnabled(true);
        runButton.setVisible(true);
        abortButton.setVisible(false);
    }

    public void exportLocalLibrary(Library lib) {
        try {
            File newFile = new File(Base.getSketchbookFolder(), lib.getName() + ".zip");
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(newFile);
            javax.swing.filechooser.FileFilter filter = new ZipFileFilter();
            fc.setFileFilter(filter);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (Preferences.getBoolean("editor.save.remloc")) {
                File loc = Preferences.getFile("editor.locations.exportlib");
                if (loc == null) {
                    loc = Base.getSketchbookFolder();
                }
                fc.setCurrentDirectory(loc);
            } else {
                fc.setCurrentDirectory(Base.getSketchbookFolder());
            }

            int rv = fc.showSaveDialog(this);

            if(rv == JFileChooser.APPROVE_OPTION) {
                File archiveFile = fc.getSelectedFile();
                if (Preferences.getBoolean("editor.save.remloc")) {
                    Preferences.setFile("editor.locations.exportlib", archiveFile.getParentFile());
                }

                if(archiveFile.exists()) {
                    archiveFile.delete(); // Confirm!!!
                }

                FileOutputStream outfile = new FileOutputStream(archiveFile);
                ZipOutputStream zip = new ZipOutputStream(outfile);
                zip.putNextEntry(new ZipEntry(lib.getName() + "/"));
                zip.closeEntry();

                File libsFolder = new File(loadedSketch.getFolder(), "libraries");
                File libFolder = new File(libsFolder, lib.getName());

                loadedSketch.addTree(libFolder, lib.getName(), zip);
                zip.flush();
                zip.close();
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }

    public void fireEvent(int event) {
        for(Plugin plugin : plugins) {
            try {
                plugin.catchEvent(event);
            } catch(AbstractMethodError e) {
            } catch(Exception e) {
            }
        }
    }

    void urlClicked(String url) {
        Pattern p = Pattern.compile("^uecide://error/(\\d+)/(.*)$");
        Matcher m = p.matcher(url);
        if (m.find()) {
            try {
                int line = Integer.parseInt(m.group(1));
                int tab = openOrSelectFile(new File(m.group(2)));
                if (tab != -1) {
                    EditorBase eb = getTab(tab);
                    eb.gotoLine(line - 1);
                }

            } catch (Exception e) {
            }
        }
    }

    public Console getConsole() { return console; }
    
    public void addPanelsToTabs(JTabbedPane tabs, int flags) {
        for(Plugin plugin : plugins) {
            try {
                plugin.addPanelsToTabs(tabs, flags);
            } catch(AbstractMethodError e) {
            } catch(Exception e) {
            }
        }
    }

    public void updateSketchConfig() {
        if (loadedSketch.updateSketchConfig()) {
            populateProgrammersMenu(programmersSubmenu);
            populateOptionsMenu(optionsMenu);
        }
    }
        
    public void setXPosition(int x) {
        Point p = getLocation(null);
        p.x = x;
        setLocation(p);
    }

    public void setYPosition(int y) {
        Point p = getLocation(null);
        p.y = y;
        setLocation(p);
    }

    public void setWidth(int w) {
        Dimension d = getSize(null);
        d.width = w;
        setSize(d);
    }

    public void setHeight(int h) {
        Dimension d = getSize(null);
        d.height = h;
        setSize(d);
    }

    JLabel updateLabel = new JLabel();
    Timer updateBlockerTimer = null;

    public void startUpdateBlocker() {
        editorPanel.remove(editorTabs);
        updateLabel.setHorizontalAlignment(JLabel.CENTER);
        updateLabel.setVerticalAlignment(JLabel.CENTER);
        editorPanel.add(updateLabel, BorderLayout.CENTER);
        editorPanel.revalidate();
        editorPanel.repaint();
        //pack();

        updateBlockerTimer = new Timer();
        updateBlockerTimer.schedule(new TimerTask() {
            public void run() {
                tickUpdateBlocker();
            }
        }, 10, 50);

    }

    int spinPos = 0;
    public void tickUpdateBlocker() {
        spinPos++;
        if (spinPos == 36) spinPos = 0;
        String iname = "Spinner" + (spinPos * 10);
        ImageIcon i = Base.getIcon("spinner", iname, 64);
        updateLabel.setIcon(i);
    }

    public void stopUpdateBlocker() {
        updateBlockerTimer.cancel();
        editorPanel.remove(updateLabel);
        editorPanel.add(editorTabs, BorderLayout.CENTER);
        editorPanel.revalidate();
        editorPanel.repaint();
        //pack();
    }
}

