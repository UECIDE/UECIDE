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

package uecide.app;

import uecide.plugin.*;
import uecide.app.debug.*;
import uecide.app.editors.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.lang.reflect.*;
import javax.imageio.*;

import java.awt.datatransfer.*;

import uecide.app.debug.Compiler;

public class Editor extends JFrame {
    
    Box mainDecorationContainer;

    JSplitPane topBottomSplit;
    JSplitPane leftRightSplit;

    JTree sketchContentTree;

    Sketch loadedSketch;

    JMenuBar menuBar;

    JMenu fileMenu;
    JMenu editMenu;
    JMenu sketchMenu;
    JMenu hardwareMenu;
    JMenu toolsMenu;
    JMenu helpMenu;

    JToolBar toolbar;

    JPanel consolePanel;
    JPanel treePanel;
    JPanel editorPanel;
    JPanel statusBar;

    JTabbedPane editorTabs;

    JScrollPane treeScroll;
    DefaultMutableTreeNode treeRoot;
    DefaultMutableTreeNode treeSource;
    DefaultMutableTreeNode treeHeaders;
    DefaultMutableTreeNode treeLibraries;
    DefaultMutableTreeNode treeOutput;
    DefaultMutableTreeNode treeBinaries;
    DefaultTreeModel treeModel;

    JScrollPane consoleScroll;
    BufferedStyledDocument consoleDoc;
    JTextPane consoleTextPane;

    MutableAttributeSet stdStyle;
    MutableAttributeSet errStyle;
    MutableAttributeSet warnStyle;

    JProgressBar statusProgress;
    JLabel statusLabel;

    boolean compilerRunning = false;

    JButton runButton;
    JButton programButton;

    ArrayList<Plugin> plugins = new ArrayList<Plugin>();

    class DefaultRunHandler implements Runnable {
        public void run() {
            if (compilerRunning) return;
            compilerRunning = true;
            runButton.setEnabled(false);
            programButton.setEnabled(false);
            try {
                if(loadedSketch.build()) {
            //        reportSize();
                }
            } catch (Exception e) {
                error(e);
            }
            compilerRunning = false;
            runButton.setEnabled(true);
            programButton.setEnabled(true);
        }
    }

    class LibCompileRunHandler implements Runnable {
        Library library;
        public LibCompileRunHandler(Library lib) {
            library = lib;
        }
        public void run() {
            if (compilerRunning) return;
            runButton.setEnabled(false);
            programButton.setEnabled(false);
            compilerRunning = true;
            try {
                loadedSketch.precompileLibrary(library);
            } catch (Exception e) {
                error(e);
            }
            compilerRunning = false;
            runButton.setEnabled(true);
            programButton.setEnabled(true);
        }
    }

    public Editor(Sketch s) {
        super();
        loadedSketch = s;
        s.attachToEditor(this);

        for (Class<?> plugin : Base.plugins.values()) {
            try {
                Constructor<?> ctor = plugin.getConstructor(Editor.class);
                Plugin p = (Plugin)(ctor.newInstance(new Object[] { this }));
                plugins.add(p);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.setLayout(new BorderLayout());

        treePanel = new JPanel();
        editorPanel = new JPanel();
        consolePanel = new JPanel();
        statusBar = new JPanel();

        treePanel.setLayout(new BorderLayout());
        editorPanel.setLayout(new BorderLayout());
        consolePanel.setLayout(new BorderLayout());
        statusBar.setLayout(new BorderLayout());

        editorTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);


        editorTabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateMenus();
            }
        });

        editorPanel.add(editorTabs, BorderLayout.CENTER);

        int width = Base.preferences.getInteger("editor.window.width");
        if (width < Base.preferences.getInteger("editor.window.width.min")) {
            width = Base.preferences.getInteger("editor.window.width.min");
        }
        int height = Base.preferences.getInteger("editor.window.height");
        if (height < Base.preferences.getInteger("editor.window.height.min")) {
            height = Base.preferences.getInteger("editor.window.height.min");
        }

        leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, editorPanel);
        topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, leftRightSplit, consolePanel);

        leftRightSplit.setOneTouchExpandable(true);
        topBottomSplit.setOneTouchExpandable(true);

        leftRightSplit.setContinuousLayout(true);
        topBottomSplit.setContinuousLayout(true);

        leftRightSplit.setResizeWeight(0D);
        topBottomSplit.setResizeWeight(1D);

        int dividerSize = Base.preferences.getInteger("editor.divider.split", 250);
        topBottomSplit.setDividerLocation(height - dividerSize);

        dividerSize = Base.preferences.getInteger("editor.tree.split", 150);
        leftRightSplit.setDividerLocation(dividerSize);

        this.add(topBottomSplit, BorderLayout.CENTER);

        consoleScroll = new JScrollPane();
        int maxLineCount = Base.preferences.getInteger("console.length");

        consoleDoc = new BufferedStyledDocument(10000, maxLineCount);
        consoleTextPane = new JTextPane(consoleDoc);
        consoleTextPane.setEditable(false);

        MutableAttributeSet standard = new SimpleAttributeSet();
        StyleConstants.setAlignment(standard, StyleConstants.ALIGN_LEFT);
        consoleDoc.setParagraphAttributes(0, 0, standard, true);

        // build styles for different types of console output
        Color bgColor    = Base.theme.getColor("console.color");
        Color fgColorOut = Base.theme.getColor("console.output.color");
        Color fgColorErr = Base.theme.getColor("console.error.color");
        Color fgColorWarn = Base.theme.getColor("console.warning.color");
        Font font        = Base.preferences.getFont("console.font");

        stdStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stdStyle, fgColorOut);
        StyleConstants.setBackground(stdStyle, bgColor);
        StyleConstants.setFontSize(stdStyle, font.getSize());
        StyleConstants.setFontFamily(stdStyle, font.getFamily());
        StyleConstants.setBold(stdStyle, font.isBold());
        StyleConstants.setItalic(stdStyle, font.isItalic());

        errStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errStyle, fgColorErr);
        StyleConstants.setBackground(errStyle, bgColor);
        StyleConstants.setFontSize(errStyle, font.getSize());
        StyleConstants.setFontFamily(errStyle, font.getFamily());
        StyleConstants.setBold(errStyle, font.isBold());
        StyleConstants.setItalic(errStyle, font.isItalic());

        warnStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(warnStyle, fgColorWarn);
        StyleConstants.setBackground(warnStyle, bgColor);
        StyleConstants.setFontSize(warnStyle, font.getSize());
        StyleConstants.setFontFamily(warnStyle, font.getFamily());
        StyleConstants.setBold(warnStyle, font.isBold());
        StyleConstants.setItalic(warnStyle, font.isItalic());

        consoleTextPane.setBackground(bgColor);

        consoleScroll.setViewportView(consoleTextPane);

        consolePanel.add(consoleScroll);

        toolbar = new JToolBar();

        statusLabel = new JLabel("");
        statusProgress = new JProgressBar();

        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(statusProgress, BorderLayout.EAST);

        Font labelFont = statusLabel.getFont();
        statusLabel.setFont(new Font(labelFont.getName(), Font.PLAIN, 12));

        this.add(toolbar, BorderLayout.NORTH);
        this.add(statusBar, BorderLayout.SOUTH);

        treeScroll = new JScrollPane();
        treePanel.add(treeScroll, BorderLayout.CENTER);
        initTreeStructure();

        File themeFolder = Base.getContentFile("lib/theme");
        runButton = addToolbarButton(toolbar, "toolbar/run.png", "Compile", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearConsole();
                if (compilerRunning) {
                    error("Sorry, there is already a compiler thread running for this sketch.");
                    return;
                }
                DefaultRunHandler runHandler = new DefaultRunHandler();
                new Thread(runHandler, "Compiler").start();
            }
        });

        programButton = addToolbarButton(toolbar, "toolbar/burn.png", "Program");
        toolbar.addSeparator();

        addToolbarButton(toolbar, "toolbar/new.png", "New Sketch", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleNew();
            }
        });



        addToolbarButton(toolbar, "toolbar/open.png", "Open Sketch", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleOpenPrompt();
            }
        });

        addToolbarButton(toolbar, "toolbar/save.png", "Save Sketch", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAllTabs();
            }
        });

        toolbar.addSeparator();

        addPluginsToToolbar(toolbar, Plugin.TOOLBAR_EDITOR);

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


        this.pack();
        this.setVisible(true);

        setSize(width, height);
        setLocation(Base.preferences.getInteger("editor.window.x"), Base.preferences.getInteger("editor.window.y"));
        setProgress(0);
        updateAll();
        openOrSelectFile(loadedSketch.getMainFile());
    }

    class FileCellRenderer implements TreeCellRenderer {
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

                if (userObject instanceof File) {
                    File file = (File)userObject;
                    JLabel text = new JLabel(file.getName());
                    icon = Base.loadIconFromResource(FileType.getIcon(file));
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
                    if (icon != null) {
                        JLabel i = new JLabel(icon);
                        container.add(i, BorderLayout.WEST);
                    }
                    container.add(text, BorderLayout.CENTER);
                    return container;
                }

                if (userObject instanceof Library) {
                    Library lib = (Library)userObject;
                    int pct = lib.getCompiledPercent();

                    if (loadedSketch.libraryIsCompiled(lib) && (pct >= 100 || pct <= 0)) {
                        icon = Base.loadIconFromResource("files/library-good.png");
                    } else {
                        icon = Base.loadIconFromResource("files/library-bad.png");
                    }

                    container.setOpaque(false);
                    container.setBorder(noBorder);
                    JLabel i = new JLabel(icon);
                    container.add(i, BorderLayout.WEST);

                    if (pct > 0 && pct < 100) {
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
                        if (selected) {
                            text.setBackground(bg);
                            text.setForeground(fg);
                            text.setOpaque(true);
                        } else {
                            text.setOpaque(false);
                        }
                        container.add(text, BorderLayout.CENTER);
                    }
                    return container;
                }
                JLabel text = new JLabel(userObject.toString());
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
                return container;

            }
            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    }

    public void addFileTreeToNode(DefaultMutableTreeNode treenode, File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] buildFiles = dir.listFiles();
        for (File file : buildFiles) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(file.getName());
            if (file.isDirectory()) {
                addFileTreeToNode(node, file);
            } else {
                node.setUserObject(file);
            }
            treenode.add(node);
        };
    }


    public void initTreeStructure() {
        treeRoot = new DefaultMutableTreeNode(loadedSketch.getName());
        treeModel = new DefaultTreeModel(treeRoot);
        sketchContentTree = new JTree(treeModel);
        treeSource = new DefaultMutableTreeNode("Source");
        treeHeaders = new DefaultMutableTreeNode("Headers");
        treeLibraries = new DefaultMutableTreeNode("Libraries");
        treeBinaries = new DefaultMutableTreeNode("Binaries");
        treeOutput = new DefaultMutableTreeNode("Output");
        TreeCellRenderer renderer = new FileCellRenderer();
        sketchContentTree.setCellRenderer(renderer);
        treeRoot.add(treeSource);
        treeRoot.add(treeHeaders);
        treeRoot.add(treeLibraries);
        treeRoot.add(treeBinaries);
        treeRoot.add(treeOutput);

        sketchContentTree.expandPath(new TreePath(treeRoot.getPath()));
        sketchContentTree.expandPath(new TreePath(treeSource.getPath()));

        treeScroll.setViewportView(sketchContentTree);
        Font font        = Base.preferences.getFont("tree.font");
        sketchContentTree.setFont(font);
        sketchContentTree.addMouseListener(new TreeMouseListener());
    }

    public void updateSourceTree() {
        boolean treeSourceOpen = sketchContentTree.isExpanded(new TreePath(treeSource.getPath()));
        treeSource.removeAllChildren();
        DefaultMutableTreeNode node;
        for (File f : loadedSketch.sketchFiles) {
            int type = FileType.getType(f);
            switch (type) {
                case FileType.CSOURCE:
                case FileType.CPPSOURCE:
                case FileType.ASMSOURCE:
                case FileType.SKETCH:
                    node = new DefaultMutableTreeNode(f.getName());
                    node.setUserObject(f);
                    treeSource.add(node);
                    break;
            }
        }
        treeModel.nodeStructureChanged(treeSource);
        if (treeSourceOpen) sketchContentTree.expandPath(new TreePath(treeSource.getPath()));
    }

    public void updateHeadersTree() {
        boolean treeHeadersOpen = sketchContentTree.isExpanded(new TreePath(treeHeaders.getPath()));
        treeHeaders.removeAllChildren();
        DefaultMutableTreeNode node;
        for (File f : loadedSketch.sketchFiles) {
            int type = FileType.getType(f);
            switch (type) {
                case FileType.HEADER:
                    node = new DefaultMutableTreeNode(f.getName());
                    node.setUserObject(f);
                    treeHeaders.add(node);
                    break;
            }
        }
        treeModel.nodeStructureChanged(treeHeaders);
        if (treeHeadersOpen) sketchContentTree.expandPath(new TreePath(treeHeaders.getPath()));
    }

    public void updateLibrariesTree() {
        boolean treeLibrariesOpen = sketchContentTree.isExpanded(new TreePath(treeLibraries.getPath()));
        treeLibraries.removeAllChildren();
        HashMap<String, Library>libList = loadedSketch.getLibraries();
        DefaultMutableTreeNode node;
        if (libList != null) {
            for (String libname : libList.keySet()) {
                node = new DefaultMutableTreeNode(libname);
                node.setUserObject(libList.get(libname));
                treeLibraries.add(node);
            }
        }
        treeModel.nodeStructureChanged(treeLibraries);
        if (treeLibrariesOpen) sketchContentTree.expandPath(new TreePath(treeLibraries.getPath()));
    }

    public void updateBinariesTree() {
        boolean treeBinariesOpen = sketchContentTree.isExpanded(new TreePath(treeBinaries.getPath()));
        treeBinaries.removeAllChildren();
        File bins = loadedSketch.getBinariesFolder();
        DefaultMutableTreeNode node;
        if (bins.exists() && bins.isDirectory()) {
            File[] files = bins.listFiles();
            for (File binFile : files) {
                if (binFile.getName().startsWith(".")) {
                    continue;
                }
                node = new DefaultMutableTreeNode(binFile.getName());
                node.setUserObject(binFile);
                treeBinaries.add(node);
            }
        }
        treeModel.nodeStructureChanged(treeBinaries);
        if (treeBinariesOpen) sketchContentTree.expandPath(new TreePath(treeBinaries.getPath()));
    }

    public void updateOutputTree() {
        boolean treeOutputOpen = sketchContentTree.isExpanded(new TreePath(treeOutput.getPath()));
        treeOutput.removeAllChildren();
        addFileTreeToNode(treeOutput, loadedSketch.getBuildFolder());
        treeModel.nodeStructureChanged(treeOutput);
        if (treeOutputOpen) sketchContentTree.expandPath(new TreePath(treeOutput.getPath()));
    }

    public void updateTree() {
        boolean treeRootOpen = sketchContentTree.isExpanded(new TreePath(treeRoot.getPath()));

        updateSourceTree();
        updateHeadersTree();
        updateLibrariesTree();
        updateBinariesTree();
        updateOutputTree();

        if (treeRootOpen) sketchContentTree.expandPath(new TreePath(treeRoot.getPath()));
    }

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

            int selRow = sketchContentTree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = sketchContentTree.getPathForLocation(e.getX(), e.getY());
            sketchContentTree.setSelectionPath(selPath);
            if (e.getButton() == 3) {
                DefaultMutableTreeNode o = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                DefaultMutableTreeNode p = (DefaultMutableTreeNode)o.getParent();
                if (o.getUserObject().getClass().equals(String.class)) {
                    String s = (String)o.getUserObject();
                    if (s.equals("Source")) {
                        JPopupMenu menu = new JPopupMenu();
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
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if (s.equals("Headers")) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem item = new JMenuItem("Create header file");
                        item.setActionCommand("h");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem("Import header file");
                        item.setActionCommand("header");
                        item.addActionListener(importFileAction);
                        menu.add(item);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if (s.equals("Libraries")) {
                        JPopupMenu menu = new JPopupMenu();
                        populateLibrariesMenu(menu);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if (s.equals("Binaries")) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem item = new JMenuItem("Add binary file");
                        item.setActionCommand("binary");
                        item.addActionListener(importFileAction);
                        menu.add(item);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if (s.equals("Output")) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem item = new JMenuItem("Purge output files");
                        menu.add(item);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } 
                } else if (o.getUserObject().getClass().equals(File.class)) {
                    File thisFile = (File)o.getUserObject();
                    JPopupMenu menu = new JPopupMenu();

                    JMenuItem renameItem = new JMenuItem("Rename file");
                    renameItem.setActionCommand("rename");
                    menu.add(renameItem);

                    JMenuItem deleteItem = new JMenuItem("Delete file");
                    deleteItem.setActionCommand("delete");
                    menu.add(deleteItem);

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


                    if (p.getUserObject().getClass().equals(String.class)) {
                        String ptext = (String)p.getUserObject();
                        if (ptext.equals("Binaries")) {
                            JMenuItem insertRef = new JMenuItem("Insert reference");
                            insertRef.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    String filename = e.getActionCommand();
                                    filename = filename.replaceAll("\\.","_");
                                    int at = getActiveTab();
                                    if (at > -1) {
                                        EditorBase eb = getTab(at);
                                        eb.insertAtCursor("extern char " + filename + "[] asm(\"_binary_objects_" + filename + "_start\");\n");
                                    }
                                }
                            });
                            insertRef.setActionCommand(thisFile.getName());
                            menu.add(insertRef);
                        }
                    }

                    menu.show(sketchContentTree, e.getX(), e.getY());
                } else if (o.getUserObject().getClass().equals(Library.class)) {
                    final Library lib = (Library)(o.getUserObject());
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem item = new JMenuItem("Delete cached archive");
                    item.setEnabled(loadedSketch.libraryIsCompiled(lib));
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            loadedSketch.purgeLibrary(lib);
                        }
                    });
                    menu.add(item);
                    item = new JMenuItem("Recompile now");
                    item.setEnabled(!compilerRunning);
                    item.setActionCommand("yes");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            loadedSketch.purgeLibrary(lib);
                            clearConsole();
                            if (compilerRunning) {
                                error("Sorry, there is already a compiler thread running for this sketch.");
                                return;
                            }
                            LibCompileRunHandler runHandler = new LibCompileRunHandler(lib);
                            new Thread(runHandler, "Compiler").start();
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
                    if (o.getUserObject().getClass().equals(File.class)) {
                        File sf = (File)o.getUserObject();
                        openOrSelectFile(sf);
                    }
                }
            }
        }
    }

    public Board getBoard() {
        if (loadedSketch == null) {
            return null;
        }
        return loadedSketch.getBoard();
    }

    public Core getCore() {
        if (loadedSketch == null) {
            return null;
        }
        return loadedSketch.getCore();
    }

    public Compiler getCompiler() {
        if (loadedSketch == null) {
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

    public void message(String msg) {
        if (!msg.endsWith("\n")) { msg += "\n"; }
        consoleDoc.appendString(msg, stdStyle);
        consoleDoc.insertAll();
        consoleTextPane.setCaretPosition(consoleDoc.getLength());
    }

    public void warning(String msg) {
        if (!msg.endsWith("\n")) { msg += "\n"; }
        consoleDoc.appendString(msg, warnStyle);
        consoleDoc.insertAll();
        consoleTextPane.setCaretPosition(consoleDoc.getLength());
    }

    public void error(String msg) {
        if (!msg.endsWith("\n")) { msg += "\n"; }
        consoleDoc.appendString(msg, errStyle);
        consoleDoc.insertAll();
        consoleTextPane.setCaretPosition(consoleDoc.getLength());
    }

    public void error(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        error(sw.toString());
    }


    public void clearConsole() {
        try {
            consoleDoc.remove(0, consoleDoc.getLength());
        } catch (BadLocationException e) {
        }
    }

    public void setProgress(int x) {
        statusProgress.setValue(x);
    }

    public void setStatus(String s) {
        statusLabel.setText(s);
    }

    public void openOrSelectFile(File sf) {
        int existing = getTabByFile(sf);
        if (existing == -1) {
            openNewTab(sf);
        } else {
            selectTab(existing);
        }
    }

    public JButton addToolbarButton(JToolBar tb, String path, String tooltip) {
        return addToolbarButton(tb, path, tooltip, null);
    }

    public JButton addToolbarButton(JToolBar tb, String path, String tooltip, ActionListener al) {
        ImageIcon buttonIcon = Base.loadIconFromResource(path);
        JButton button = new JButton(buttonIcon);
        button.setToolTipText(tooltip);
        if (al != null) {
            button.addActionListener(al);
        }
        tb.add(button);
        return button;
    }

    public void closeTab(int tab) {
        if (tab == -1) return;
        EditorBase eb = (EditorBase)editorTabs.getComponentAt(tab);
        if (eb.isModified()) {
            int option = threeOptionBox(
                JOptionPane.WARNING_MESSAGE,
                Translate.t("Unsaved File"),
                Translate.w("This file has been modified.  Do you want to save your work before you close this tab?", 40, "\n"), 
                "Save", 
                "Don't Save", 
                "Cancel"
            );

            if (option == 2) return;
            if (option == 0) eb.save();
        }
        editorTabs.remove(tab);
    }

    public class TabLabel extends JPanel {
        ImageIcon fileIcon = null;
        JLabel nameLabel;
        JButton button;
        File sketchFile;
        String name;
        long expectedFileTime;
        boolean modified = false;
        
        public TabLabel(File sf) {
            sketchFile = sf;
            name = sketchFile.getName();
            this.setLayout(new BorderLayout());
            nameLabel = new JLabel(name);
            fileIcon = Base.loadIconFromResource(FileType.getIcon(sketchFile));
            if (fileIcon != null) {
                nameLabel.setIcon(fileIcon);
            }
            button = new JButton(Base.loadIconFromResource("tabs/close.png"));
            button.setBorder(new EmptyBorder(0, 4, 0, 4));
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int tab = getTabByFile(sketchFile);
                    closeTab(tab);
                }
            });
            nameLabel.setOpaque(false);
            button.setOpaque(false);
            this.setOpaque(false);
            this.add(nameLabel, BorderLayout.CENTER);
            this.add(button, BorderLayout.EAST);
            expectedFileTime = sf.lastModified();
            update();
        }

        public void update() {
            Font labelFont = nameLabel.getFont();
            if (modified) {
                nameLabel.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
                nameLabel.setText(sketchFile.getName() + " * ");
            } else {
                nameLabel.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
                nameLabel.setText(sketchFile.getName());
            }
        }
    
        public boolean needsReload() {
            return (sketchFile.lastModified() > expectedFileTime);
        }

        public void setReloaded() {
            expectedFileTime = sketchFile.lastModified();
        }

        public void setModified(boolean m) {
            if (modified != m) {
                modified = m;
                update();
            }
        }

        public void setFile(File f) {
            System.err.println("Changing tab file from " + sketchFile.getAbsolutePath() + " to " + f.getAbsolutePath());
            sketchFile = f;
            update();
        }

        public File getFile() {
            return sketchFile;
        }
    }

    public void openNewTab(File sf) {
        if (sf.exists() == false) {
            error("Sorry, I can't find " + sf.getName() + ".");
            return;
        }
        try {
            String className = FileType.getEditor(sf.getName());
            if (className == null) {
                error("File type for " + sf.getName() + " unknown");
                return;
            }
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            Class<?> edClass = Class.forName(className);
            Constructor<?> edConst = edClass.getConstructor(Sketch.class, File.class, Editor.class);

            EditorBase ed = (EditorBase)edConst.newInstance(loadedSketch, sf, this);
        
            editorTabs.addTab(sf.getName(), (JPanel) ed);
            int tabno = editorTabs.getTabCount() - 1;

            TabLabel lab = new TabLabel(sf);
            editorTabs.setTabComponentAt(tabno, lab);
            
            selectTab(tabno);
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            ed.requestFocus();
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public EditorBase getTab(int i) {
        return (EditorBase)editorTabs.getComponentAt(i);
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
        return editorTabs.getTabCount();
    }

    public void setTabModified(int t, boolean m) {
        TabLabel tl = getTabLabel(t);
        if (tl != null) {
            tl.setModified(m);
        }
    }

    public int getTabByFile(File f) {
        for (int i = 0; i < editorTabs.getTabCount(); i++) {
            TabLabel l = (TabLabel)editorTabs.getTabComponentAt(i);
            if (l.getFile().equals(f)) {
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
        if (tl == null) {
            return;
        }
        if (tl.needsReload()) {
            try {
                EditorBase eb = (EditorBase)editorTabs.getComponentAt(tab);
                eb.reloadFile();
                tl.setReloaded();
            } catch (Exception e) {
                error(e);
            }
        }
        editorTabs.setSelectedIndex(tab);
    }

    public boolean isModified() {
        for (int i = 0; i < editorTabs.getTabCount(); i++) {
            EditorBase eb = (EditorBase)editorTabs.getComponentAt(i);
            if (eb.isModified()) {
                return true;
            }
        }
        return false;
    }

    public void saveAllTabs() {
        if (loadedSketch.isUntitled()) {
            saveAs();
            return; 
        }
        for (int i = 0; i < editorTabs.getTabCount(); i++) {
            EditorBase eb = (EditorBase)editorTabs.getComponentAt(i);
            if (eb.isModified()) {
                eb.save();
            }
        }
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
        if (isModified()) {
            int option = threeOptionBox(
                JOptionPane.WARNING_MESSAGE,
                Translate.t("Unsaved Files"),
                Translate.w("You have unsaved files.  Do you want to save your work before you close this window?", 40, "\n"), 
                "Save", 
                "Don't Save", 
                "Cancel"
            );
            if (option == 0) {
                saveAllTabs();
            }
            if (option == 2) {
                return false;
            }
        }
        Base.unregisterEditor(this);
        this.dispose();
        return true;
    }

    public void addSketchesFromFolder(JMenu menu, File folder) {
        if (folder == null) return;
        if (menu == null) return;
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        Arrays.sort(files);
        for (File file : files) {
            if (file.isDirectory()) {
                File testFile1 = new File(file, file.getName() + ".ino");
                File testFile2 = new File(file, file.getName() + ".pde");
                if (testFile1.exists() || testFile2.exists()) {
                    JMenuItem item = new JMenuItem(file.getName());
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String path = e.getActionCommand();
                            if (new File(path).exists()) {
                                Base.createNewEditor(path);
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
                    if (submenu.getItemCount() > 0) {
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
                Base.handleOpenPrompt();
            }
        });
        fileMenu.add(item);

        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_TOP);

        submenu = new JMenu(Translate.t("Recent Sketches"));
        fileMenu.add(submenu);

        for (File m : Base.MRUList) {
            item = new JMenuItem(m.getName());
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String path = e.getActionCommand();
                    if (new File(path).exists()) {
                        Base.createNewEditor(path);
                    } else {
                        error("Unable to find file " + path);
                    }
                }
            });
            item.setActionCommand(m.getAbsolutePath());
            submenu.add(item);
        }


        submenu = new JMenu(Translate.t("Examples"));
        if (loadedSketch.getCore() != null) {
            addSketchesFromFolder(submenu, loadedSketch.getCore().getExamplesFolder());
            addSketchesFromFolder(submenu, loadedSketch.getBoard().getExamplesFolder());
            addSketchesFromFolder(submenu, loadedSketch.getCompiler().getExamplesFolder());

            JMenu boardLibsMenu = new JMenu(loadedSketch.getBoard().getName());
            HashMap<String, Library> boardLibraries = Base.getLibraryCollection("board:" + loadedSketch.getBoard().getName(), loadedSketch.getCore().getName());
            for (String libName : boardLibraries.keySet()) {
                JMenu libMenu = new JMenu(libName);
                addSketchesFromFolder(libMenu, boardLibraries.get(libName).getExamplesFolder());
                if (libMenu.getItemCount() > 0) {
                    boardLibsMenu.add(libMenu);
                }
            }

            JMenu coreLibsMenu = new JMenu(loadedSketch.getCore().getName());
            HashMap<String, Library> coreLibraries = Base.getLibraryCollection("core:" + loadedSketch.getCore().getName(), loadedSketch.getCore().getName());
            for (String libName : coreLibraries.keySet()) {
                JMenu libMenu = new JMenu(libName);
                addSketchesFromFolder(libMenu, coreLibraries.get(libName).getExamplesFolder());
                if (libMenu.getItemCount() > 0) {
                    coreLibsMenu.add(libMenu);
                }
            }

            JMenu compilerLibsMenu = new JMenu(loadedSketch.getCompiler().getName());
            HashMap<String, Library> compilerLibraries = Base.getLibraryCollection("compiler:" + loadedSketch.getCompiler().getName(), loadedSketch.getCore().getName());
            for (String libName : compilerLibraries.keySet()) {
                JMenu libMenu = new JMenu(libName);
                addSketchesFromFolder(libMenu, compilerLibraries.get(libName).getExamplesFolder());
                if (libMenu.getItemCount() > 0) {
                    compilerLibsMenu.add(libMenu);
                }
            }

            if (boardLibsMenu.getItemCount() > 0) {
                submenu.add(boardLibsMenu);
            }

            if (coreLibsMenu.getItemCount() > 0) {
                submenu.add(coreLibsMenu);
            }

            if (compilerLibsMenu.getItemCount() > 0) {
                submenu.add(compilerLibsMenu);
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

        fileMenu.addSeparator();
        item = new JMenuItem(Translate.t("Preferences"));
        item.setAccelerator(KeyStroke.getKeyStroke("ctrl-,"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handlePrefs();
            }
        });
        fileMenu.add(item);

        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_BOTTOM);

        item = new JMenuItem(Translate.t("Quit"));
        item.setAccelerator(KeyStroke.getKeyStroke("alt Q"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.closeAllEditors();
            }
        });
        fileMenu.add(item);

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
        submenu.add(item);
        submenu.addSeparator();
        populateLibrariesMenu(submenu);
        sketchMenu.add(submenu);
        

        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_TOP);
        sketchMenu.addSeparator();
        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_MID);
        sketchMenu.addSeparator();
        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_BOTTOM);

        submenu = new JMenu("Boards");
        populateBoardsMenu(submenu);
        hardwareMenu.add(submenu);

        submenu = new JMenu("Cores");
        populateCoresMenu(submenu);
        hardwareMenu.add(submenu);

        submenu = new JMenu("Compilers");
        populateCompilersMenu(submenu);
        hardwareMenu.add(submenu);

        submenu = new JMenu("Options");
        populateOptionsMenu(submenu);
        submenu.setEnabled(submenu.getItemCount() > 0);
        hardwareMenu.add(submenu);

        submenu = new JMenu("Serial Port");
        populateSerialMenu(submenu);
        submenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Serial.updatePortList();
            }
        });
        hardwareMenu.add(submenu);

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
        toolsMenu.addSeparator();
        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_BOTTOM);

        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_TOP);
        helpMenu.addSeparator();
        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_MID);
        helpMenu.addSeparator();
        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_BOTTOM);

    }

    public void populateOptionsMenu(JMenu menu) {
        HashMap<String, String> opts = loadedSketch.getOptionGroups();
        for (String opt : opts.keySet()) {
            JMenu submenu = new JMenu(opts.get(opt));
            HashMap<String, String>optvals = loadedSketch.getOptionNames(opt);
            ButtonGroup thisGroup = new ButtonGroup();
            for (String key : optvals.keySet()) {
                JMenuItem item = new JRadioButtonMenuItem(optvals.get(key));
                thisGroup.add(item);
                item.setActionCommand(opt + "=" + key);
                item.setSelected(loadedSketch.getOption(opt).equals(key));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String data = e.getActionCommand();
                        int idx = data.indexOf("=");
                        String key = data.substring(0, idx);
                        String value = data.substring(idx+1);
                        loadedSketch.setOption(key, value);
                    }
                });
                        
                submenu.add(item);
            }
            menu.add(submenu);
        }
    }

    public void populateSerialMenu(JMenu menu) {
        ButtonGroup portGroup = new ButtonGroup();
        ArrayList<String> ports = Serial.getPortList();
        for (String port : ports) {
            JMenuItem item = new JRadioButtonMenuItem(port);
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

    public String[] getBoardGroups() {
        ArrayList<String> out = new ArrayList<String>();
        for (Board board : Base.boards.values()) {
            String group = board.get("group");
            if (out.indexOf(group) == -1) {
                out.add(group);
            }
        }

        String[] groupList = out.toArray(new String[0]);
        Arrays.sort(groupList);
        return groupList;
    }

    ButtonGroup boardMenuButtonGroup;

    public void populateProgrammersMenu(JMenu menu) {
        HashMap<String, String> programmers = loadedSketch.getProgrammerList();
        ButtonGroup programmerGroup = new ButtonGroup();

        for (String pn : programmers.keySet()) {
            JMenuItem item = new JRadioButtonMenuItem(programmers.get(pn));
            programmerGroup.add(item);
            item.setSelected(loadedSketch.getProgrammer().equals(pn));
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

        for (String group : groups) {
            JMenu groupmenu = new JMenu(group);
            fillGroupMenu(groupmenu, group);
            if (groupmenu.getItemCount() > 0) {
                menu.add(groupmenu);
            }
        }
    }


    public void fillGroupMenu(JMenu menu, String group) {
        ArrayList<Board> boards = new ArrayList<Board>();
        for (Board board : Base.boards.values()) {
            if (board.get("group").equals(group)) {
                boards.add(board);
            }
        }

        Board[] boardList = boards.toArray(new Board[0]);
        Arrays.sort(boardList);

        for (Board board : boardList) {
            JMenuItem item = new JRadioButtonMenuItem(board.getDescription());
            boardMenuButtonGroup.add(item);
            if (loadedSketch.getBoard().equals(board)) {
                item.setSelected(true);
            }
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setBoard(e.getActionCommand());
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

        for (Core core : Base.cores.values()) {
            if (core.worksWith(board)) {
                coreList.add(core);
            }
        }

        Core[] cores = coreList.toArray(new Core[0]);
        Arrays.sort(cores);
        for (Core core : cores) {
            JMenuItem item = new JRadioButtonMenuItem(core.getName());
            coreGroup.add(item);
            item.setSelected(loadedSketch.getCore().equals(core));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadedSketch.setCore(e.getActionCommand());
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

        for (Compiler compiler : Base.compilers.values()) {
            if (compiler.worksWith(core)) {
                compilerList.add(compiler);
            }
        }

        Compiler[] compilers = compilerList.toArray(new Compiler[0]);
        Arrays.sort(compilers);
        for (Compiler compiler : compilers) {
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
        if (tab > -1) {
            EditorBase eb = getTab(tab);
            eb.populateMenu(menu, filterFlags);
        }
        addPluginsToMenu(menu, filterFlags);
    }


    public void addPluginsToMenu(JMenu menu, int filterFlags) {
        for (Plugin plugin : plugins) {
            plugin.populateMenu(menu, filterFlags);
        }
    }

    public void addPluginsToToolbar(JToolBar tb, int filterFlags) {
        for (final Plugin plugin : plugins) {
            try {
                plugin.addToolbarButtons(tb, filterFlags);
            } catch (Exception e) {
                error(e);
            }
        }
    }

    public void updateStatus() {
        StringBuilder statusInfo = new StringBuilder();

        statusInfo.append(loadedSketch.getBoard().getDescription());
        statusInfo.append(" on ");
        statusInfo.append(loadedSketch.getSerialPort());


        setStatus(statusInfo.toString());
    }

    public void updateAll() {
        updateMenus();
        updateTree();
        updateStatus();
    }

    public String getSerialPort() {
        return loadedSketch.getSerialPort();
    }

    public void message(String s, int c) {
        if (c == 2) {
            error(s);
        } else {
            message(s);
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

        if (rv == JFileChooser.APPROVE_OPTION) {
            File newFile = fc.getSelectedFile();
            if (newFile.exists()) {
                int n = twoOptionBox(
                    JOptionPane.WARNING_MESSAGE,
                    "Overwrite File?",
                    Translate.w("Do you really want to overwrite the file %1?", 40, "\n", newFile.getName()),
                    "Yes", "No");
                if (n != 0) {
                    return;
                }
                newFile.delete();
            }
            loadedSketch.saveAs(newFile);
        }
    }

    public boolean validName(String name) {
        final String validCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-";

        if (name == null) return false;

        for (int i = 0; i < name.length(); i++) {
            if (validCharacters.indexOf(name.charAt(i)) == -1) {
                return false;
            }
        }
        return true;
    }

    public void createNewSketchFile(String extension) {
        System.err.println("Create new " + extension + " file");
        String name = (String)JOptionPane.showInputDialog(
                    this,
                    "Enter filename",
                    "Create new ." + extension + " file",
                    JOptionPane.PLAIN_MESSAGE
                    );
        while (!validName(name)) {
            if (name == null) {
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

    public void importFile(String type) {
        JFileChooser fc = new JFileChooser();
        javax.swing.filechooser.FileFilter filter;
        if (type == "source") {
            filter = new SourceFileFilter();
            fc.setFileFilter(filter);
        } else if (type == "header") {
            filter = new HeaderFileFilter();
            fc.setFileFilter(filter);
        }
        int r = fc.showOpenDialog(this);

        if (r == JFileChooser.APPROVE_OPTION) {
            File src = fc.getSelectedFile();
            if (!src.exists()) {
                JOptionPane.showMessageDialog(this, "Cannot find file", "Unable to find the file " + src.getName(), JOptionPane.ERROR_MESSAGE);
                return;
            }
            File dest = null;
            if (type == "source" || type == "header") {
                dest = new File(loadedSketch.getFolder(), src.getName());
            } else if (type == "binary") {
                dest = new File(loadedSketch.getBinariesFolder(), src.getName());
            } else {
                System.err.println("Urk, I don't know what I'm doing!");
                return;
            }
            File dp = dest.getParentFile();
            if (!dp.exists()) {
                dp.mkdirs();
            }
            Base.copyFile(src, dest);
            if (type == "source" || type == "header") {
                loadedSketch.loadFile(dest);
            }
            updateTree();
        }
    }

    ActionListener insertIncludeAction = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String lib = e.getActionCommand();
            openOrSelectFile(loadedSketch.getMainFile());
            int tab = getTabByFile(loadedSketch.getMainFile());
            if (tab == -1) { 
                return;
            }
            EditorBase eb = getTab(tab);
            eb.insertAtStart("#include <" + lib + ".h>\n");
            loadedSketch.addLibraryToImportList(lib);
            updateLibrariesTree();
        }
    };
            

    public void populateLibrariesMenu(JComponent menu) {
        JMenuItem item;

        JMenu compilerLibsMenu = new JMenu(loadedSketch.getCompiler().getName());
        HashMap<String, Library> compilerLibraries = Base.getLibraryCollection("compiler:" + loadedSketch.getCompiler().getName(), loadedSketch.getCore().getName());
        for (String libName : compilerLibraries.keySet()) {
            item = new JMenuItem(libName);
            item.addActionListener(insertIncludeAction);
            item.setActionCommand(libName);
            compilerLibsMenu.add(item);
        }
        if (compilerLibsMenu.getItemCount() > 0) {
            menu.add(compilerLibsMenu);
        }

        JMenu coreLibsMenu = new JMenu(loadedSketch.getCore().getName());
        HashMap<String, Library> coreLibraries = Base.getLibraryCollection("core:" + loadedSketch.getCore().getName(), loadedSketch.getCore().getName());
        for (String libName : coreLibraries.keySet()) {
            item = new JMenuItem(libName);
            item.addActionListener(insertIncludeAction);
            item.setActionCommand(libName);
            coreLibsMenu.add(item);
        }
        if (coreLibsMenu.getItemCount() > 0) {
            menu.add(coreLibsMenu);
        }

        JMenu boardLibsMenu = new JMenu(loadedSketch.getBoard().getName());
        HashMap<String, Library> boardLibraries = Base.getLibraryCollection("board:" + loadedSketch.getBoard().getName(), loadedSketch.getCore().getName());
        for (String libName : boardLibraries.keySet()) {
            item = new JMenuItem(libName);
            item.addActionListener(insertIncludeAction);
            item.setActionCommand(libName);
            boardLibsMenu.add(item);
        }
        if (boardLibsMenu.getItemCount() > 0) {
            menu.add(boardLibsMenu);
        }

        for (String key : Base.libraryCategoryNames.keySet()) {
            JMenu catLibsMenu = new JMenu(Base.libraryCategoryNames.get(key));
            HashMap<String, Library> catLib = Base.getLibraryCollection("cat:" + key, getCore().getName());
            for (Library lib : catLib.values()) {
                item = new JMenuItem(lib.getName());
                item.addActionListener(insertIncludeAction);
                item.setActionCommand(lib.getName());
                catLibsMenu.add(item);
            }
            if (catLibsMenu.getItemCount() > 0) {
                menu.add(catLibsMenu);
            }

        }
    }
}
