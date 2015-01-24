/*
 * Copyright (c) 2014, Majenko Technologies
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
import org.uecide.debug.*;
import org.uecide.editors.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import java.util.*;
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
    JSplitPane manualSplit;

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

    JToolBar toolbar;
    JToolBar treeToolBar;

    JPanel treePanel;
    JPanel editorPanel;
    JPanel statusBar;
    JPanel projectPanel;
    JPanel filesPanel;
    JPanel consolePanel;

    Console console;

    JTabbedPane editorTabs;
    JTabbedPane projectTabs;

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

    JScrollPane consoleScroll;

    JProgressBar statusProgress;
    JLabel statusText;

    JButton abortButton;
    JButton runButton;
    JButton programButton;

    JScrollPane manualScroll;
    Browser manualPane;
    JToolBar manualBar;

    JButton manualButton;

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
        s.attachToEditor(this);

        for(Class<?> plugin : Base.plugins.values()) {
            try {
                Constructor<?> ctor = plugin.getConstructor(Editor.class);
                Plugin p = (Plugin)(ctor.newInstance(new Object[] { this }));
                plugins.add(p);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        this.setLayout(new BorderLayout());

        Base.setIcon(this);

        treePanel = new JPanel();
        projectPanel = new JPanel();
        filesPanel = new JPanel();
        editorPanel = new JPanel();
        consolePanel = new JPanel();
        statusBar = new JPanel();

        treePanel.setLayout(new BorderLayout());
        projectPanel.setLayout(new BorderLayout());
        filesPanel.setLayout(new BorderLayout());
        editorPanel.setLayout(new BorderLayout());
        consolePanel.setLayout(new BorderLayout());
        statusBar.setLayout(new BorderLayout());

        editorTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        projectTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

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

        int width = Base.preferences.getInteger("editor.window.width");

        if(width < Base.preferences.getInteger("editor.window.width.min")) {
            width = Base.preferences.getInteger("editor.window.width.min");
        }

        int height = Base.preferences.getInteger("editor.window.height");

        if(height < Base.preferences.getInteger("editor.window.height.min")) {
            height = Base.preferences.getInteger("editor.window.height.min");
        }

        File manroot = null;
        if (loadedSketch.getCore() != null) {
            manroot = loadedSketch.getCore().getManual();
        }
        manualPane = new Browser(manroot);
        manualScroll = new JScrollPane();
        manualScroll.setViewportView(manualPane);

        leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectPanel, editorPanel);
        manualSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftRightSplit, manualScroll);
        topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, manualSplit, consolePanel);

/*
String sexy = null;
System.err.println(sexy.length());
*/
        manualSplit.setOneTouchExpandable(false);
        leftRightSplit.setOneTouchExpandable(true);
        topBottomSplit.setOneTouchExpandable(true);

        manualScroll.setVisible(false);

        leftRightSplit.setContinuousLayout(true);
        topBottomSplit.setContinuousLayout(true);

        leftRightSplit.setResizeWeight(0D);
        topBottomSplit.setResizeWeight(1D);
        manualSplit.setResizeWeight(1D);

        int dividerSize = Base.preferences.getInteger("editor.manual.split", width - 250);
        manualSplit.setDividerLocation(dividerSize);

        dividerSize = Base.preferences.getInteger("editor.divider.split", height - 250);
        topBottomSplit.setDividerLocation(dividerSize);

        dividerSize = Base.preferences.getInteger("editor.tree.split", 150);
        leftRightSplit.setDividerLocation(dividerSize);

        this.add(topBottomSplit, BorderLayout.CENTER);

        final Editor me = this;

        consoleScroll = new JScrollPane();
        console = new Console();

        console.setURLClickListener(this);

        consoleScroll.setViewportView(console);

        consolePanel.add(consoleScroll);

        toolbar = new JToolBar();
        treeToolBar = new JToolBar();

        toolbar.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                Window window = SwingUtilities.getWindowAncestor(toolbar);

                if(window == Editor.this) {
                    if(e.getChangeFlags() == HierarchyEvent.PARENT_CHANGED) {

                        JPanel pan = (JPanel)e.getChangedParent();

                        BorderLayout layout = (BorderLayout)pan.getLayout();

                        if(toolbar == layout.getLayoutComponent(BorderLayout.NORTH)) {
                            Base.preferences.set("editor.toolbar.position", "n");
                            Base.preferences.saveDelay();
                        }

                        if(toolbar == layout.getLayoutComponent(BorderLayout.SOUTH)) {
                            Base.preferences.set("editor.toolbar.position", "s");
                            Base.preferences.saveDelay();
                        }

                        if(toolbar == layout.getLayoutComponent(BorderLayout.EAST)) {
                            Base.preferences.set("editor.toolbar.position", "e");
                            Base.preferences.saveDelay();
                        }

                        if(toolbar == layout.getLayoutComponent(BorderLayout.WEST)) {
                            Base.preferences.set("editor.toolbar.position", "w");
                            Base.preferences.saveDelay();
                        }
                    }
                } else {
                    if(e.getChangeFlags() == HierarchyEvent.PARENT_CHANGED) {
                        Base.preferences.set("editor.toolbar.position", "f");
                        Base.preferences.saveDelay();
                    }
                }
            }
        });

        statusText = new JLabel("");
        statusProgress = new JProgressBar();

        JPanel sp = new JPanel();

        sp.add(statusText);

        statusBar.add(sp, BorderLayout.WEST);
        statusBar.add(statusProgress, BorderLayout.EAST);

        Font labelFont = statusText.getFont();
        statusText.setFont(new Font(labelFont.getName(), Font.PLAIN, 12));

        String tbPos = Base.preferences.get("editor.toolbar.position");

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
                loadedSketch.rescanFileTree();
                updateTree();
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
        projectTabs.add(treePanel, "Project");
        projectTabs.add(filesPanel, "Files");

        String theme = Base.preferences.get("theme.selected", "default");
        theme = "theme." + theme + ".";

        sketchContentTree.setBackground(Base.theme.getColor(theme + "editor.bgcolor"));
        System.err.println("Tree background: " + Base.theme.getColor(theme + "editor.bgcolor"));
        sketchContentTree.setForeground(Base.theme.getColor(theme + "editor.fgcolor"));
        sketchFilesTree.setBackground(Base.theme.getColor(theme + "editor.bgcolor"));
        sketchFilesTree.setForeground(Base.theme.getColor(theme + "editor.fgcolor"));

        treeScroll.setBackground(Base.theme.getColor(theme + "editor.bgcolor"));
        treePanel.setBackground(Base.theme.getColor(theme + "editor.bgcolor"));

        treeScroll.setOpaque(false);
        treePanel.setOpaque(false);
        sketchContentTree.setOpaque(true);

        File themeFolder = Base.getContentFile("lib/theme");

        abortButton = Editor.addToolbarButton(toolbar, "actions", "cancel", "Abort", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                abortCompilation();
            }
        });
        abortButton.setVisible(false);
        runButton = Editor.addToolbarButton(toolbar, "actions", "run", "Compile (Shift: clean compile)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    loadedSketch.purgeCache();
                    loadedSketch.purgeBuildFiles();
                }
                
                compile();
            }
        });

        programButton = Editor.addToolbarButton(toolbar, "actions", "program", "Program (Shift: clean compile and program)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    loadedSketch.purgeCache();
                    loadedSketch.purgeBuildFiles();
                }
                program();
            }
        });

        toolbar.addSeparator();

        Editor.addToolbarButton(toolbar, "actions", "new", "New Sketch", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleNew();
            }
        });

        Editor.addToolbarButton(toolbar, "actions", "open", "Open Sketch", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleOpenPrompt();
            }
        });

        Editor.addToolbarButton(toolbar, "actions", "save", "Save Sketch", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAllTabs();
            }
        });

        toolbar.addSeparator();

        addPluginsToToolbar(toolbar, Plugin.TOOLBAR_EDITOR);

        toolbar.addSeparator();
        Editor.addToolbarButton(toolbar, "apps", "manual", "Manual", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (manualScroll.isVisible()) {
                    hideManual();
                } else {
                    showManual();
                }
            }
        });

        menuBar = new JMenuBar();

        fileMenu = new JMenu(Translate.t("File"));
        menuBar.add(fileMenu);

        editMenu = new JMenu(Translate.t("Edit"));
        menuBar.add(editMenu);

        sketchMenu = new JMenu(Translate.t("Sketch"));
        menuBar.add(sketchMenu);

        hardwareMenu = new JMenu(Translate.t("Hardware"));
        menuBar.add(hardwareMenu);

        toolsMenu = new JMenu(Translate.t("Tools"));
        menuBar.add(toolsMenu);

        helpMenu = new JMenu(Translate.t("Help"));
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                askCloseWindow();
            }
        });
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        if(Base.preferences.getBoolean("editor.fullscreen")) {
            isFullScreen = true;
            this.dispose();
            setUndecorated(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        this.pack();
        this.setVisible(true);

        setSize(width, height);
        setLocation(Base.preferences.getInteger("editor.window.x"), Base.preferences.getInteger("editor.window.y"));
        setProgress(0);
        updateAll();

        addComponentListener(new ComponentListener() {
            public void componentMoved(ComponentEvent e) {
                Point windowPos = e.getComponent().getLocation(null);
                Base.preferences.setInteger("editor.window.x", windowPos.x);
                Base.preferences.setInteger("editor.window.y", windowPos.y);
                Base.preferences.saveDelay();
            }
            public void componentResized(ComponentEvent e) {
                Dimension windowSize = e.getComponent().getSize(null);
                Base.preferences.setInteger("editor.window.width", windowSize.width);
                Base.preferences.setInteger("editor.window.height", windowSize.height);
                Base.preferences.saveDelay();
            }
            public void componentHidden(ComponentEvent e) {
            }
            public void componentShown(ComponentEvent e) {
            }
        });

        openOrSelectFile(loadedSketch.getMainFile());
        dividerSize = Base.preferences.getInteger("editor.divider.split", height - 250);
        topBottomSplit.setDividerLocation(dividerSize);

        dividerSize = Base.preferences.getInteger("editor.tree.split", 150);
        leftRightSplit.setDividerLocation(dividerSize);

        // We want to do this last as the previous SETs trigger this change listener.

        manualSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    int pos = (Integer)(e.getNewValue());
                    Base.preferences.setInteger("editor.manual.split", pos);
                    Base.preferences.saveDelay();
                }
            }
        );

        leftRightSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    int pos = (Integer)(e.getNewValue());
                    Base.preferences.setInteger("editor.tree.split", pos);
                    Base.preferences.saveDelay();
                }
            }
        );

        topBottomSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    int pos = (Integer)(e.getNewValue());
                    Base.preferences.setInteger("editor.divider.split", pos);
                    Base.preferences.saveDelay();
                }
            }
        );

        if (Base.preferences.getBoolean("manual.split.visible")) {
            showManual();
        }

        for (int i = 0; toolbar.getComponentAtIndex(i) != null; i++) {
            Component c = toolbar.getComponentAtIndex(i);
            if (c instanceof JButton) {
                JButton b = (JButton)c;
                b.setBorderPainted(false);
            }
        }

    }

    public void hideManual() {
        manualScroll.setVisible(false);
        int w = getSize().width;
        int spos = w - manualSplit.getDividerLocation();
        Base.preferences.setInteger("manual.split.position", spos);
        Base.preferences.setBoolean("manual.split.visible", false);
        manualSplit.setDividerLocation(1D);
        Base.preferences.saveDelay();
    }

    public void showManual() {
        if (loadedSketch == null) {
            return;
        }
        if (loadedSketch.getCore() == null) {
            return;
        }
        manualScroll.setVisible(true);
        int w = getSize().width;
        int spos = Base.preferences.getInteger("manual.split.position", 200);
        Base.preferences.setBoolean("manual.split.visible", true);
        manualSplit.setDividerLocation(w - spos);
        Base.preferences.saveDelay();
        manualPane.setRoot(loadedSketch.getCore().getManual());
        manualPane.home();
    }

    class FileCellRenderer implements TreeCellRenderer {
        DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            String theme = Base.preferences.get("theme.selected", "default");
            theme = "theme." + theme + ".";

            Color textColor = Base.theme.getColor(theme + "editor.fgcolor");

            if((value != null) && (value instanceof DefaultMutableTreeNode)) {
                JPanel container = new JPanel();
                container.setLayout(new BorderLayout());
                ImageIcon icon = null;
                UIDefaults defaults = javax.swing.UIManager.getDefaults();
                Color bg = defaults.getColor("List.selectionBackground");
                Color fg = defaults.getColor("List.selectionForeground");
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                Border noBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
                Border paddingBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);

                if (userObject instanceof FlaggedList) {
                    FlaggedList ent = (FlaggedList)userObject;

                    JLabel text = new JLabel(ent.toString());
                    if (ent.getColor() == FlaggedList.Red) {
                        icon = Base.getIcon("flags", "fixme", 16);
                    } else if (ent.getColor() == FlaggedList.Green) {
                        icon = Base.getIcon("flags", "note", 16);
                    } else if (ent.getColor() == FlaggedList.Yellow) {
                        icon = Base.getIcon("flags", "todo", 16);
                    } else if (ent.getColor() == FlaggedList.Blue) {
                        icon = Base.getIcon("flags", "info", 16);
                    }

                    text.setBorder(paddingBorder);

                    if(selected) {
                        text.setBackground(bg);
                        text.setForeground(fg);
                        text.setOpaque(true);
                    } else {
                        text.setOpaque(false);
                        text.setForeground(textColor);
                    }

                    container.setOpaque(false);
                    container.setBorder(noBorder);

                    if(icon != null) {
                        JLabel i = new JLabel(icon);
                        container.add(i, BorderLayout.WEST);
                    }

                    Font f = text.getFont();
                    text.setFont(new Font(f.getFamily(), Font.PLAIN, f.getSize() - 2));
                    container.add(text, BorderLayout.CENTER);
                    return container;
                }

                if (userObject instanceof TodoEntry) {
                    TodoEntry ent = (TodoEntry)userObject;

                    JLabel text = new JLabel(ent.toString());
                    icon = Base.getIcon("bookmarks", "todo", 16);
                    text.setBorder(paddingBorder);

                    if(selected) {
                        text.setBackground(bg);
                        text.setForeground(fg);
                        text.setOpaque(true);
                    } else {
                        text.setOpaque(false);
                        text.setForeground(textColor);
                    }

                    container.setOpaque(false);
                    container.setBorder(noBorder);

                    if(icon != null) {
                        JLabel i = new JLabel(icon);
                        container.add(i, BorderLayout.WEST);
                    }

                    Font f = text.getFont();
                    text.setFont(new Font(f.getFamily(), Font.PLAIN, f.getSize() - 2));
                    container.add(text, BorderLayout.CENTER);
                    return container;
                }

                if(userObject instanceof FunctionBookmark) {
                    FunctionBookmark bm = (FunctionBookmark)userObject;
                    JLabel text = new JLabel(bm.formatted());
                    icon = Base.getIcon("bookmarks", "function", 16);
                    text.setBorder(paddingBorder);

                    if(selected) {
                        text.setBackground(bg);
                        text.setForeground(fg);
                        text.setOpaque(true);
                    } else {
                        text.setOpaque(false);
                        text.setForeground(textColor);
                    }

                    container.setOpaque(false);
                    container.setBorder(noBorder);

                    if(icon != null) {
                        JLabel i = new JLabel(icon);
                        container.add(i, BorderLayout.WEST);
                    }

                    Font f = text.getFont();
                    text.setFont(new Font(f.getFamily(), Font.PLAIN, f.getSize() - 2));
                    container.add(text, BorderLayout.CENTER);
                    return container;
                }

                if(userObject instanceof File) {
                    File file = (File)userObject;
                    JLabel text = new JLabel(file.getName());

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
                                error(e);
                            }
                        }

                        icon = (ImageIcon)oicon;
                    }

                    text.setBorder(paddingBorder);

                    if(selected) {
                        text.setBackground(bg);
                        text.setForeground(fg);
                        text.setOpaque(true);
                    } else {
                        text.setOpaque(false);
                        text.setForeground(textColor);
                    }

                    container.setOpaque(false);
                    container.setBorder(noBorder);

                    if(icon != null) {
                        JLabel i = new JLabel(icon);
                        container.add(i, BorderLayout.WEST);
                    }

                    container.add(text, BorderLayout.CENTER);
                    return container;
                }

                if(userObject instanceof Library) {
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

                    container.setOpaque(false);
                    container.setBorder(noBorder);
                    JLabel i = new JLabel(icon);
                    container.add(i, BorderLayout.WEST);

                    if(pct > 0 && pct < 100) {
                        JProgressBar bar = new JProgressBar();
                        bar.setString(lib.getName());
                        Dimension d = bar.getSize();
                        d.width = 80;
                        //bar.setSize(d);
                        bar.setPreferredSize(d);
                        //bar.setMinimumSize(d);
                        //bar.setMaximumSize(d);
                        bar.setStringPainted(true);
                        bar.setValue(pct);
                        container.add(bar, BorderLayout.CENTER);
                    } else {
                        JLabel text = new JLabel(lib.getName());
                        text.setBorder(paddingBorder);

                        if(selected) {
                            text.setBackground(bg);
                            text.setForeground(fg);
                            text.setOpaque(true);
                        } else {
                            text.setOpaque(false);
                            text.setForeground(textColor);
                        }

                        container.add(text, BorderLayout.CENTER);
                    }

                    return container;
                }

                if(userObject instanceof Sketch) {
                    Sketch so = (Sketch)userObject;
                    JLabel text = new JLabel(so.getName());
                    icon = so.getIcon(16);

                    if(icon == null) {
                        icon = Base.loadIconFromResource("icon16.png");
                    }

                    text.setBorder(paddingBorder);

                    if(selected) {
                        text.setBackground(bg);
                        text.setForeground(fg);
                        text.setOpaque(true);
                    } else {
                        text.setOpaque(false);
                        text.setForeground(textColor);
                    }

                    container.setOpaque(false);
                    container.setBorder(noBorder);
                    JLabel i = new JLabel(icon);
                    container.add(i, BorderLayout.WEST);
                    container.add(text, BorderLayout.CENTER);
                    return container;
                }

                JLabel text = new JLabel(userObject.toString());

                if(expanded) {
                    icon = Base.getIcon("bookmarks", "folder-open", 16);
                } else {
                    icon = Base.getIcon("bookmarks", "folder", 16);
                }

                text.setBorder(paddingBorder);

                if(selected) {
                    text.setBackground(bg);
                    text.setForeground(fg);
                    text.setOpaque(true);
                } else {
                    text.setOpaque(false);
                    text.setForeground(textColor);
                }

                container.setOpaque(false);
                container.setBorder(noBorder);
                JLabel i = new JLabel(icon);
                container.add(i, BorderLayout.WEST);
                container.add(text, BorderLayout.CENTER);
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

        Arrays.sort(buildFiles);

        for(File file : buildFiles) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(file.getName());
            node.setUserObject(file);

            if(file.isDirectory()) {
                addFileTreeToNode(node, file);
            }

            treenode.add(node);
        };
    }


    public void initTreeStructure() {
        treeRoot = new DefaultMutableTreeNode(loadedSketch.getName());
        treeRoot.setUserObject(loadedSketch);
        treeModel = new DefaultTreeModel(treeRoot);
        sketchContentTree = new JTree(treeModel);
        treeSource = new DefaultMutableTreeNode("Source");
        treeHeaders = new DefaultMutableTreeNode("Headers");
        treeLibraries = new DefaultMutableTreeNode("Libraries");
        treeBinaries = new DefaultMutableTreeNode("Binaries");
        treeOutput = new DefaultMutableTreeNode("Output");
        treeDocs = new DefaultMutableTreeNode("Documentation");
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
        Font font        = Base.preferences.getFont("tree.font");
        sketchContentTree.setFont(font);
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
                ee.printStackTrace();
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

                            ArrayList<TodoEntry> todo = loadedSketch.todo(f);
                            if (todo != null && todo.size() > 0) {

                                FlaggedList noteList = new FlaggedList(FlaggedList.Green, "Notes");
                                FlaggedList todoList = new FlaggedList(FlaggedList.Yellow, "To Do");
                                FlaggedList fixmeList = new FlaggedList(FlaggedList.Red, "Fix Me");

                                EditorBase eb = null;
                                int tab = getTabByFile(f);
                                if (tab != -1) {
                                    eb = getTab(tab);
                                    eb.removeFlagGroup(0x2000);
                                }

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

                if(o.getUserObject().getClass().equals(String.class)) {
                    String s = (String)o.getUserObject();

                    if(s.equals(loadedSketch.getName())) {
                        JMenuItem openInOS = new JMenuItem("Open in OS");
                        openInOS.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                Base.openURL(loadedSketch.getFolder().getAbsolutePath());
                            }
                        });
                        menu.add(openInOS);

                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals("Source")) {
                        JMenuItem item = new JMenuItem("Create sketch file (.ino)");
                        item.setActionCommand("ino");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem("Create C++ source file");
                        item.setActionCommand("cpp");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem("Create C source file");
                        item.setActionCommand("c");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem("Create assembly source file");
                        item.setActionCommand("S");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem("Import source file");
                        item.setActionCommand("source");
                        item.addActionListener(importFileAction);
                        menu.add(item);

                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals("Headers")) {
                        JMenuItem item = new JMenuItem("Create header file");
                        item.setActionCommand("h");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem("Import header file");
                        item.setActionCommand("header");
                        item.addActionListener(importFileAction);
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
                    } else if(s.equals("Binaries")) {
                        JMenuItem item = new JMenuItem("Add binary file");
                        item.setActionCommand("binary");
                        item.addActionListener(importFileAction);
                        menu.add(item);

                        populateContextMenu(menu, Plugin.MENU_TREE_BINARIES | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_BINARIES | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_BINARIES | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals("Output")) {
                        JMenuItem item = new JMenuItem("Purge output files");
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                loadedSketch.purgeBuildFiles();
                                updateOutputTree();
                            }
                        });
                        menu.add(item);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_BOTTOM, o);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals("Documentation")) {
                        JMenuItem item = new JMenuItem("Create Markdown file");
                        item.setActionCommand("md");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_BOTTOM, o);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    }

                    // Otherwise, if the user object is a File, then it must be one of the entries in one
                    // of the groups, except the libraries group.
                } else if(o.getUserObject().getClass().equals(File.class)) {
                    File thisFile = (File)o.getUserObject();

                    String ee = Base.preferences.get("editor.external.command");

                    if(ee != null && !ee.equals("")) {
                        JMenuItem openExternal = new JMenuItem("Open in external editor");
                        openExternal.setActionCommand(thisFile.getAbsolutePath());
                        openExternal.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                String cmd = Base.preferences.get("editor.external.command");
                                String fn = e.getActionCommand();
                                loadedSketch.settings.put("filename", fn);
                                String c = loadedSketch.parseString(cmd);
                                Base.exec(c.split("::"));
                            }
                        });
                        menu.add(openExternal);
                    }

                    JMenuItem renameItem = new JMenuItem("Rename file");
                    renameItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();

                            if(f.equals(mf)) {
                                alert("You cannot rename the main sketch file.\nUse \"Save As\" instead.");
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
                    menu.add(renameItem);

                    JMenuItem deleteItem = new JMenuItem("Delete file");
                    deleteItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();

                            if(f.equals(mf)) {
                                alert("You cannot delete the main sketch file.");
                                return;
                            }

                            if(twoOptionBox(JOptionPane.QUESTION_MESSAGE, "Delete?", "Are you sure you want to delete\n" + f.getName() + "?", "Yes", "No") == 0) {
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
                    menu.add(deleteItem);

                    populateContextMenu(menu, Plugin.MENU_TREE_FILE | Plugin.MENU_TOP, o);
                    menu.addSeparator();

                    JMenu infoMenu = new JMenu("Info");
                    JMenuItem filePath = new JMenuItem(thisFile.getAbsolutePath());
                    filePath.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            StringSelection sel = new StringSelection(e.getActionCommand());
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(sel, sel);
                            message("Path copied to clipboard.");
                        }
                    });
                    filePath.setActionCommand(thisFile.getAbsolutePath());
                    infoMenu.add(filePath);
                    JMenuItem fileSize = new JMenuItem(thisFile.length() + " bytes");
                    infoMenu.add(fileSize);
                    menu.add(infoMenu);

                    populateContextMenu(menu, Plugin.MENU_TREE_FILE | Plugin.MENU_MID, o);

                    if(p.getUserObject().getClass().equals(String.class)) {
                        String ptext = (String)p.getUserObject();

                        if(ptext.equals("Binaries")) {
                            JMenuItem insertRef = new JMenuItem("Insert reference");
                            insertRef.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    String filename = e.getActionCommand();
                                    filename = filename.replaceAll("\\.", "_");
                                    int at = getActiveTab();

                                    if(at > -1) {
                                        EditorBase eb = getTab(at);
                                        eb.insertAtCursor("extern char " + filename + "[] asm(\"_binary_objects_" + filename + "_start\");\n");
                                    }
                                }
                            });
                            insertRef.setActionCommand(thisFile.getName());
                            menu.add(insertRef);
                        }
                    }

                    populateContextMenu(menu, Plugin.MENU_TREE_FILE | Plugin.MENU_BOTTOM, o);

                    menu.show(sketchContentTree, e.getX(), e.getY());

                    // Otherwise it might be a library.  This generates the menu entries for a
                    // library object.

                } else if(o.getUserObject().getClass().equals(Library.class)) {
                    final Library lib = (Library)(o.getUserObject());
                    JMenuItem item = new JMenuItem("Delete cached archive");
                    item.setEnabled(loadedSketch.libraryIsCompiled(lib));
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            loadedSketch.purgeLibrary(lib);
                            updateLibrariesTree();
                        }
                    });
                    menu.add(item);
                    item = new JMenuItem("Recompile now");
                    item.setEnabled(!compilerRunning());
                    item.setActionCommand("yes");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            loadedSketch.purgeLibrary(lib);
                            clearConsole();

                            if(compilerRunning()) {
                                error("Sorry, there is already a compiler thread running for this sketch.");
                                return;
                            }

                            LibCompileRunHandler runHandler = new LibCompileRunHandler(lib);
                            compilationThread = new Thread(runHandler, "Compiler");
                            compilationThread.start();
                        }
                    });
                    menu.add(item);

                    if(lib.isLocal(loadedSketch.getFolder())) {
                        item = new JMenuItem("Export library...");
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                exportLocalLibrary(lib);
                            }
                        });
                        menu.add(item);
                    } else {
                        item = new JMenuItem("Localize library");
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                if(loadedSketch.parentIsProtected()) {
                                    error("You cannot localize a library in an example. Use Save As first.");
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
                        menu.add(item);
                    }

                    menu.show(sketchContentTree, e.getX(), e.getY());
                }

                return;
            }

            if(selRow != -1) {
                if(e.getClickCount() == 2) {
                    DefaultMutableTreeNode o = (DefaultMutableTreeNode)selPath.getLastPathComponent();

                    if(o.getUserObject().getClass().equals(File.class)) {
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

                if(o.getUserObject().getClass().equals(File.class)) {
                    File thisFile = (File)o.getUserObject();
                    JPopupMenu menu = new JPopupMenu();

                    if(thisFile.isDirectory()) {
                        JMenuItem openInOS = new JMenuItem("Open in OS");
                        openInOS.setActionCommand(thisFile.getAbsolutePath());
                        openInOS.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                Base.openURL(e.getActionCommand());
                            }
                        });
                        menu.add(openInOS);

                        JMenuItem mkdirItem = new JMenuItem("Create directory");
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
                        menu.add(mkdirItem);
                        JMenuItem unzipItem = new JMenuItem("Extract ZIP file here");
                        unzipItem.setActionCommand(thisFile.getAbsolutePath());
                        unzipItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                findAndUnzipZipFile(e.getActionCommand());
                            }
                        });
                        menu.add(unzipItem);
                    } else {
                        String ee = Base.preferences.get("editor.external.command");

                        if(ee != null && !ee.equals("")) {
                            JMenuItem openExternal = new JMenuItem("Open in external editor");
                            openExternal.setActionCommand(thisFile.getAbsolutePath());
                            openExternal.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    String cmd = Base.preferences.get("editor.external.command");
                                    String fn = e.getActionCommand();
                                    loadedSketch.settings.put("filename", fn);
                                    String c = loadedSketch.parseString(cmd);
                                    Base.exec(c.split("::"));
                                }
                            });
                            menu.add(openExternal);
                        }
                    }

                    populateContextMenu(menu, Plugin.MENU_FILE_FILE | Plugin.MENU_TOP, o);

                    JMenuItem renameItem = new JMenuItem("Rename file");
                    renameItem.setActionCommand(thisFile.getAbsolutePath());
                    renameItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();

                            if(f.equals(mf)) {
                                alert("You cannot rename the main sketch file.\nUse \"Save As\" instead.");
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
                    menu.add(renameItem);

                    JMenuItem deleteItem = new JMenuItem("Delete file");
                    deleteItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();

                            if(f.equals(mf)) {
                                alert("You cannot delete the main sketch file.");
                                return;
                            }

                            if(twoOptionBox(JOptionPane.QUESTION_MESSAGE, "Delete?", "Are you sure you want to delete\n" + f.getName() + "?", "Yes", "No") == 0) {
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

                    menu.add(deleteItem);

                    menu.addSeparator();
                    populateContextMenu(menu, Plugin.MENU_FILE_FILE | Plugin.MENU_MID, o);

                    JMenu infoMenu = new JMenu("Info");
                    JMenuItem filePath = new JMenuItem(thisFile.getAbsolutePath());
                    filePath.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            StringSelection sel = new StringSelection(e.getActionCommand());
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(sel, sel);
                            message("Path copied to clipboard.");
                        }
                    });
                    filePath.setActionCommand(thisFile.getAbsolutePath());
                    infoMenu.add(filePath);
                    JMenuItem fileSize = new JMenuItem(thisFile.length() + " bytes");
                    infoMenu.add(fileSize);
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

                    if(o.getUserObject().getClass().equals(File.class)) {
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

        return loadedSketch.getBoard();
    }

    public Core getCore() {
        if(loadedSketch == null) {
            return null;
        }

        return loadedSketch.getCore();
    }

    public Compiler getCompiler() {
        if(loadedSketch == null) {
            return null;
        }

        return loadedSketch.getCompiler();
    }

    public String getSketchName() {
        return loadedSketch.getName();
    }

    public Sketch getSketch() {
        return loadedSketch;
    }


    String mBuffer = "";
    public void messageStream(String msg) {
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
    }

    String wBuffer = "";
    public void warningStream(String msg) {
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
    }

    String eBuffer = "";
    public void errorStream(String msg) {
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

    public void command(String msg) {
        Debug.message(msg);

        if(msg == null) {
            return;
        }

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.COMMAND);
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
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        error(sw.toString());
    }


    public void clearConsole() {
        console.clear();
    }

    public void setProgress(int x) {
        statusProgress.setValue(x);
    }

    public void setStatus() {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><b>Board: </b><i>");
        sb.append((loadedSketch.getBoard() != null ? loadedSketch.getBoard() : "None"));
        sb.append("</i> <b>Core: </b><i>");
        sb.append((loadedSketch.getCore() != null ? loadedSketch.getCore() : "None"));
        sb.append("</i> <b>Compiler: </b><i>");
        sb.append((loadedSketch.getCompiler() != null ? loadedSketch.getCompiler() : "None"));
        sb.append("</i> <b>Port: </b><i>");
        sb.append((loadedSketch.getProgramPort() != null ? loadedSketch.getProgramPort() : "None"));
        sb.append("</i></html>");
        statusText.setText(sb.toString());
        statusText.setOpaque(false);
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

    public boolean closeTab(int tab) {
        if(tab == -1) return false;

        if(editorTabs.getComponentAt(tab) instanceof EditorBase) {
            EditorBase eb = (EditorBase)editorTabs.getComponentAt(tab);

            if(eb.isModified()) {
                int option = threeOptionBox(
                                 JOptionPane.WARNING_MESSAGE,
                                 Translate.t("Unsaved File"),
                                 Translate.w("This file has been modified.  Do you want to save your work before you close this tab?", 40, "\n"),
                                 "Save",
                                 "Don't Save",
                                 "Cancel"
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
            error("Sorry, I can't find " + sf.getName() + ".");
            return -1;
        }

        try {
            String className = FileType.getEditor(sf.getName());

            if(className == null) {
                error("File type for " + sf.getName() + " unknown");
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

        loadedSketch.saveConfig();
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
        JOptionPane.showMessageDialog(this, message, "Alert", JOptionPane.WARNING_MESSAGE);
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

        Base.preferences.save();

        if(editorList.size() == 1) {
            if(Base.isMacOS()) {
                Object[] options = { Translate.t("OK"), Translate.t("Cancel") };
                String prompt =
                    "Are you sure you want to Quit?\n" + 
                    "Closing the last open sketch will quit UECIDE.";

                int result = JOptionPane.showOptionDialog(this,
                             prompt,
                             Translate.t("Quit"),
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
            Base.preferences.save();
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
                                error("Unable to find file " + path);
                            }
                        }
                    });
                    item.setActionCommand(file.getAbsolutePath());
                    menu.add(item);
                } else {
                    JMenu submenu = new JMenu(file.getName());
                    addSketchesFromFolder(submenu, file);

                    if(submenu.getItemCount() > 0) {
                        menu.add(submenu);
                    }
                }
            }
        }
    }

    public void updateMenus() {
        fileMenu.removeAll();
        editMenu.removeAll();
        sketchMenu.removeAll();
        hardwareMenu.removeAll();
        toolsMenu.removeAll();
        helpMenu.removeAll();

        JMenuItem item;
        JMenu submenu;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        item = new JMenuItem(Translate.t("New"));
        item.setAccelerator(KeyStroke.getKeyStroke('N', modifiers));
        fileMenu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleNew();
            }
        });


        item = new JMenuItem(Translate.t("Open..."));
        item.setAccelerator(KeyStroke.getKeyStroke('O', modifiers));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleOpenPrompt();
            }
        });
        fileMenu.add(item);

        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_TOP);

        submenu = new JMenu(Translate.t("Recent Sketches"));
        fileMenu.add(submenu);

        for(File m : Base.MRUList) {
            item = new JMenuItem(m.getName());
            item.setToolTipText(m.getAbsolutePath());
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String path = e.getActionCommand();

                    if(new File(path).exists()) {
                        loadSketch(path);
                    } else {
                        error("Unable to find file " + path);
                    }
                }
            });
            item.setActionCommand(m.getAbsolutePath());
            submenu.add(item);
        }


        submenu = new JMenu(Translate.t("Examples"));

        if(loadedSketch.getCore() != null) {
            if (loadedSketch.getCompiler() != null) {
                addSketchesFromFolder(submenu, loadedSketch.getCompiler().getExamplesFolder());
            }
            if (loadedSketch.getCore() != null) {
                addSketchesFromFolder(submenu, loadedSketch.getCore().getExamplesFolder());
            }
            if (loadedSketch.getBoard() != null) {
                addSketchesFromFolder(submenu, loadedSketch.getBoard().getExamplesFolder());
            }

            submenu.addSeparator();


            TreeSet<String> catNames = Library.getLibraryCategories();

            if (loadedSketch.getCore() != null) {
                for(String group : catNames) {
                    TreeSet<Library>libs = Library.getLibraries(group, loadedSketch.getCore().getName());

                    if(libs != null && libs.size() > 0) {
                        JMenu top = new JMenu(Library.getCategoryName(group));

                        for(Library lib : libs) {
                            JMenu libMenu = new JMenu(lib.getName());
                            addSketchesFromFolder(libMenu, lib.getExamplesFolder());

                            if(libMenu.getItemCount() > 0) {
                                top.add(libMenu);
                            }
                        }

                        submenu.add(top);
                    }
                }
            }
        }

        fileMenu.add(submenu);

        fileMenu.addSeparator();
        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_MID);

        item = new JMenuItem(Translate.t("Close"));
        item.setAccelerator(KeyStroke.getKeyStroke('W', modifiers));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                askCloseWindow();
            }
        });
        fileMenu.add(item);

        item = new JMenuItem(Translate.t("Save"));
        item.setAccelerator(KeyStroke.getKeyStroke('S', modifiers));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAllTabs();
            }
        });
        fileMenu.add(item);

        item = new JMenuItem(Translate.t("Save As..."));
        item.setAccelerator(KeyStroke.getKeyStroke('S', modifiers | ActionEvent.SHIFT_MASK));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        });
        fileMenu.add(item);

        item = new JMenuItem(Translate.t("Export as SAR..."));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportSar();
            }
        });
        fileMenu.add(item);

        item = new JMenuItem(Translate.t("Import SAR..."));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importSar();
            }
        });
        fileMenu.add(item);


        fileMenu.addSeparator();
        item = new JMenuItem(Translate.t("Preferences"));
        item.setAccelerator(KeyStroke.getKeyStroke("ctrl-,"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Preferences prefs = new Preferences(Editor.this);
                prefs.showFrame();
            }
        });
        fileMenu.add(item);


        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_BOTTOM);

        item = new JMenuItem(Translate.t("Quit"));
        item.setAccelerator(KeyStroke.getKeyStroke("alt Q"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(Editor.closeAllEditors()) {
                    Base.preferences.save();
                    System.exit(0);
                }
            }
        });
        fileMenu.add(item);

        addMenuChunk(editMenu, Plugin.MENU_EDIT | Plugin.MENU_TOP);
        editMenu.addSeparator();
        addMenuChunk(editMenu, Plugin.MENU_EDIT | Plugin.MENU_MID);
        editMenu.addSeparator();
        item = new JMenuItem("Toggle Full Screen");
        item.setAccelerator(KeyStroke.getKeyStroke("F11"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleFullScreen();
            }
        });

        editMenu.add(item);

        addMenuChunk(editMenu, Plugin.MENU_EDIT | Plugin.MENU_BOTTOM);

        ActionListener createNewAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createNewSketchFile(e.getActionCommand());
            }
        };

        item = new JMenuItem(Translate.t("Compile"));
        item.setAccelerator(KeyStroke.getKeyStroke('R', modifiers));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compile();
            }
        });
        sketchMenu.add(item);
        
        item = new JMenuItem(Translate.t("Compile and Program"));
        item.setAccelerator(KeyStroke.getKeyStroke('U', modifiers));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                program();
            }
        });
        sketchMenu.add(item);
        
        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_TOP);
        sketchMenu.addSeparator();

        submenu = new JMenu("Create new");
        item = new JMenuItem("Sketch file (.ino)");
        item.setActionCommand("ino");
        item.addActionListener(createNewAction);
        submenu.add(item);
        item = new JMenuItem("C++ file");
        item.setActionCommand("cpp");
        item.addActionListener(createNewAction);
        submenu.add(item);
        item = new JMenuItem("C file");
        item.setActionCommand("c");
        item.addActionListener(createNewAction);
        submenu.add(item);
        item = new JMenuItem("Header file");
        item.setActionCommand("h");
        item.addActionListener(createNewAction);
        submenu.add(item);
        item = new JMenuItem("Assembly file");
        item.setActionCommand("S");
        item.addActionListener(createNewAction);
        submenu.add(item);
        item = new JMenuItem("Library");
        item.setActionCommand("lib");
        item.addActionListener(createNewAction);
        submenu.add(item);
        sketchMenu.add(submenu);


        submenu = new JMenu("Import file");
        item = new JMenuItem("Source file");
        submenu.add(item);
        item = new JMenuItem("Header file");
        submenu.add(item);
        item = new JMenuItem("Binary file");
        submenu.add(item);
        sketchMenu.add(submenu);

        submenu = new JMenu("Libraries");
        item = new JMenuItem("Install library archive...");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                installLibraryArchive();
            }
        });
        submenu.add(item);
        submenu.addSeparator();
        populateLibrariesMenu(submenu);
        sketchMenu.add(submenu);


        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_MID);
        sketchMenu.addSeparator();
        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_BOTTOM);

        item = new JMenuItem("Sketch properties...");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new SketchProperties(Editor.this, loadedSketch);
            }
        });
        sketchMenu.add(item);

        submenu = new JMenu("Boards");
        populateBoardsMenu(submenu);
        hardwareMenu.add(submenu);

        submenu = new JMenu("Cores");
        populateCoresMenu(submenu);
        hardwareMenu.add(submenu);

        submenu = new JMenu("Compilers");
        populateCompilersMenu(submenu);
        hardwareMenu.add(submenu);

        optionsMenu = new JMenu("Options");
        populateOptionsMenu(optionsMenu);
        optionsMenu.setEnabled(optionsMenu.getItemCount() > 0);
        hardwareMenu.add(optionsMenu);

        serialPortsMenu = new JMenu("Serial Port");
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
        hardwareMenu.add(serialPortsMenu);

        discoveredBoardsMenu = new JMenu("Discovered Boards");
        populateDiscoveredBoardsMenu(discoveredBoardsMenu);
        discoveredBoardsMenu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                populateDiscoveredBoardsMenu(discoveredBoardsMenu);
            }
            public void menuCanceled(MenuEvent e) {
            }
            public void menuDeselected(MenuEvent e) {
            }
        });
        hardwareMenu.add(discoveredBoardsMenu);



        submenu = new JMenu("Programmers");
        populateProgrammersMenu(submenu);
        hardwareMenu.add(submenu);

        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_TOP);
        hardwareMenu.addSeparator();
        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_MID);
        hardwareMenu.addSeparator();
        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_BOTTOM);

        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_TOP);
        toolsMenu.addSeparator();
        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_MID);
        item = new JMenuItem("Service Manager");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ServiceManager.open(Editor.this);
            }
        });
        toolsMenu.add(item);

        toolsMenu.addSeparator();
        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_BOTTOM);

        item = new JMenuItem("About " + Base.theme.get("product.cap"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleAbout();
            }
        });
        helpMenu.add(item);
        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_TOP);
        helpMenu.addSeparator();

        PropertyFile links = Base.theme.getChildren("links");

        for(String link : links.childKeys()) {
            item = new JMenuItem(links.get(link + ".name"));
            item.setActionCommand(links.get(link + ".url"));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String link = e.getActionCommand();
                    Base.openURL(link);
                }
            });
            helpMenu.add(item);
        }

        links = loadedSketch.mergeAllProperties().getChildren("links");

        for(String link : links.childKeys()) {
            String iname = links.get(link + ".name");

            if(iname != null) {
                item = new JMenuItem(iname);
                item.setActionCommand(links.get(link + ".url"));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String link = e.getActionCommand();
                        Base.openURL(link);
                    }
                });
                helpMenu.add(item);
            }
        }

        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_MID);
        helpMenu.addSeparator();
        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_BOTTOM);
        submenu = new JMenu("Debug");

        item = new JMenuItem("Debug Console");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.show();
            }
        });
        submenu.add(item);
        item = new JMenuItem("Rebuild internal structures");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.cleanAndScanAllSettings();
            }
        });
        submenu.add(item);
        item = new JMenuItem("Purge cache files");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadedSketch.purgeCache();
            }
        });
        submenu.add(item);
        item = new JMenuItem("Open data folder");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.openURL(Base.getSettingsFolder().getAbsolutePath());
            }
        });
        submenu.add(item);
        helpMenu.add(submenu);

    }

    public void populateOptionsMenu(JMenu menu) {
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

                submenu.add(item);
            }

            menu.add(submenu);
        }
    }

    public void populateSerialMenu(JMenu menu) {
        menu.removeAll();
        ButtonGroup portGroup = new ButtonGroup();
        ArrayList<String> ports = Serial.getPortList();

        for(String port : ports) {
            String pn = Serial.getName(port);
            JMenuItem item = null;

            if(pn != null & !pn.equals("")) {
                item = new JRadioButtonMenuItem(port + ": " + pn);
            } else {
                item = new JRadioButtonMenuItem(port);
            }

            portGroup.add(item);
            item.setSelected(port.equals(loadedSketch.getSerialPort()));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setSerialPort(e.getActionCommand());
                }
            });
            item.setActionCommand(port);
            menu.add(item);
        }
    }

    public class DiscoveredBoardAction extends AbstractAction {
        DiscoveredBoard discoveredBoard;

        public DiscoveredBoardAction(DiscoveredBoard b) {
            super(b.toString());
            discoveredBoard = b;
        }

        public void actionPerformed(ActionEvent e) {
            System.err.println(discoveredBoard.toString());
            loadedSketch.setBoard(discoveredBoard.board);
            loadedSketch.setPort(discoveredBoard.location);

            if(discoveredBoard.programmer != null) {
                loadedSketch.setProgrammer(discoveredBoard.programmer);
            }

            for(Object k : discoveredBoard.properties.keySet()) {
                loadedSketch.put("mdns." + (String)k, discoveredBoard.properties.get((String)k));
            }
            manualPane.setRoot(loadedSketch.getCore().getManual());
            manualPane.home();
        }
    }

    public void populateDiscoveredBoardsMenu(JMenu menu) {
        menu.removeAll();
        ButtonGroup portGroup = new ButtonGroup();

        for(Object ob : Base.discoveredBoards.keySet()) {
            DiscoveredBoard b = Base.discoveredBoards.get(ob);

            JMenuItem item = new JMenuItem(new DiscoveredBoardAction(b));
            ImageIcon i = b.board.getIcon(16);

            if(i == null) {
                Core c = b.board.getCore();

                if(c != null) {
                    i = c.getIcon(16);
                }
            }

            if(i != null) {
                item.setIcon(i);
            }

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
        TreeMap<String, String> programmers = loadedSketch.getProgrammerList();
        ButtonGroup programmerGroup = new ButtonGroup();

        for(String pn : programmers.keySet()) {
            JMenuItem item = new JRadioButtonMenuItem(programmers.get(pn));
            programmerGroup.add(item);
            if (loadedSketch.getProgrammer() != null) {
                item.setSelected(loadedSketch.getProgrammer().equals(pn));
            }
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setProgrammer(e.getActionCommand());
                }
            });
            item.setActionCommand(pn);
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

            if(loadedSketch.getBoard() != null) {
                if(loadedSketch.getBoard().equals(board)) {
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
                    manualPane.setRoot(loadedSketch.getCore().getManual());
                    manualPane.home();
                }
            });
            item.setActionCommand(board.getName());
            menu.add(item);
        }
    }

    public void populateCoresMenu(JMenu menu) {
        ButtonGroup coreGroup = new ButtonGroup();
        Board board = loadedSketch.getBoard();
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

            if(loadedSketch.getCore() != null) {
                item.setSelected(loadedSketch.getCore().equals(core));
            }

            ImageIcon i = core.getIcon(16);

            if(i != null) {
                item.setIcon(i);
            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setCore(e.getActionCommand());
                    manualPane.setRoot(loadedSketch.getCore().getManual());
                    manualPane.home();
                }
            });
            item.setActionCommand(core.getName());
            menu.add(item);
        }

    }

    public void populateCompilersMenu(JMenu menu) {
        ButtonGroup compilerGroup = new ButtonGroup();
        Core core = loadedSketch.getCore();
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
            JMenuItem item = new JRadioButtonMenuItem(compiler.getName());
            compilerGroup.add(item);
            item.setSelected(loadedSketch.getCompiler().equals(compiler));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setCompiler(e.getActionCommand());
                }
            });
            item.setActionCommand(compiler.getName());
            menu.add(item);
        }
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
                error(e);
            }
        }
    }

    public void addPluginsToMenu(JMenu menu, int filterFlags) {
        for(Plugin plugin : plugins) {
            try {
                plugin.populateMenu(menu, filterFlags);
            } catch(AbstractMethodError e) {
            } catch(Exception e) {
                error(e);
            }
        }
    }

    public void addPluginsToToolbar(JToolBar tb, int filterFlags) {
        for(final Plugin plugin : plugins) {
            try {
                plugin.addToolbarButtons(tb, filterFlags);
            } catch(AbstractMethodError e) {
            } catch(Exception e) {
                error(e);
            }
        }
    }

    public void updateAll() {
        updateMenus();
        updateTree();
        setStatus();
    }

    public String getSerialPort() {
        return loadedSketch.getSerialPort();
    }

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

        fc.setCurrentDirectory(Base.getSketchbookFolder());

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            try {
                File sarFile = fc.getSelectedFile();
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

                JButton cancel = new JButton(Translate.t("Cancel"));
                cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        me.willDoImport = false;
                        dialog.dispose();
                    }
                });
                JButton impt = new JButton(Translate.t("Import"));
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

        int rv = fc.showSaveDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            newFile = fc.getSelectedFile();
            loadedSketch.generateSarFile(newFile);
        }
    }

    public void saveAs() {
        JFileChooser fc = new JFileChooser();
        javax.swing.filechooser.FileFilter filter = new SketchFolderFilter();
        fc.setFileFilter(filter);

        javax.swing.filechooser.FileView view = new SketchFileView();
        fc.setFileView(view);

        fc.setCurrentDirectory(Base.getSketchbookFolder());
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        int rv = fc.showSaveDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            File newFile = fc.getSelectedFile();

            if(newFile.exists()) {
                int n = twoOptionBox(
                            JOptionPane.WARNING_MESSAGE,
                            "Overwrite File?",
                            Translate.w("Do you really want to overwrite the file %1?", 40, "\n", newFile.getName()),
                            "Yes", "No");

                if(n != 0) {
                    return;
                }

                newFile.delete();
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
                          "Enter filename",
                          "Create new ." + extension + " file",
                          JOptionPane.PLAIN_MESSAGE
                      );

        while(!validName(name)) {
            if(name == null) {
                return;
            }

            JOptionPane.showMessageDialog(this,
                                          "Invalid Filename",
                                          "The filename contains invalid characters.",
                                          JOptionPane.ERROR_MESSAGE);
            name = (String)JOptionPane.showInputDialog(
                       this,
                       "Enter filename",
                       "Create new ." + extension + " file",
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

        if(importFileDefaultDir == null) {
            importFileDefaultDir = new File(System.getProperty("user.dir"));
        }

        fc.setCurrentDirectory(importFileDefaultDir);

        int r = fc.showOpenDialog(this);

        if(r == JFileChooser.APPROVE_OPTION) {
            File src = fc.getSelectedFile();
            importFileDefaultDir = src.getParentFile();

            if(!src.exists()) {
                JOptionPane.showMessageDialog(this, "Cannot find file", "Unable to find the file " + src.getName(), JOptionPane.ERROR_MESSAGE);
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
            loadedSketch.addLibraryToImportList(lib);
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

        Core thisCore = loadedSketch.getCore();

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

            menu.add(libsMenu);
        }
    }

    public void releasePort(String portName) {
        for(Plugin plugin : plugins) {
            try {
                plugin.releasePort(portName);
            } catch(Exception e) {
                Base.error(e);
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

    public static void selectAllEditorBoards() {
        for(Editor e : editorList) {
            e.reselectEditorBoard();
        }
    }

    public void reselectEditorBoard() {
        String eb = loadedSketch.getBoardName();

        if(eb != null) {
            loadedSketch.setBoard(eb);
            manualPane.setRoot(loadedSketch.getCore().getManual());
            manualPane.home();
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

        fc.setCurrentDirectory(Base.getSketchbookFolder());

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            // Convert the DefaultShellFolder into a File object via a string representation
            // of the path
            File f = new File(fc.getSelectedFile().getAbsolutePath());
            loadSketch(f);
        }
    }

    public void loadSketch(String f) {
        loadSketch(new File(f));
    }

    public void loadSketch(File f) {
        if(loadedSketch.isUntitled() && !isModified()) {
            closeAllTabs();
            loadedSketch = new Sketch(f);
            loadedSketch.attachToEditor(this);
            filesTreeRoot.setUserObject(loadedSketch.getFolder());
            treeRoot.setUserObject(loadedSketch);
            updateAll();
//            treeModel.nodeStructureChanged(treeRoot);
            openOrSelectFile(loadedSketch.getMainFile());
        } else {
            Base.createNewEditor(f.getPath());
        }

        fireEvent(UEvent.SKETCH_OPEN);
    }

    public static void updateLookAndFeel() {
        for(Editor e : editorList) {
            SwingUtilities.updateComponentTreeUI(e);
        }
    }

    public static void releasePorts(String n) {
        for(Editor e : editorList) {
            e.releasePort(n);
        }
    }

    public static void refreshAllEditors() {
        System.err.println("Refresh called!");
        for(Editor e : editorList) {
            e.refreshEditors();
        }
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
//        Dimension ss = getSize();
//        Point sl = getLocation();
//        Splash splash = new Splash(sl.x, sl.y, ss.width, ss.height);
//        splash.enableCloseOnClick();

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
                return Translate.t("ZIP Files");
            }
        });

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            String[] entries = getZipEntries(fc.getSelectedFile());
            if (fc.getSelectedFile() == null) {
                error("Open seems to have been cancelled. Aborting");
                return;
            }
            message("Analyzing " + fc.getSelectedFile().getName() + "...");

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
                            message("Found library " + possibleLibraryName + " at " + folder);
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

                LibCatObject loc = (LibCatObject)JOptionPane.showInputDialog(this, "Select location to store this library:", "Select Destination", JOptionPane.PLAIN_MESSAGE,
                                   null, catarr, null);

                if(loc != null) {
                    File installPath = Library.getCategoryLocation(loc.getKey());
                    message("Installing to " + installPath.getAbsolutePath());

                    for(String lib : foundLibs.keySet()) {
                        message("Installing " + lib + "...");
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

                    message("Updating library list...");
                    Base.gatherLibraries();
                    Editor.updateAllEditors();
                    message("Installation finished.");
                }
            } else {
                error("Unable to detect any valid libraries in the archive.");
                warning("You may need to manually install it or re-package it properly.");
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
                             "Enter filename:",
                             "Enter Filename",
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
                return Translate.t("ZIP Files");
            }
        });

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            File zipFile = fc.getSelectedFile();

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
            error("Sorry, there is already a compiler thread running for this sketch.");
            return;
        }

        if(Base.preferences.getBoolean("editor.autosave")) {
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
            error("Sorry, there is already a compiler thread running for this sketch.");
            return;
        }

        if(Base.preferences.getBoolean("editor.autosave")) {
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

            int rv = fc.showSaveDialog(this);

            if(rv == JFileChooser.APPROVE_OPTION) {
                File archiveFile = fc.getSelectedFile();

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
                error(e);
            }
        }
    }

    public boolean isFullScreen = false;
    public void toggleFullScreen() {
        if(!isFullScreen) {
            this.dispose();
            setUndecorated(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            this.pack();
            this.setVisible(true);
            isFullScreen = true;
            Base.preferences.setBoolean("editor.fullscreen", true);
            Base.preferences.saveDelay();
        } else {
            int width = Base.preferences.getInteger("editor.window.width");

            if(width < Base.preferences.getInteger("editor.window.width.min")) {
                width = Base.preferences.getInteger("editor.window.width.min");
            }

            int height = Base.preferences.getInteger("editor.window.height");

            if(height < Base.preferences.getInteger("editor.window.height.min")) {
                height = Base.preferences.getInteger("editor.window.height.min");
            }

            this.dispose();
            setUndecorated(false);
            setLocation(Base.preferences.getInteger("editor.window.x"), Base.preferences.getInteger("editor.window.y"));
            setSize(new Dimension(width, height));
            setExtendedState(JFrame.NORMAL);
            this.pack();
            this.setVisible(true);
            isFullScreen = false;
            Base.preferences.setBoolean("editor.fullscreen", false);
            Base.preferences.saveDelay();
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
}

