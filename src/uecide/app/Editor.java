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

import java.util.Timer;
import uecide.app.Compiler;

import java.beans.*;

import java.util.jar.*;
import java.util.zip.*;



public class Editor extends JFrame {
    
    Box mainDecorationContainer;

    JSplitPane topBottomSplit;
    JSplitPane leftRightSplit;

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

    JToolBar toolbar;

    JPanel consolePanel;
    JPanel treePanel;
    JPanel editorPanel;
    JPanel statusBar;
    JPanel projectPanel;
    JPanel filesPanel;

    JTabbedPane editorTabs;
    JTabbedPane projectTabs;

    JScrollPane treeScroll;
    JScrollPane filesTreeScroll;

    DefaultMutableTreeNode treeRoot;
    DefaultMutableTreeNode treeSource;
    DefaultMutableTreeNode treeHeaders;
    DefaultMutableTreeNode treeLibraries;
    DefaultMutableTreeNode treeOutput;
    DefaultMutableTreeNode treeBinaries;
    DefaultTreeModel treeModel;

    DefaultMutableTreeNode filesTreeRoot;
    DefaultTreeModel filesTreeModel;

    JScrollPane consoleScroll;
    BufferedStyledDocument consoleDoc;
    JTextPane consoleTextPane;

    MutableAttributeSet stdStyle;
    MutableAttributeSet errStyle;
    MutableAttributeSet warnStyle;

    JProgressBar statusProgress;
    JLabel statusLabel;

    JButton abortButton;
    JButton runButton;
    JButton programButton;

    public static ArrayList<Editor>editorList = new ArrayList<Editor>();

    ArrayList<Plugin> plugins = new ArrayList<Plugin>();

    Thread compilationThread = null;

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
                    if (upload) {
                        loadedSketch.upload();
                    }
                }
            } catch (Exception e) {
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
            } catch (Exception e) {
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

        leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectPanel, editorPanel);
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

        final Editor me = this;

        leftRightSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    Dimension size = me.getSize();
                    int pos = (Integer)(e.getNewValue());
                    Base.preferences.setInteger("editor.tree.split", pos);
                    Base.preferences.saveDelay();
                }
            }
        );

        topBottomSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    Dimension size = me.getSize();
                    int pos = size.height - (Integer)(e.getNewValue());
                    Base.preferences.setInteger("editor.divider.split", pos);
                    Base.preferences.saveDelay();
                }
            }
        );

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

        toolbar.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                Window window = SwingUtilities.getWindowAncestor(toolbar);
                if (window == Editor.this) {
                    if (e.getChangeFlags() == HierarchyEvent.PARENT_CHANGED) {

                        JPanel pan = (JPanel)e.getChangedParent();

                        BorderLayout layout = (BorderLayout)pan.getLayout();

                        if (toolbar == layout.getLayoutComponent(BorderLayout.NORTH)) {
                            Base.preferences.set("editor.toolbar.position", "n");
                            Base.preferences.saveDelay();
                        }

                        if (toolbar == layout.getLayoutComponent(BorderLayout.SOUTH)) {
                            Base.preferences.set("editor.toolbar.position", "s");
                            Base.preferences.saveDelay();
                        }

                        if (toolbar == layout.getLayoutComponent(BorderLayout.EAST)) {
                            Base.preferences.set("editor.toolbar.position", "e");
                            Base.preferences.saveDelay();
                        }

                        if (toolbar == layout.getLayoutComponent(BorderLayout.WEST)) {
                            Base.preferences.set("editor.toolbar.position", "w");
                            Base.preferences.saveDelay();
                        }
                    }
                } else {
                    if (e.getChangeFlags() == HierarchyEvent.PARENT_CHANGED) {
                        Base.preferences.set("editor.toolbar.position", "f");
                        Base.preferences.saveDelay();
                    }
                }
            }
        });

        statusLabel = new JLabel("");
        statusProgress = new JProgressBar();

        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(statusProgress, BorderLayout.EAST);

        Font labelFont = statusLabel.getFont();
        statusLabel.setFont(new Font(labelFont.getName(), Font.PLAIN, 12));

        String tbPos = Base.preferences.get("editor.toolbar.position");
        if ((tbPos == null) || tbPos.equals("f") || tbPos.equals("n")) {
            this.add(toolbar, BorderLayout.NORTH);
            toolbar.setOrientation(JToolBar.HORIZONTAL);
        } else if (tbPos.equals("s")) {
            this.add(toolbar, BorderLayout.SOUTH);
            toolbar.setOrientation(JToolBar.HORIZONTAL);
        } else if (tbPos.equals("e")) {
            this.add(toolbar, BorderLayout.EAST);
            toolbar.setOrientation(JToolBar.VERTICAL);
        } else if (tbPos.equals("w")) {
            this.add(toolbar, BorderLayout.WEST);
            toolbar.setOrientation(JToolBar.VERTICAL);
        } else {
            this.add(toolbar, BorderLayout.NORTH);
            toolbar.setOrientation(JToolBar.HORIZONTAL);
        }

        this.add(statusBar, BorderLayout.SOUTH);

        treeScroll = new JScrollPane();
        treePanel.add(treeScroll, BorderLayout.CENTER);
        filesTreeScroll = new JScrollPane();
        filesPanel.add(filesTreeScroll, BorderLayout.CENTER);
        initTreeStructure();

        projectPanel.add(projectTabs, BorderLayout.CENTER);
        projectTabs.add(treePanel, "Project");
        projectTabs.add(filesPanel, "Files");

        File themeFolder = Base.getContentFile("lib/theme");

        abortButton = Editor.addToolbarButton(toolbar, "toolbar/stop.png", "Abort", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                abortCompilation();
            }
        });
        abortButton.setVisible(false);
        runButton = Editor.addToolbarButton(toolbar, "toolbar/run.png", "Compile", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compile();
            }
        });

        programButton = Editor.addToolbarButton(toolbar, "toolbar/burn.png", "Program", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                program();
            }
        });
                    
        toolbar.addSeparator();

        Editor.addToolbarButton(toolbar, "toolbar/new.png", "New Sketch", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.handleNew();
            }
        });



        Editor.addToolbarButton(toolbar, "toolbar/open.png", "Open Sketch", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleOpenPrompt();
            }
        });

        Editor.addToolbarButton(toolbar, "toolbar/save.png", "Save Sketch", new ActionListener() {
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
                    if (file.isDirectory()) {
                        if (expanded) {
                            icon = Base.loadIconFromResource("files/folder-open.png");
                        } else {
                            icon = Base.loadIconFromResource("files/folder.png");
                        }
                    } else {
                        icon = Base.loadIconFromResource(FileType.getIcon(file));
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
            node.setUserObject(file);
            if (file.isDirectory()) {
                addFileTreeToNode(node, file);
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
            } catch (ClassNotFoundException e) {
                error(e);
            }
        }

        public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }
            support.setShowDropLocation(true);
            if (support.isDataFlavorSupported(nodesFlavor)) {
                JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
                JTree tree = (JTree)support.getComponent();
                int dropRow = tree.getRowForPath(dl.getPath());

                int[] selRows = tree.getSelectionRows();
                for (int i = 0; i < selRows.length; i++) {
                    if (selRows[i] == dropRow) {
                        return false;
                    }
                }

                TreePath dest = dl.getPath();
                DefaultMutableTreeNode target = (DefaultMutableTreeNode)dest.getLastPathComponent();
                TreePath path = tree.getPathForRow(selRows[0]);
                DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode)path.getLastPathComponent();
                if (firstNode.getChildCount() > 0 && target.getLevel() < firstNode.getLevel()) {
                    return false;
                }
                return true;
            }

            try {
                if (support.isDataFlavorSupported(nixFileDataFlavor)) {
                    return true;
                }
            } catch (Exception e) {
                //error(e);
            }
            return false;
        }

        private boolean haveCompleteNode(JTree tree) {
            int[] selRows = tree.getSelectionRows();
            TreePath path = tree.getPathForRow(selRows[0]);
            DefaultMutableTreeNode first = (DefaultMutableTreeNode)path.getLastPathComponent();
            int childCount = first.getChildCount();
            if (childCount > 0 && selRows.length == 1) {
                return false;
            }

            for (int i = 1; i < selRows.length; i++) {
                path = tree.getPathForRow(selRows[i]);
                DefaultMutableTreeNode next = (DefaultMutableTreeNode)path.getLastPathComponent();
                if (first.isNodeChild(next)) {
                    if (childCount > selRows.length - 1) {
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
                    if (nodesToRemove[i].getParent() != null) {
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

                if (support.isDataFlavorSupported(nodesFlavor)) {
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
                    if (!destinationFile.isDirectory()) {
                        parent = (DefaultMutableTreeNode)parent.getParent();
                        destinationFile = destinationFile.getParentFile();
                    }

                    File originalParent = ((File)((DefaultMutableTreeNode)nodes[0].getUserObject()).getUserObject()).getParentFile();

                    if (destinationFile.equals(originalParent)) {
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
                        if (tab >= 0) {
                            setTabFile(tab, newFile);
                        }

                        // If the file being moved was a "top level" file then move it, otherwise ignore it.
                        if (nodeFile.getParentFile().equals(originalParent)) {
                            nodeFile.renameTo(newFile);
                        }


                        model.insertNodeInto(nodes[i], parent, index++);
                    }
                } else if (support.isDataFlavorSupported(nixFileDataFlavor)) {
                    Transferable t = support.getTransferable();
                    String data = (String)t.getTransferData(nixFileDataFlavor);

                    JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
                    TreePath dest = dl.getPath();
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)dest.getLastPathComponent();

                    File destinationFile = (File)parent.getUserObject();
                    if (!destinationFile.isDirectory()) {
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
                            if (file.isDirectory()) {
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
            } catch (Exception ee) {
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

    public void updateFilesTree() {
        TreePath[] saved = saveTreeState(sketchFilesTree);
        filesTreeRoot.removeAllChildren();
        filesTreeRoot.setUserObject(loadedSketch.getFolder());
        addFileTreeToNode(filesTreeRoot, loadedSketch.getFolder());
        filesTreeModel.nodeStructureChanged(filesTreeRoot);
        restoreTreeState(sketchFilesTree, saved);
    }

    public void updateTree() {
        boolean treeRootOpen = sketchContentTree.isExpanded(new TreePath(treeRoot.getPath()));

        treeRoot.setUserObject(loadedSketch.getName());

        updateSourceTree();
        updateHeadersTree();
        updateLibrariesTree();
        updateBinariesTree();
        updateOutputTree();
        updateFilesTree();

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
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                loadedSketch.purgeBuildFiles();
                                updateOutputTree();
                            }
                        });
                        menu.add(item);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } 
                } else if (o.getUserObject().getClass().equals(File.class)) {
                    File thisFile = (File)o.getUserObject();
                    JPopupMenu menu = new JPopupMenu();

                    JMenuItem renameItem = new JMenuItem("Rename file");
                    renameItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();
                            if (f.equals(mf)) {
                                alert("You cannot rename the main sketch file.\nUse \"Save As\" instead.");
                                return;
                            }
                            String newName = askForFilename(f.getName());
                            if (newName != null) {
                                File newFile = new File(f.getParentFile(), newName);
                                int tab = getTabByFile(f);
                                loadedSketch.renameFile(f, newFile);
                                f.renameTo(newFile);
                                if (tab >= 0) {
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
                            if (f.equals(mf)) {
                                alert("You cannot delete the main sketch file.");
                                return;
                            }
                            if (twoOptionBox(JOptionPane.QUESTION_MESSAGE, "Delete?", "Are you sure you want to delete\n" + f.getName() + "?", "Yes", "No") == 0) {
                                loadedSketch.deleteFile(f);
                                int tab = getTabByFile(f);
                                if (f.isDirectory()) {
                                    Base.removeDir(f);
                                } else {
                                    f.delete();
                                }
                                if (tab >= 0) {
                                    TabLabel tl = (TabLabel)editorTabs.getTabComponentAt(tab);
                                    tl.cancelFileWatcher();
                                    editorTabs.remove(tab);
                                }
                                updateTree();
                            }
                        }
                    });
                    deleteItem.setActionCommand(thisFile.getAbsolutePath());
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
                            if (compilerRunning()) {
                                error("Sorry, there is already a compiler thread running for this sketch.");
                                return;
                            }
                            LibCompileRunHandler runHandler = new LibCompileRunHandler(lib);
                            compilationThread = new Thread(runHandler, "Compiler");
                            compilationThread.start();
                        }
                    });
                    menu.add(item);
                    if (lib.isLocal(loadedSketch.getFolder())) {
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
                                if (loadedSketch.parentIsProtected()) {
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
                    if (o.getUserObject().getClass().equals(File.class)) {
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
            if (e.getButton() == 3) {
                DefaultMutableTreeNode o = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                DefaultMutableTreeNode p = (DefaultMutableTreeNode)o.getParent();
                if (o.getUserObject().getClass().equals(File.class)) {
                    File thisFile = (File)o.getUserObject();
                    JPopupMenu menu = new JPopupMenu();

                    if (thisFile.isDirectory()) {
                        JMenuItem mkdirItem = new JMenuItem("Create directory");
                        mkdirItem.setActionCommand(thisFile.getAbsolutePath());
                        mkdirItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                String fn = e.getActionCommand();
                                File f = new File(fn);
                                String newName = askForFilename(null);
                                if (newName != null) {
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
                    }

                    JMenuItem renameItem = new JMenuItem("Rename file");
                    renameItem.setActionCommand(thisFile.getAbsolutePath());
                    renameItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String fn = e.getActionCommand();
                            File f = new File(fn);
                            File mf = loadedSketch.getMainFile();
                            if (f.equals(mf)) {
                                alert("You cannot rename the main sketch file.\nUse \"Save As\" instead.");
                                return;
                            }
                            String newName = askForFilename(f.getName());
                            if (newName != null) {
                                File newFile = new File(f.getParentFile(), newName);
                                int tab = getTabByFile(f);
                                loadedSketch.renameFile(f, newFile);
                                f.renameTo(newFile);
                                if (tab >= 0) {
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
                            if (f.equals(mf)) {
                                alert("You cannot delete the main sketch file.");
                                return;
                            }
                            if (twoOptionBox(JOptionPane.QUESTION_MESSAGE, "Delete?", "Are you sure you want to delete\n" + f.getName() + "?", "Yes", "No") == 0) {
                                loadedSketch.deleteFile(f);
                                int tab = getTabByFile(f);
                                if (f.isDirectory()) {
                                    Base.removeDir(f);
                                } else {
                                    f.delete();
                                }
                                if (tab >= 0) {
                                    TabLabel tl = (TabLabel)editorTabs.getTabComponentAt(tab);
                                    tl.cancelFileWatcher();
                                    editorTabs.remove(tab);
                                }
                                updateTree();
                            }
                        }
                    });
                    deleteItem.setActionCommand(thisFile.getAbsolutePath());

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
                    menu.show(sketchFilesTree, e.getX(), e.getY());
                }
                return;
            }
            if(selRow != -1) {
                if(e.getClickCount() == 2) {
                    DefaultMutableTreeNode o = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                    if (o.getUserObject().getClass().equals(File.class)) {
                        File sf = (File)o.getUserObject();
                        String cl = FileType.getEditor(sf);
                        if (cl != null) {
                            openOrSelectFile(sf);
                        }
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
        Debug.message(msg);
        if (msg == null) { return; }
        if (!msg.endsWith("\n")) { msg += "\n"; }
        consoleDoc.appendString(msg, stdStyle);
        consoleDoc.insertAll();
        consoleTextPane.setCaretPosition(consoleDoc.getLength());
    }

    public void warning(String msg) {
        Debug.message(msg);
        if (!msg.endsWith("\n")) { msg += "\n"; }
        consoleDoc.appendString(msg, warnStyle);
        consoleDoc.insertAll();
        consoleTextPane.setCaretPosition(consoleDoc.getLength());
    }

    public void error(String msg) {
        Debug.message(msg);
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

    public static JButton addToolbarButton(JToolBar tb, String path, String tooltip) {
        return addToolbarButton(tb, path, tooltip, null);
    }

    public static JButton addToolbarButton(JToolBar tb, String path, String tooltip, ActionListener al) {
        ImageIcon buttonIcon = Base.loadIconFromResource(path);
        JButton button = new JButton(buttonIcon);
        button.setToolTipText(tooltip);
        if (al != null) {
            button.addActionListener(al);
        }
        button.setBorderPainted(false);
        button.addMouseListener(new MouseListener() {
            public void mouseEntered(MouseEvent e) {
                ((JButton)(e.getComponent())).setBackground(UIManager.getColor("control").brighter());
                ((JButton)(e.getComponent())).setOpaque(true);
            }
            public void mouseExited(MouseEvent e) {
                ((JButton)(e.getComponent())).setBackground(UIManager.getColor("control"));
                ((JButton)(e.getComponent())).setOpaque(false);
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseClicked(MouseEvent e) {}
        });
        tb.add(button);
        return button;
    }

    public boolean closeTab(int tab) {
        if (tab == -1) return false;
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

            if (option == 2) return false;
            if (option == 0) eb.save();
        }
        TabLabel tl = (TabLabel)editorTabs.getTabComponentAt(tab);
        tl.cancelFileWatcher();
        editorTabs.remove(tab);
        return true;
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

            TabLabel lab = new TabLabel(this, sf);
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

        if (loadedSketch.parentIsProtected()) {
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

    public void closeAllTabs() {
        while (editorTabs.getTabCount() > 0) {
            if (!closeTab(0)) {
                return;
            }
        }
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

        if (editorList.size() == 1) {
            if (Base.isMacOS()) {
                Object[] options = { Translate.t("OK"), Translate.t("Cancel") };
                String prompt =
                    "<html> " +
                    "<head> <style type=\"text/css\">"+
                    "b { font: 13pt \"Lucida Grande\" }"+
                    "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                    "</style> </head>" +
                    "<b>" + Translate.t("Are you sure you want to Quit?") + "</b>" +
                    "<p>" + Translate.t("Closing the last open sketch will quit %1.", Base.theme.get("product.cap"));

                int result = JOptionPane.showOptionDialog(this,
                    prompt,
                    Translate.t("Quit"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
                if (result == JOptionPane.NO_OPTION ||
                    result == JOptionPane.CLOSED_OPTION
                ) {
                    return false;
                }
            }
        }

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

        Editor.unregisterEditor(this);
        this.dispose();
        if (Editor.shouldQuit()) {
            Base.preferences.save();
            System.exit(0);
        }
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
                handleOpenPrompt();
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
        if (loadedSketch.getCore() != null) {
            addSketchesFromFolder(submenu, loadedSketch.getCompiler().getExamplesFolder());
            addSketchesFromFolder(submenu, loadedSketch.getCore().getExamplesFolder());
            addSketchesFromFolder(submenu, loadedSketch.getBoard().getExamplesFolder());

            submenu.addSeparator();

            JMenu compilerLibsMenu = new JMenu(loadedSketch.getCompiler().getName());
            TreeMap<String, Library> compilerLibraries = Base.getLibraryCollection("compiler:" + loadedSketch.getCompiler().getName(), loadedSketch.getCore().getName());
            for (String libName : compilerLibraries.keySet()) {
                JMenu libMenu = new JMenu(libName);
                addSketchesFromFolder(libMenu, compilerLibraries.get(libName).getExamplesFolder());
                if (libMenu.getItemCount() > 0) {
                    compilerLibsMenu.add(libMenu);
                }
            }

            JMenu coreLibsMenu = new JMenu(loadedSketch.getCore().getName());
            TreeMap<String, Library> coreLibraries = Base.getLibraryCollection("core:" + loadedSketch.getCore().getName(), loadedSketch.getCore().getName());
            for (String libName : coreLibraries.keySet()) {
                JMenu libMenu = new JMenu(libName);
                addSketchesFromFolder(libMenu, coreLibraries.get(libName).getExamplesFolder());
                if (libMenu.getItemCount() > 0) {
                    coreLibsMenu.add(libMenu);
                }
            }

            JMenu boardLibsMenu = new JMenu(loadedSketch.getBoard().getName());
            TreeMap<String, Library> boardLibraries = Base.getLibraryCollection("board:" + loadedSketch.getBoard().getName(), loadedSketch.getCore().getName());
            for (String libName : boardLibraries.keySet()) {
                JMenu libMenu = new JMenu(libName);
                addSketchesFromFolder(libMenu, boardLibraries.get(libName).getExamplesFolder());
                if (libMenu.getItemCount() > 0) {
                    boardLibsMenu.add(libMenu);
                }
            }

            if (compilerLibsMenu.getItemCount() > 0) {
                submenu.add(compilerLibsMenu);
            }

            if (coreLibsMenu.getItemCount() > 0) {
                submenu.add(coreLibsMenu);
            }

            if (boardLibsMenu.getItemCount() > 0) {
                submenu.add(boardLibsMenu);
            }


            for (String key : Base.libraryCategoryNames.keySet()) {
                TreeMap<String, Library> catLib = Base.getLibraryCollection("cat:" + key, getCore().getName());
                JMenu catLibsMenu = new JMenu(Base.libraryCategoryNames.get(key));
                for (Library lib : catLib.values()) {
                    JMenu libMenu = new JMenu(lib.getName());
                    addSketchesFromFolder(libMenu, lib.getExamplesFolder());
                    if (libMenu.getItemCount() > 0) {
                        catLibsMenu.add(libMenu);
                    }
                }
                if (catLibsMenu.getItemCount() > 0) {
                    submenu.add(catLibsMenu);
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
                Preferences prefs = new Preferences();
                prefs.showFrame();
            }
        });
        fileMenu.add(item);

        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_BOTTOM);

        item = new JMenuItem(Translate.t("Quit"));
        item.setAccelerator(KeyStroke.getKeyStroke("alt Q"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (Editor.closeAllEditors()) {
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

        for (String link : links.childKeys()) {
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

        for (String link : links.childKeys()) {
            String iname = links.get(link + ".name");
            if (iname != null) {
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
        helpMenu.add(submenu);

    }

    public void populateOptionsMenu(JMenu menu) {
        TreeMap<String, String> opts = loadedSketch.getOptionGroups();
        for (String opt : opts.keySet()) {
            JMenu submenu = new JMenu(opts.get(opt));
            TreeMap<String, String>optvals = loadedSketch.getOptionNames(opt);
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
        menu.removeAll();
        ButtonGroup portGroup = new ButtonGroup();
        ArrayList<String> ports = Serial.getPortList();
        for (String port : ports) {
            String pn = Serial.getName(port);
            JMenuItem item = null;
            if (pn != null & !pn.equals("")) {
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
        TreeMap<String, String> programmers = loadedSketch.getProgrammerList();
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

        if (groups == null) {
            return;
        }

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
            if (loadedSketch.getBoard() != null) {
                if (loadedSketch.getBoard().equals(board)) {
                    item.setSelected(true);
                }
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
            if (loadedSketch.getCore() != null) {
                item.setSelected(loadedSketch.getCore().equals(core));
            }
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

        if (loadedSketch.getBoard() == null) {
            setStatus("No board selected!");
            return;
        }            

        if (loadedSketch.getSerialPort() == null) {
            setStatus("No port selected!");
            return;
        }            

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

    public boolean willDoImport;

    public void importSar() {
        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        javax.swing.filechooser.FileFilter filter = new SarFileFilter();
        fc.setFileFilter(filter);

        fc.setCurrentDirectory(Base.getSketchbookFolder());

        int rv = fc.showOpenDialog(this);

        if (rv == JFileChooser.APPROVE_OPTION) {
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
                dests.addAll(Base.libraryCategoryNames.values());

                for (String l : libarr) {
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

                if (willDoImport) {
                    String oldSketchName = sketchName;
                    sketchName = sketchNameBox.getText();
                    File targetDir = new File(Base.getSketchbookFolder(), sketchName);
                    message("Importing to " + targetDir.getAbsolutePath());
                    int n = 0;
                    if (targetDir.exists()) {
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
                        if (n == 0) {
                            Base.removeDir(targetDir);
                        }
                    }

                    if (n == 0) {
                        message("Starting import...");
                        byte[] buffer = new byte[1024];
                        ZipInputStream zis = new ZipInputStream(new FileInputStream(sarFile));
                        ZipEntry ze = zis.getNextEntry();
                        while (ze != null) {
                            String fileName = ze.getName();
                            message("  " + fileName + " ->");
                            String spl[] = fileName.split("/");

                            File newFile = null;

                            if (spl[0].equals("META-INF")) {
                                ze = zis.getNextEntry();
                                continue;
                            }

                            if (spl[0].equals(oldSketchName)) {
                                spl[0] = sketchName;
                            }

                            if (fileName.startsWith(".")) {
                                ze = zis.getNextEntry();
                                continue;
                            }

                            if (spl[0].equals("libraries")) {
                                if (spl.length > 1) {
                                    if (libcheck.get(spl[1]) == null) {
                                        // This is a library we don't know about - ignore it
                                        ze = zis.getNextEntry();
                                        warning("    (ignore)");
                                        continue;
                                    }

                                    JComboBox cb = libcheck.get(spl[1]);
                                    if (cb.getSelectedIndex() == 0) {
                                        // The library isn't selected for import
                                        ze = zis.getNextEntry();
                                        message("    (skip)");
                                        continue;
                                    }

                                    // We now need to re-hash the file name.  Remove the
                                    // "libraries" from the front, and replace it with the
                                    // path of the selected target.

                                    File libTarget = null;
                                    if (cb.getSelectedIndex() == 1) {
                                        // Sketch folder
                                        File sf = new File(Base.getSketchbookFolder(), sketchName);
                                        libTarget = new File(sf, "libraries");
                                    } else {
                                        String dname = (String)cb.getSelectedItem();
                                        String cat = null;
                                        for (String k : Base.libraryCategoryNames.keySet()) {
                                            if (Base.libraryCategoryNames.get(k).equals(dname)) {
                                                cat = k;
                                            }
                                        }
                                        libTarget = Base.libraryCategoryPaths.get(cat);
                                    }

                                    if (libTarget != null) {
                                        newFile = libTarget;
                                        for (int i = 1; i < spl.length; i++) {
                                            newFile = new File(newFile, spl[i]);
                                        }
                                    }
                                } else {
                                    ze = zis.getNextEntry();
                                    continue;
                                }
                            } else {
                                newFile = Base.getSketchbookFolder();
                                for (int i = 0; i < spl.length; i++) {
                                    if (spl[i].equals(oldSketchName)) {
                                        spl[i] = sketchName;
                                    }
                                    int di = spl[i].lastIndexOf(".");
                                    if (di >= 0) {
                                        String s = spl[i].substring(0, di);
                                        String e = spl[i].substring(di + 1);
                                        warning(s);
                                        warning(e);
                                        if (s.equals(oldSketchName)) {
                                            spl[i] = sketchName + "." + e;
                                        }
                                    }
                                    newFile = new File(newFile, spl[i]);
                                }
                            }

                            if (newFile == null) {
                                ze = zis.getNextEntry();
                                error("    (broken)");
                                continue;
                            }
                            message("    " + newFile.getAbsolutePath());

                            newFile.getParentFile().mkdirs();

                            if (ze.isDirectory()) {
                                newFile.mkdirs();
                            } else {
                                FileOutputStream fos = new FileOutputStream(newFile);
                                int len;
                                while ((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                                fos.close();
                                if (!newFile.exists()) {    
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
            } catch (Exception e) {
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
        if (rv == JFileChooser.APPROVE_OPTION) {
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
            insertStringAtStart(loadedSketch.getMainFile(), "#include <" + lib + ".h>\n");
            loadedSketch.addLibraryToImportList(lib);
            updateLibrariesTree();
        }
    };

    void insertStringAtStart(File f, String s) {
        openOrSelectFile(f);
        int tab = getTabByFile(f);
        if (tab == -1) {
            return;
        }
        EditorBase eb = getTab(tab);
        eb.insertAtStart(s);
    }

    public void populateLibrariesMenu(JComponent menu) {
        JMenuItem item;

        if (loadedSketch.getCompiler() != null) {
            JMenu compilerLibsMenu = new JMenu(loadedSketch.getCompiler().getName());
            TreeMap<String, Library> compilerLibraries = Base.getLibraryCollection("compiler:" + loadedSketch.getCompiler().getName(), loadedSketch.getCore().getName());
            for (String libName : compilerLibraries.keySet()) {
                item = new JMenuItem(libName);
                item.addActionListener(insertIncludeAction);
                item.setActionCommand(libName);
                compilerLibsMenu.add(item);
            }
            if (compilerLibsMenu.getItemCount() > 0) {
                menu.add(compilerLibsMenu);
            }
        }

        if (loadedSketch.getCore() != null) {
            JMenu coreLibsMenu = new JMenu(loadedSketch.getCore().getName());
            TreeMap<String, Library> coreLibraries = Base.getLibraryCollection("core:" + loadedSketch.getCore().getName(), loadedSketch.getCore().getName());
            for (String libName : coreLibraries.keySet()) {
                item = new JMenuItem(libName);
                item.addActionListener(insertIncludeAction);
                item.setActionCommand(libName);
                coreLibsMenu.add(item);
            }
            if (coreLibsMenu.getItemCount() > 0) {
                menu.add(coreLibsMenu);
            }
        }

        if (loadedSketch.getBoard() != null && loadedSketch.getCore() != null) {
            JMenu boardLibsMenu = new JMenu(loadedSketch.getBoard().getName());
            TreeMap<String, Library> boardLibraries = Base.getLibraryCollection("board:" + loadedSketch.getBoard().getName(), loadedSketch.getCore().getName());
            for (String libName : boardLibraries.keySet()) {
                item = new JMenuItem(libName);
                item.addActionListener(insertIncludeAction);
                item.setActionCommand(libName);
                boardLibsMenu.add(item);
            }
            if (boardLibsMenu.getItemCount() > 0) {
                menu.add(boardLibsMenu);
            }
        }

        if (loadedSketch.getBoard() != null && loadedSketch.getCore() != null) {
            for (String key : Base.libraryCategoryNames.keySet()) {
                JMenu catLibsMenu = new JMenu(Base.libraryCategoryNames.get(key));
                TreeMap<String, Library> catLib = Base.getLibraryCollection("cat:" + key, getCore().getName());
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

    public void releasePort(String portName) {
        for (Plugin plugin : plugins) {
            try {
                plugin.releasePort(portName);
            } catch (Exception e) {
                Base.error(e);
            }
        }
    }

    public void launchPlugin(Class<?> pluginClass) {
        for (Plugin plugin : plugins) {
            if (plugin.getClass() == pluginClass) {
                Debug.message("Launching plugin " + plugin.getClass());
                plugin.launch();
            }
        }
    }

    public static void registerEditor(Editor e) {
        editorList.remove(e); // Just in case...?
        editorList.add(e);
    }

    public static void unregisterEditor(Editor e) {
        editorList.remove(e);
    }

    public static boolean shouldQuit() {
        return (editorList.size() == 0);
    }

    public static void broadcast(String msg) {
        for (Editor e : editorList) {
            e.message(msg);
        }
    }

    public static void updateAllEditors() {
        for (Editor e : editorList) {
            e.updateAll();
        }
    }

    public static void selectAllEditorBoards() {
        for (Editor e : editorList) {
            e.reselectEditorBoard();
        }
    }

    public void reselectEditorBoard() {
        String eb = loadedSketch.getBoardName();
        if (eb != null) {
            loadedSketch.setBoard(eb);
        }
    }

    public static boolean closeAllEditors() {
        for (Editor e : editorList) {
            if (e.askCloseWindow() == false) {
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

        if (rv == JFileChooser.APPROVE_OPTION) {
            loadSketch(fc.getSelectedFile());
        }
    }

    public void loadSketch(String f) {
        loadSketch(new File(f));
    }

    public void loadSketch(File f) {
        if (loadedSketch.isUntitled() && !isModified()) {
            closeAllTabs();
            loadedSketch = new Sketch(f);
            loadedSketch.attachToEditor(this);
            filesTreeRoot.setUserObject(loadedSketch.getFolder());
            updateAll();
            treeModel.nodeStructureChanged(treeRoot);
            openOrSelectFile(loadedSketch.getMainFile());
        } else {
            Base.createNewEditor(f.getPath());
        }
    }

    public static void updateLookAndFeel() {
        for (Editor e : editorList) {
            SwingUtilities.updateComponentTreeUI(e);
        }
    }

    public static void releasePorts(String n) {
        for (Editor e : editorList) {
            e.releasePort(n);
        }
    }

    public static void refreshAllEditors() {
        for (Editor e : editorList) {
            e.refreshEditors();
        }
    }

    public void refreshEditors() {
        int ntabs = editorTabs.getTabCount();
        for (int i = 0; i < ntabs; i++) {
            EditorBase eb = getTab(i);
            eb.refreshSettings();
        }
    }

    public void handleAbout() {
        Dimension ss = getSize();
        Point sl = getLocation();
        Splash splash = new Splash(sl.x, sl.y, ss.width, ss.height);
        splash.enableCloseOnClick();

    }

    public void installLibraryArchive() {
        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.getName().toLowerCase().endsWith(".zip")) {
                    return true;
                }
                if (f.isDirectory()) {
                    return true;
                }
                return false;
            }

            public String getDescription() {
                return Translate.t("ZIP Files");
            }
        });

        int rv = fc.showOpenDialog(this);
        if (rv == JFileChooser.APPROVE_OPTION) {
            String[] entries = getZipEntries(fc.getSelectedFile());
            message("Analyzing " + fc.getSelectedFile().getName() + "...");

            // In a library we expect certain things to be in certain places.  That's only
            // logical, or the IDE won't be able to find them!  People that don't package
            // their libraries up properly are just clueless.  Still, we have to handle all
            // sorts of idiocy here, so this is going to be quite a complex operation.

            TreeMap<String, String> foundLibs = new TreeMap<String, String>();

            // First we'll do a quick check to see if it's a properly packaged archive.
            // We expect to have at least one header file (xxx.h) in one folder (xxx/)

            for (String entry : entries) {
                if (entry.endsWith(".h")) {

                    String[] parts = entry.split("/");
                    if (parts.length >= 2) {
                        String name = parts[parts.length-1];
                        String parent = parts[parts.length-2];

                        String possibleLibraryName = name.substring(0, name.lastIndexOf("."));
                        String folder = "";
                        for (int i = 0; i < parts.length-1; i++) {
                            if (folder.equals("") == false) {
                                folder += "/";
                            }
                            folder += parts[i];
                        }

                        if (parent.equals(possibleLibraryName) || parent.contains(possibleLibraryName)) {
                            // this looks like a valid archive at this point.
                            message("Found library " + possibleLibraryName + " at " + folder);
                            foundLibs.put(possibleLibraryName, folder);
                        }
                    }
                }
            }

            if (foundLibs.size() > 0) {
                ArrayList<LibCatObject> cats = new ArrayList<LibCatObject>();

                for (String key : Base.libraryCategoryNames.keySet()) {
                    LibCatObject ob = new LibCatObject(key, Base.libraryCategoryNames.get(key));
                    cats.add(ob);
                }
                LibCatObject[] catarr = cats.toArray(new LibCatObject[cats.size()]);
                
                LibCatObject loc = (LibCatObject)JOptionPane.showInputDialog(this, "Select location to store this library:", "Select Destination", JOptionPane.PLAIN_MESSAGE,
                    null, catarr, null);

                if (loc != null) {
                    File installPath = Base.libraryCategoryPaths.get(loc.getKey());
                    message("Installing to " + installPath.getAbsolutePath());

                    for (String lib : foundLibs.keySet()) {
                        message("Installing " + lib + "...");
                        // First make a list of remapped files...
                        TreeMap<String, File> translatedFiles = new TreeMap<String, File>();

                        File libDir = new File(installPath, lib);

                        String prefix = foundLibs.get(lib) + "/";
                        for (String entry : entries) {
                            if (entry.startsWith(prefix)) {
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
            while (ze != null) {
                File outputFile = mapping.get(ze.getName());
                if (outputFile != null) {
                    File p = outputFile.getParentFile();
                    if (!p.exists()) {
                        p.mkdirs();
                    }
                    byte[] buffer = new byte[1024];
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    outputFile.setExecutable(true, false);
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
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
            while (ze != null) {
                if (!ze.isDirectory()) {
                    files.add(ze.getName());
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }

        String[] out = files.toArray(new String[0]);
        Arrays.sort(out);

        return out;
    }

    public String askForFilename(String oldName) {
        if (oldName == null) {
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
        if (!destFolder.isDirectory()) {
            return;
        }
        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.getName().toLowerCase().endsWith(".zip")) {
                    return true;
                }
                if (f.isDirectory()) {
                    return true;
                }
                return false;
            }

            public String getDescription() {
                return Translate.t("ZIP Files");
            }
        });

        int rv = fc.showOpenDialog(this);
        if (rv == JFileChooser.APPROVE_OPTION) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            File zipFile = fc.getSelectedFile();
            try {
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    File extractTo = new File(destFolder, ze.getName());
                    if (ze.isDirectory()) {
                        extractTo.mkdirs();
                    } else {
                        File p = extractTo.getParentFile();
                        if (!p.exists()) {
                            p.mkdirs();
                        }
                        byte[] buffer = new byte[1024];
                        FileOutputStream fos = new FileOutputStream(extractTo);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        extractTo.setExecutable(true, false);
                    }
                    ze = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
            } catch (Exception e) {
                System.err.println(e.getMessage());
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
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                getPaths(tree, path, list);
            }
        }
    }

    public TreePath[] saveTreeState(JTree tree) {
        TreePath[] allPaths = getPaths(tree);
        ArrayList<TreePath> openPaths = new ArrayList<TreePath>();

        for (TreePath path : allPaths) {
            if (tree.isExpanded(path)) {
                openPaths.add(path);
            }
        }

        return (TreePath[]) openPaths.toArray(new TreePath[openPaths.size()]);
    }

    public void restoreTreeState(JTree tree, TreePath[] savedState) {
        TreePath[] allPaths = getPaths(tree);
        for (TreePath path : savedState) {
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
    }

    public void compile() {
        clearConsole();
        if (compilerRunning()) {
            error("Sorry, there is already a compiler thread running for this sketch.");
            return;
        }
        DefaultRunHandler runHandler = new DefaultRunHandler(false);
        compilationThread = new Thread(runHandler, "Compiler");
        compilationThread.start();
    }

    public void program() {
        clearConsole();
        if (compilerRunning()) {
            error("Sorry, there is already a compiler thread running for this sketch.");
            return;
        }
        DefaultRunHandler runHandler = new DefaultRunHandler(true);
        compilationThread = new Thread(runHandler, "Compiler");
        compilationThread.start();
    }

    public boolean compilerRunning() {
        if (compilationThread == null) {
            return false;
        }
        return compilationThread.isAlive();
    }

    public void abortCompilation() {
        if (!compilerRunning()) {
            return;
        }
        loadedSketch.requestTermination();
        while (compilerRunning()) {
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
            if (rv == JFileChooser.APPROVE_OPTION) {
                File archiveFile = fc.getSelectedFile();
                if (archiveFile.exists()) {
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
        } catch (Exception e) {
            Base.error(e);
        }
    }
}

