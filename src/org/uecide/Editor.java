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

import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

import com.wittams.gritty.swing.*;

public class Editor extends JFrame {

    Box mainDecorationContainer;

    FixedSplitPane topBottomSplit;
    FixedSplitPane leftRightSplit;
    FixedSplitPane sidebarSplit;

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
    JMenu optionsMenu;
    JMenu programmersSubmenu; 

    JToolBar toolbar;
    JToolBar miniBar;
    JToolBar treeToolBar;

    JPanel treePanel;
    JPanel editorPanel;
    JPanel statusBar;
    JPanel projectPanel;
    JPanel sidebarPanel;
    JPanel filesPanel;
    JPanel consolePanel;

    Console console;
    GrittyTerminal testConsole;
    ConsoleTty outputTty;

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

    HashMap<String, Object> dataStore = new HashMap<String, Object>();

    AnimatedIcon abortIcon;

    public static ArrayList<Editor>editorList = new ArrayList<Editor>();

    ArrayList<Plugin> plugins = new ArrayList<Plugin>();
    ArrayList<JSPlugin> jsplugins = new ArrayList<JSPlugin>();

    Thread compilationThread = null;


    public void set(String s, Object o) {
        dataStore.put(s, o);
    }

    public Object get(String s) {
        return dataStore.get(s);
    }

    public void indicateStartCompiling() {
        abortIcon.start(abortButton);
        runButton.setEnabled(false);
        programButton.setEnabled(false);
        runButton.setVisible(false);
        abortButton.setVisible(true);
    }

    public void indicateStopCompiling() {
        runButton.setEnabled(true);
        programButton.setEnabled(true);
        runButton.setVisible(true);
        abortButton.setVisible(false);
        abortIcon.stop();
    }

    class DefaultRunHandler implements Runnable {
        boolean upload = false;
        public DefaultRunHandler(boolean doUpload) {
            upload = doUpload;
        }
        public void run() {
            indicateStartCompiling();

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

            indicateStopCompiling();
        }
    }

    class LibCompileRunHandler implements Runnable {
        Library library;
        public LibCompileRunHandler(Library lib) {
            library = lib;
        }
        public void run() {
            indicateStartCompiling();

            try {
                loadedSketch.precompileLibrary(library);
            } catch(Exception e) {
                error(e);
            }

            indicateStopCompiling();
        }
    }

    public Editor(Sketch s) throws IOException {
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

        for (JSPlugin plugin : Base.jsplugins.values()) {
            JSPlugin newCopy = new JSPlugin(plugin, this);
            jsplugins.add(newCopy);
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

        int width = Preferences.getInteger("editor.layout.window.width");
        int height = Preferences.getInteger("editor.layout.window.height");


        // Check for old preference types and convert them

        if (Preferences.getFloat("editor.layout.splits.tree") > 0f && Preferences.getFloat("editor.layout.splits.tree") < 1f) {

            Preferences.setInteger("editor.layout.splits.tree", 200);
            Preferences.setInteger("editor.layout.splits.console", 200);
            Preferences.setInteger("editor.layout.splits.sidebar", 0);
        }

        leftRightSplit = new FixedSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectPanel, editorPanel, "editor.layout.splits.tree", FixedSplitPane.LEFT);
        sidebarSplit = new FixedSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftRightSplit, sidebarPanel, "editor.layout.splits.sidebar", FixedSplitPane.RIGHT);
        topBottomSplit = new FixedSplitPane(JSplitPane.VERTICAL_SPLIT, sidebarSplit, consolePanel, "editor.layout.splits.console", FixedSplitPane.BOTTOM);

        add(topBottomSplit, BorderLayout.CENTER);

        final Editor me = this;

        consoleScroll = new JShadowScrollPane(
            Preferences.getInteger("theme.console.shadow.top"),
            Preferences.getInteger("theme.console.shadow.bottom")
        );

        outputScroll = new JShadowScrollPane(
            Preferences.getInteger("theme.console.shadow.top"),
            Preferences.getInteger("theme.console.shadow.bottom")
        );

        console = new Console();

        testConsole = new GrittyTerminal();
        outputTty = new ConsoleTty();
        testConsole.getTermPanel().setSize(new Dimension(100, 100));
        testConsole.getTermPanel().setAntiAliasing(true);
        testConsole.getTermPanel().setFont(Preferences.getFont("theme.console.fonts.command"));

        testConsole.getTermPanel().addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                testConsole.getScrollBar().setValue(
                    testConsole.getScrollBar().getValue() + (
                        e.getScrollAmount() * e.getWheelRotation()
                    )
                );
            }
        });

        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new BorderLayout());
        outputPanel.add(testConsole.getTermPanel(), BorderLayout.CENTER);
        outputPanel.add(testConsole.getScrollBar(), BorderLayout.EAST);

        testConsole.setTty(outputTty);
        testConsole.start();

        console.setURLClickListener(this);

        consoleScroll.setViewportView(console);

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

        JButton refreshButton = new ToolbarButton("actions", "refresh", "Refresh Project Tree", 24, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!compilerRunning()) {
                    loadedSketch.rescanFileTree();
                    updateTree();
                }
            }
        });
        treeToolBar.add(refreshButton);

        JButton projectSearchButton = new ToolbarButton("actions", "search", "Search entire project", 24, new ActionListener() {
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
        consoleTabs.add(outputPanel, Base.i18n.string("tab.output"));

        addPanelsToTabs(consoleTabs, Plugin.TABS_CONSOLE);

//        rotateTabLabels();
        

        updateToolbar();

        toolbar.add(new ToolbarSpacer()); //Separator();


        miniBar = new JToolBar();
        miniBar.setFloatable(false);

        miniBar.add(new ToolbarButton("actions", "run", Base.i18n.string("toolbar.run"), 16, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    loadedSketch.purgeCache();
                    loadedSketch.purgeBuildFiles();
                }

                try {
                    compile();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

       miniBar.add(new ToolbarButton("actions", "program", Base.i18n.string("toolbar.program"), 16, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    loadedSketch.purgeCache();
                    loadedSketch.purgeBuildFiles();
                }
                try {
                    program();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        miniBar.add(new ToolbarButton("actions", "new", Base.i18n.string("toolbar.new"), 16, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Base.handleNew();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        miniBar.add(new ToolbarButton("actions", "open", Base.i18n.string("toolbar.open"), 16, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    handleOpenPrompt();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        miniBar.add(new ToolbarButton("actions", "save", Base.i18n.string("toolbar.save"), 16, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    saveAllTabs();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));


        menuBar = new JMenuBar();


        fileMenu = new JMenu(Base.i18n.string("menu.file"));
        menuBar.add(fileMenu);

        editMenu = new JMenu(Base.i18n.string("menu.edit"));
        menuBar.add(editMenu);

        sketchMenu = new JMenu(Base.i18n.string("menu.sketch"));
        menuBar.add(sketchMenu);

        hardwareMenu = new JMenu(Base.i18n.string("menu.hardware"));
        menuBar.add(hardwareMenu);

        toolsMenu = new JMenu(Base.i18n.string("menu.tools"));
        menuBar.add(toolsMenu);

        helpMenu = new JMenu(Base.i18n.string("menu.help"));
        menuBar.add(helpMenu);

        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(miniBar);

        setJMenuBar(menuBar);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    if (askCloseWindow()) {
                        if(Editor.shouldQuit()) {
                            Preferences.save();
                            // Do we want to open a new empty editor?
                            if (Preferences.getBoolean("editor.newonclose")) {
                                Base.handleNew();
                            } else {
                                System.exit(0); 
                            }
                        }
                    }
                } catch (IOException ex) {
                    error(ex);
                }
            }
        });
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        if (Preferences.getBoolean("editor.layout.minimal")) {
            enableMinimalMode();
        } else {
            disableMinimalMode();
        }

        this.pack();

        setSize(width, height);
        setLocation(Preferences.getInteger("editor.layout.window.x"), Preferences.getInteger("editor.layout.window.y"));
        setLocationRelativeTo(null);
        setProgress(0);
        updateAll();

        addComponentListener(new ComponentListener() {
            public void componentMoved(ComponentEvent e) {
                Point windowPos = e.getComponent().getLocation(null);
                Preferences.setInteger("editor.layout.window.x", windowPos.x);
                Preferences.setInteger("editor.layout.window.y", windowPos.y);
            }
            public void componentResized(ComponentEvent e) {
                Dimension windowSize = e.getComponent().getSize(null);
                Preferences.setInteger("editor.layout.window.width", windowSize.width);
                Preferences.setInteger("editor.layout.window.height", windowSize.height);

//                Editor.this.updateSplits();
            }
            public void componentHidden(ComponentEvent e) {
            }
            public void componentShown(ComponentEvent e) {
            }
        });

        openOrSelectFile(loadedSketch.getMainFile());
/*
        for (int i = 0; toolbar.getComponentAtIndex(i) != null; i++) {
            Component c = toolbar.getComponentAtIndex(i);
            if (c instanceof JButton) {
                JButton b = (JButton)c;
                b.setBorderPainted(false);
            }
        }
*/
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

    public void updateToolbar() throws IOException {
        toolbar.removeAll();

        abortIcon = new AnimatedIcon(100,
            new InternalIcon("spinner", "circle1", 24),
            new InternalIcon("spinner", "circle2", 24),
            new InternalIcon("spinner", "circle3", 24),
            new InternalIcon("spinner", "circle4", 24)
        );

        //"actions", "cancel", Base.i18n.string("toolbar.abort"), 24, new ActionListener() {
        abortButton = new ToolbarButton(abortIcon, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                abortCompilation();
            }
        });
        abortButton.setVisible(false);
        toolbar.add(abortButton);

        runButton = new ToolbarButton("actions", "run", Base.i18n.string("toolbar.run"), 24, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    loadedSketch.purgeCache();
                    loadedSketch.purgeBuildFiles();
                }

                try {
                    compile();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        });
        toolbar.add(runButton);
        runButton.setBorderPainted(false);

        programButton = new ToolbarButton("actions", "program", Base.i18n.string("toolbar.program"), 24, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    loadedSketch.purgeCache();
                    loadedSketch.purgeBuildFiles();
                }
                try {
                    program();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        });
        toolbar.add(programButton);
        toolbar.add(new ToolbarSpacer()); //Separator();

        toolbar.add(new ToolbarButton("actions", "new", Base.i18n.string("toolbar.new"), 24, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Base.handleNew();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        toolbar.add(new ToolbarButton("actions", "open", Base.i18n.string("toolbar.open"), 24, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    handleOpenPrompt();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        toolbar.add(new ToolbarButton("actions", "save", Base.i18n.string("toolbar.save"), 24, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    saveAllTabs();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        toolbar.add(new ToolbarSpacer()); //Separator();

        addPluginsToToolbar(toolbar, Plugin.TOOLBAR_EDITOR);

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
        TreeCellRenderer renderer = new FileCellRenderer(this);
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

//        private boolean haveCompleteNode(JTree tree) {
//            int[] selRows = tree.getSelectionRows();
//            TreePath path = tree.getPathForRow(selRows[0]);
//            DefaultMutableTreeNode first = (DefaultMutableTreeNode)path.getLastPathComponent();
//            int childCount = first.getChildCount();
//
//            if(childCount > 0 && selRows.length == 1) {
//                return false;
//            }
//
//            for(int i = 1; i < selRows.length; i++) {
//                path = tree.getPathForRow(selRows[i]);
//                DefaultMutableTreeNode next = (DefaultMutableTreeNode)path.getLastPathComponent();
//
//                if(first.isNodeChild(next)) {
//                    if(childCount > selRows.length - 1) {
//                        return false;
//                    }
//                }
//            }
//
//            return true;
//        }

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
                        return false;
                    } catch(java.io.IOException ioe) {
                        System.out.println("I/O error: " + ioe.getMessage());
                        return false;
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
                                Files.copy(file.toPath(), destfile.toPath(), REPLACE_EXISTING);
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
                            ArrayList<FunctionBookmark> funcs = loadedSketch.getFunctionsForFile(f);

                            if(funcs != null) {
                                for(FunctionBookmark b : funcs) {
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
                        goToLineInFile(bm.getFile(), bm.getLine());
                    } else if (userObject instanceof TodoEntry) {
                        TodoEntry ent = (TodoEntry)userObject;
                        goToLineInFile(ent.getFile(), ent.getLine());
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
                                Utils.open(loadedSketch.getFolder().getAbsolutePath());
                            }
                        });
                        menu.add(openInOS);

                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SKETCH | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals(Base.i18n.string("tree.source"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.create.ino"));
                        item.setActionCommand("ino");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.create.cpp"));
                        item.setActionCommand("cpp");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.create.c"));
                        item.setActionCommand("c");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.create.asm"));
                        item.setActionCommand("S");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.create.blk"));
                        item.setActionCommand("blk");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.import.source"));
                        item.setActionCommand("source");
                        item.addActionListener(importFileAction);
                        menu.add(item);

                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_SOURCE | Plugin.MENU_BOTTOM, o);

                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals(Base.i18n.string("tree.headers"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.create.header"));
                        item.setActionCommand("h");
                        item.addActionListener(createNewAction);
                        menu.add(item);
                        item = new JMenuItem(Base.i18n.string("menu.import.header"));
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
                    } else if(s.equals(Base.i18n.string("tree.binaries"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.import.binary"));
                        item.setActionCommand("binary");
                        item.addActionListener(importFileAction);
                        menu.add(item);

                        populateContextMenu(menu, Plugin.MENU_TREE_BINARIES | Plugin.MENU_TOP, o);

                        item = new JMenuItem("Create new PNG");
                        item.setActionCommand("png");
                        item.addActionListener(createNewAction);
                        menu.add(item);

                        item = new JMenuItem("Create new JPEG");
                        item.setActionCommand("jpg");
                        item.addActionListener(createNewAction);
                        menu.add(item);

                        item = new JMenuItem("Create new GIF");
                        item.setActionCommand("gif");
                        item.addActionListener(createNewAction);
                        menu.add(item);

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
                        menu.add(item);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_TOP, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_MID, o);
                        populateContextMenu(menu, Plugin.MENU_TREE_OUTPUT | Plugin.MENU_BOTTOM, o);
                        menu.show(sketchContentTree, e.getX(), e.getY());
                    } else if(s.equals(Base.i18n.string("tree.docs"))) {
                        JMenuItem item = new JMenuItem(Base.i18n.string("menu.create.markdown"));
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
                                    Base.tryDelete(f);
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

                    JMenuItem infoMenu = new JMenu(Base.i18n.string("menu.file.info"));
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
                    menu.add(infoMenu);

                    populateContextMenu(menu, Plugin.MENU_TREE_FILE | Plugin.MENU_MID, o);

                    if(p.getUserObject() instanceof String) {
                        String ptext = (String)p.getUserObject();

                        if(ptext.equals(Base.i18n.string("tree.binaries"))) {

                            switch (FileType.getType(thisFile)) {
                                case FileType.GRAPHIC: {
                                    JMenuItem imageConversion = new JMenuItem("Image conversion options");
                                    imageConversion.addActionListener(new ActionListener() {
                                        public void actionPerformed(ActionEvent e) {
                                            String filename = e.getActionCommand();
                                            Editor.this.openImageConversionSettings(filename);
                                        }
                                    });
                                    imageConversion.setActionCommand(thisFile.getName());
                                    menu.add(imageConversion);
                                } break;

                                default: {
                                    JMenuItem binaryConversion = new JMenuItem("Binary conversion options");
                                    binaryConversion.addActionListener(new ActionListener() {
                                        public void actionPerformed(ActionEvent e) {
                                            String filename = e.getActionCommand();
                                            Editor.this.openBinaryConversionSettings(filename);
                                        }
                                    });
                                    binaryConversion.setActionCommand(thisFile.getName());
                                    menu.add(binaryConversion);
                                } break;

                            }
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
                    menu.add(item);

                    if(lib.isLocal(loadedSketch.getFolder())) {
                        item = new JMenuItem(Base.i18n.string("menu.library.export"));
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                exportLocalLibrary(lib);
                            }
                        });
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
//            ActionListener createNewAction = new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    // createNewAnyFile(e.getActionCommand());
//                }
//            };
//            ActionListener importFileAction = new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    // importAnyFile(e.getActionCommand());
//                }
//            };

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
                                Utils.open(e.getActionCommand());
                            }
                        });
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
                        menu.add(mkdirItem);
                        JMenuItem unzipItem = new JMenuItem(Base.i18n.string("menu.dir.unzip"));
                        unzipItem.setActionCommand(thisFile.getAbsolutePath());
                        unzipItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                findAndUnzipZipFile(e.getActionCommand());
                            }
                        });
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
                                    Base.tryDelete(f);
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

                    JMenuItem infoMenu = new JMenu(Base.i18n.string("menu.file.info"));
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
        outputTty.feed("[32m" + msg);
//        Debug.message(msg);
//
//        if(msg == null) {
//            return;
//        }
//
//        if(!msg.endsWith("\n")) {
//            msg += "\n";
//        }
//
//        output.append(msg, Console.COMMAND);
    }

    public void outputMessageStream(String msg) {
        outputTty.feed("[32m" + msg);
//        output.append(msg, Console.BODY);
    }

    public void outputErrorStream(String msg) {
        outputTty.feed("[31m" + msg);
//        output.append(msg, Console.ERROR);
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
        if (msg == null) return;
        Debug.message(msg);

        if(!msg.endsWith("\n")) {
            msg += "\n";
        }

        console.append(msg, Console.WARNING);
    }

    public void error(String msg) {
        if (msg == null) return;
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
//        output.clear();
        outputTty.feed("[2J[1;1H");
    }

    public void setProgress(int x) {
        statusProgress.setValue(x);
    }

    public void setStatus() {

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

    public boolean closeTab(Component c) {
        return false;
    }

    public boolean closeTab(int tab) throws IOException {
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

                if(option == 0) {
                    if (loadedSketch.isUntitled()) {
                        saveAs();
                    }
                    eb.save();
                }
            }

        }

        editorTabs.remove(tab);
        return true;
    }

    public int openNewTab(File sf) {
        if (sf.isDirectory()) return -1;
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

            EditorBase ed = null;

            if (className.equals("org.uecide.editors.code")) {
                ed = new org.uecide.editors.code(loadedSketch, sf, this);
            } else if (className.equals("org.uecide.editors.markdown")) {
                ed = new org.uecide.editors.markdown(loadedSketch, sf, this); // TODO: Reimplement the markdown editor
            } else if (className.equals("org.uecide.editors.bitmap")) {
                ed = new org.uecide.editors.bitmap(loadedSketch, sf, this);
            } else if (className.equals("org.uecide.editors.object")) {
                ed = new org.uecide.editors.object(loadedSketch, sf, this);
            } else if (className.equals("org.uecide.editors.ardublock")) {
                ed = new org.uecide.editors.ardublock(loadedSketch, sf, this);
            } else if (className.equals("org.uecide.editors.text")) {
                ed = new org.uecide.editors.text(loadedSketch, sf, this);
            }

            if (ed == null) {
                error(Base.i18n.string("err.badfile", sf.getName()));
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                return -1;
            }

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

    public void saveAllTabs() throws IOException {
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

    public boolean closeAllTabs() throws IOException {
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

    public boolean askCloseWindow() throws IOException {

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
                                try {
                                    loadSketch(path);
                                } catch (IOException ex) {
                                    error(ex);
                                }
                            } else {
                                error(Base.i18n.string("err.notfound", path));
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

        JMenu submenu;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.new"), KeyEvent.VK_N, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Base.handleNew();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.open"), KeyEvent.VK_O, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    handleOpenPrompt();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_TOP);

        JMenu recentSketchesMenu = new JMenu(Base.i18n.string("menu.file.recent"));
        fileMenu.add(recentSketchesMenu);

        for(File m : Base.MRUList) {
            JMenuItem recentitem = new ActiveMenuItem(m.getName(), 0, 0, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String path = e.getActionCommand();

                    if(new File(path).exists()) {
                        try {
                            loadSketch(path);
                        } catch (IOException ex) {
                            error(ex);
                        }
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
            for(Map.Entry<File, Integer> m : Base.MCUList.entrySet()) {
                if (m.getValue().equals(hits)) {
                    File mf = m.getKey();
                    JMenuItem recentitem = new ActiveMenuItem(mf.getName(), 0, 0, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String path = e.getActionCommand();

                            if(new File(path).exists()) {
                                try {
                                    loadSketch(path);
                                } catch (IOException ex) {
                                    error(ex);
                                }
                            } else {
                                error(Base.i18n.string("err.notfound", path));
                            }
                        }
                    });

                    recentitem.setToolTipText(mf.getAbsolutePath());
                    recentitem.setActionCommand(mf.getAbsolutePath());
                    frequentSketchesMenu.add(recentitem, 0);
                }
            }
        }


        JMenu examplesMenu = new JMenu(Base.i18n.string("menu.file.examples"));

        JMenuItem emBrowse = new JMenuItem(Base.i18n.string("menu.file.examples.browse"));
        emBrowse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ExampleBrowser(Editor.this);
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

                        examplesMenu.add(top);
                    }
                }
            }
        }

        fileMenu.add(examplesMenu);

        fileMenu.addSeparator();
        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_MID);

        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.close"), KeyEvent.VK_W, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    askCloseWindow();
                    if(Editor.shouldQuit()) {
                        Preferences.save();
                        // Do we want to open a new empty editor?
                        if (Preferences.getBoolean("editor.newonclose")) {
                            Base.handleNew();
                        } else {
                            System.exit(0); 
                        }
                    }
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.save"), KeyEvent.VK_S, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    saveAllTabs();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.saveas"), KeyEvent.VK_S, KeyEvent.SHIFT_MASK, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    saveAs();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));

        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.export"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportSar();
            }
        }));

        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.import"), 0, 0, new ActionListener() {
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

        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.preferences"), KeyEvent.VK_MINUS, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Preferences(Editor.this);
            }
        }));

        addMenuChunk(fileMenu, Plugin.MENU_FILE | Plugin.MENU_BOTTOM);


        fileMenu.add(new ActiveMenuItem(Base.i18n.string("menu.file.quit"), KeyEvent.VK_Q, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if(Editor.closeAllEditors()) {
                        Preferences.save();
                        System.exit(0);
                    }
                } catch (IOException ex) {
                    error(ex);
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

        JMenu sketchFilesMenu = new JMenu("Files");
        sketchMenu.add(sketchFilesMenu);
        JMenu sketchFilesSource = new JMenu("Source");
        sketchFilesMenu.add(sketchFilesSource);
        JMenu sketchFilesHeaders = new JMenu("Headers");
        sketchFilesMenu.add(sketchFilesHeaders);

        File[] flist = loadedSketch.sketchFiles.toArray(new File[0]);

        Arrays.sort(flist);

        for(File f : flist) {
            int type = FileType.getType(f);
    
            JMenuItem item;

            switch(type) {
                case FileType.CSOURCE:
                case FileType.CPPSOURCE:
                case FileType.ASMSOURCE:
                case FileType.SKETCH:
                    item = new JMenuItemWithObject(f.getName(), f);
                    sketchFilesSource.add(item);
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            JMenuItemWithObject m = (JMenuItemWithObject)e.getSource();
                            File f = (File)(m.getObject());
                            openOrSelectFile(f);
                        }
                    });
                    break;
                case FileType.HEADER:
                    item = new JMenuItemWithObject(f.getName(), f);
                    sketchFilesHeaders.add(item);
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            JMenuItemWithObject m = (JMenuItemWithObject)e.getSource();
                            File f = (File)(m.getObject());
                            openOrSelectFile(f);
                        }
                    });
                    break;
            }
        }

        sketchMenu.add(new ActiveMenuItem("Compile", KeyEvent.VK_R, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    compile();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));
        
        sketchMenu.add(new ActiveMenuItem("Compile and Program", KeyEvent.VK_U, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    program();
                } catch (IOException ex) {
                    error(ex);
                }
            }
        }));
        
        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_TOP);
        sketchMenu.addSeparator();

        JMenu createSubmenu = new JMenu(Base.i18n.string("menu.sketch.create"));
        createSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.create.ino"), 0, 0, createNewAction, "ino"));
        createSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.create.cpp"), 0, 0, createNewAction, "cpp"));
        createSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.create.c"), 0, 0, createNewAction, "c"));
        createSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.create.header"), 0, 0, createNewAction, "h"));
        createSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.create.asm"), 0, 0, createNewAction, "S"));
        createSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.create.library"), 0, 0, createNewAction, "lib"));
        sketchMenu.add(createSubmenu);

        JMenu importSubmenu = new JMenu(Base.i18n.string("menu.sketch.import"));
        importSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.sketch.import.source"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        }));
        importSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.sketch.import.header"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        }));
        importSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.sketch.import.binary"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        }));
        sketchMenu.add(importSubmenu);

        JMenu librariesSubmenu = new JMenu(Base.i18n.string("menu.sketch.import.libraries"));
        librariesSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.sketch.import.libraries.install"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                installLibraryArchive();
            }
        }));

        librariesSubmenu.addSeparator();
        populateLibrariesMenu(librariesSubmenu);
        sketchMenu.add(librariesSubmenu);


        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_MID);
        sketchMenu.addSeparator();
        addMenuChunk(sketchMenu, Plugin.MENU_SKETCH | Plugin.MENU_BOTTOM);

        sketchMenu.add(new ActiveMenuItem(Base.i18n.string("menu.sketch.properties"), 0, 0, new ActionListener() {
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
        hardwareMenu.add(boardsSubmenu);

        JMenu coresSubmenu = new JMenu(Base.i18n.string("menu.hardware.cores") + coreName);
        populateCoresMenu(coresSubmenu);
        hardwareMenu.add(coresSubmenu);

        JMenu compilersSubmenu = new JMenu(Base.i18n.string("menu.hardware.compilers") + compilerName);
        populateCompilersMenu(compilersSubmenu);
        hardwareMenu.add(compilersSubmenu);

        optionsMenu = new JMenu(Base.i18n.string("menu.hardware.options"));
        populateOptionsMenu(optionsMenu);
        optionsMenu.setEnabled(optionsMenu.getItemCount() > 0);
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

        hardwareMenu.add(serialPortsMenu);

        programmersSubmenu = new JMenu(Base.i18n.string("menu.hardware.programmers"));
        populateProgrammersMenu(programmersSubmenu);
        programmersSubmenu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                populateProgrammersMenu(programmersSubmenu);
            }
            public void menuCanceled(MenuEvent e) {
            }
            public void menuDeselected(MenuEvent e) {
            }
        });


        hardwareMenu.add(programmersSubmenu);

        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_TOP);
        hardwareMenu.addSeparator();
        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_MID);
        hardwareMenu.addSeparator();
        addMenuChunk(hardwareMenu, Plugin.MENU_HARDWARE | Plugin.MENU_BOTTOM);

        toolsMenu.add(new ActiveMenuItem(Base.i18n.string("menu.tools.pm"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    PluginManager pm = new PluginManager();
                    pm.openWindow(Editor.this);
                } catch (Exception ex) { error(ex); }
            }
        }));

        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_TOP);
        toolsMenu.addSeparator();
        addMenuChunk(toolsMenu, Plugin.MENU_TOOLS | Plugin.MENU_MID);

        toolsMenu.add(new ActiveMenuItem(Base.i18n.string("menu.tools.sm"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ServiceManager.open(Editor.this);
            }
        }));

        toolsMenu.addSeparator();
        toolsMenu.add(new ActiveMenuItem("Toggle minimalist mode", KeyEvent.VK_M, KeyEvent.SHIFT_MASK, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Preferences.setBoolean("editor.layout.minimal", !Preferences.getBoolean("editor.layout.minimal"));
                try {
                    refreshAllEditors();
                } catch (Exception ex) {
                }
            }
        }));

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

        helpMenu.add(new ActiveMenuItem(Base.i18n.string("menu.help.about"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleAbout();
            }
        }));

        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_TOP);
        helpMenu.addSeparator();

        for (String link : Base.webLinks.childKeys()) {
            String name = Base.webLinks.get(link + ".name");
            String url = Base.webLinks.get(link + ".url");
            if ((name != null) && (url != null)) {
                helpMenu.add(new ActiveMenuItem(name, 0, 0, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String link = e.getActionCommand();
                        Utils.browse(link);
                    }
                }, url));
            }
        }

//        PropertyFile links = Base.theme.getChildren("links");
//
//        for(String link : links.childKeys()) {
//            helpMenu.add(new ActiveMenuItem(links.get(link + ".name"), 0, 0, (new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    String link = e.getActionCommand();
//                    Utils.browse(link);
//                }
//            }), links.get(link + ".url")));
//        }

//        links = loadedSketch.getContext().getMerged().getChildren("links");
//
//        for(String link : links.childKeys()) {
//            helpMenu.add(new ActiveMenuItem(links.get(link + ".name"), 0, 0, (new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    String link = e.getActionCommand();
//                    Utils.browse(link);
//                }
//            }), links.get(link + ".url")));
//        }

        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_MID);
        helpMenu.addSeparator();
        addMenuChunk(helpMenu, Plugin.MENU_HELP | Plugin.MENU_BOTTOM);
        JMenu debugSubmenu = new JMenu(Base.i18n.string("menu.help.debug"));

        debugSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.help.debug.console"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.show();
            }
        }));

        debugSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.help.debug.rebuild"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Base.cleanAndScanAllSettings();
            }
        }));

        debugSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.help.debug.purge"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadedSketch.purgeCache();
            }
        }));

        debugSubmenu.add(new ActiveMenuItem(Base.i18n.string("menu.help.debug.opendata"), 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Utils.open(Base.getDataFolder().getAbsolutePath());
            }
        }));

        debugSubmenu.add(new ActiveMenuItem("Prep for screenshot", 0, 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Editor.this.setSize(new Dimension(800, 600));
                sidebarSplit.setSplitSize(0);
                topBottomSplit.setSplitSize(100);
                leftRightSplit.setSplitSize(200);
            }
        }));

        helpMenu.add(debugSubmenu);

    }

    public synchronized void updateOptionsMenu() {
        populateOptionsMenu(optionsMenu);
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
                if (loadedSketch.getOption(opt) != null) {
                    item.setSelected(loadedSketch.getOption(opt).equals(key));
                }
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

        JMenuItem copyOptions = new JMenuItem(Base.i18n.string("menu.options.copy"));
        copyOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                StringBuilder sb = new StringBuilder();

                for (String opt : opts.keySet()) {
                    sb.append("#pragma option ");
                    sb.append(opt);
                    sb.append(" = ");
                    sb.append(loadedSketch.getOption(opt));
                    sb.append("\n");
                }
                StringSelection sel = new StringSelection(sb.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(sel, sel);
                message(Base.i18n.string("menu.options.copy.done"));
            }
        });
        menu.add(copyOptions);
    }

    static class JSerialMenuItem extends JRadioButtonMenuItem {
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
            menu.add(item);
        }


    }

    public String[] getBoardGroups() {
        ArrayList<String> out = new ArrayList<String>();

        for(Board board : Base.boards.values()) {
            String group = board.get("group");

            if (group == null) {
                System.err.println("No group for " + board);
            } else {

                if(out.indexOf(group) == -1) {
                    out.add(group);
                }
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
                if (prog.get("hidden") == null) {
                    progList.add(prog);
                }
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
                    Programmer p = Base.programmers.get(e.getActionCommand());
                    p.onSelected(Editor.this);
                    loadedSketch.setProgrammer(e.getActionCommand());
                }
            });
            item.setActionCommand(prog.getName());
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
            JMenuItem item = new UObjectMenuItem(board);
            boardMenuButtonGroup.add(item);

            if(loadedSketch.getContext().getBoard() != null) {
                if(loadedSketch.getContext().getBoard().equals(board)) {
                    item.setSelected(true);
                }
            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Board b = (Board)((UObjectMenuItem)e.getSource()).getObject();
                    loadedSketch.setBoard(b);
                    b.onSelected(Editor.this);
                }
            });
//            item.setActionCommand(board.getName());
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
            JMenuItem item = new UObjectMenuItem(core);
            coreGroup.add(item);

            if(loadedSketch.getContext().getCore() != null) {
                item.setSelected(loadedSketch.getContext().getCore().equals(core));
            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Core c = (Core)((UObjectMenuItem)e.getSource()).getObject();
                    loadedSketch.setCore(c);
                    c.onSelected(Editor.this);
                }
            });
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
            JMenuItem item = new UObjectMenuItem(compiler);
            compilerGroup.add(item);
            if (loadedSketch.getContext().getCompiler() != null) {
                item.setSelected(loadedSketch.getContext().getCompiler().equals(compiler));
            }
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Compiler c = (Compiler)((UObjectMenuItem)e.getSource()).getObject();
                    loadedSketch.setCompiler(c);
                    c.onSelected(Editor.this);
                }
            });
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

    class UObjectMenuItem extends JRadioButtonMenuItem {
        UObject _object;

        public UObjectMenuItem(UObject o) {
            super(o.getDescription());
            _object = o;

            ImageIcon i = _object.getIcon(16);

            if(i != null) {
                setIcon(i);
            }
        }

        public UObject getObject() {
            return _object;
        }
    }

    class JMenuItemWithFileAndTool extends JMenuItem {
        File file;
        Tool tool;

        public JMenuItemWithFileAndTool(String text, File f, Tool t) {
            super(text);
            file = f;
            tool = t;
        }

        public File getFile() {
            return file;
        }

        public Tool getTool() {
            return tool;
        }
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

                
        if ((filterFlags & (Plugin.MENU_TREE_ID | Plugin.MENU_BOTTOM)) == (Plugin.MENU_TREE_ID | Plugin.MENU_BOTTOM)) {
            Object o = node.getUserObject();
            if (o instanceof File) {
                File f = (File)o;
                String ext = Base.getFileExtension(f);
                for (Tool tool : Base.tools.values()) {
                    String[] entries = tool.getProperties().childKeysOf("sketchtree");
                    if (entries.length > 0) {
                        for (String key : entries) {
                            String reqExt = tool.get("sketchtree." + key + ".extension");
                            if (reqExt == null) continue;
                            if (reqExt.equals(ext)) {
                                JMenuItemWithFileAndTool item = new JMenuItemWithFileAndTool(tool.get("sketchtree." + key + ".name"), f, tool);
                                item.setActionCommand(key);
                                item.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        JMenuItemWithFileAndTool item = (JMenuItemWithFileAndTool)(e.getSource());
                                        Tool t = item.getTool();
                                        File f = item.getFile();
                                        Context ctx = loadedSketch.getContext();
                                        ctx.set("tool.file", f.getAbsolutePath());
                                        String cmd = e.getActionCommand();
                                        t.execute(ctx, "sketchtree." + cmd + ".command");
                                    }
                                });
                                menu.add(item);
                            }
                        }
                    }
                }
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

        for (JSPlugin p : jsplugins) {
            ArrayList<JSAction> ents = p.getMenuActions(filterFlags);
            if (ents != null) {
                for (JSAction act : ents) {
                    JMenuItem item = new JMenuItem(act.tooltip);
                    JSActionListener l = new JSActionListener(this, act);
                    item.addActionListener(l);
                    menu.add(item);
                }
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

        for (JSPlugin p : jsplugins) {
            ArrayList<JSAction> ents = p.getMenuActions(filterFlags);
            if (ents != null) {
                for (JSAction act : ents) {
                    JMenuItem item = new JMenuItem(act.tooltip);
                    JSActionListener l = new JSActionListener(this, act);
                    item.addActionListener(l);
                    menu.add(item);
                }
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

        if (filterFlags == Plugin.TOOLBAR_EDITOR) {
            for (JSPlugin p : jsplugins) {
                
                ArrayList<JSAction> icons = p.getMainToolbarIcons();
                for (JSAction action : icons) {
                    String[] icodat = action.icon.split("/");
                    tb.add(new ToolbarButton(icodat[0], icodat[1], action.tooltip, 24, new JSActionListener(this, action)));
                }
            }
        } else {
            for (JSPlugin p : jsplugins) {
                ArrayList<JSAction> icons = p.getEditorToolbarIcons();
                for (JSAction action : icons) {
                    String[] icodat = action.icon.split("/");
                    tb.add(new ToolbarButton(icodat[0], icodat[1], action.tooltip, 16, new JSActionListener(this, action)));
                }
            }
        }

    }

    public void updateAll() {
        leftRightSplit.recalculateSplit();
        topBottomSplit.recalculateSplit();
        sidebarSplit.recalculateSplit();
        updateMenus();
        updateTree();
        attachPluginTabs();
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
                sarfile.close();
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


                    JComboBox cb = new JComboBox<String>(dests.toArray(new String[0]));
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

    public void saveAs() throws IOException {
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

                Base.tryDelete(newFile);
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

    public void createNewGraphicFile(String extension) {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;

        p.add(new JLabel("Filename:"), c);
        c.gridy++;
        p.add(new JLabel("Width:"), c);
        c.gridy++;
        p.add(new JLabel("Height:"), c);
        c.gridx = 1;
        c.gridy = 0;
        JTextField name = new JTextField(30);
        JTextField width = new JTextField(30);
        JTextField height = new JTextField(30);

        width.setText("64");
        height.setText("64");

        p.add(name, c);
        c.gridy++;
        p.add(width, c);
        c.gridy++;
        p.add(height, c);

        name.requestFocus();

        int rc = JOptionPane.showConfirmDialog(this, p, "New Image", JOptionPane.OK_CANCEL_OPTION);
        if (rc == JOptionPane.OK_OPTION) {
            try {
                int w = Integer.parseInt(width.getText());
                int h = Integer.parseInt(height.getText());
                String n = name.getText();
                BufferedImage i = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                if (!n.endsWith("." + extension)) {
                    n += "." + extension;
                }
                File o = new File(loadedSketch.getBinariesFolder(), n);
                if (extension.equals("png")) {
                    ImageIO.write(i, "PNG", o);
                } else if (extension.equals("jpg")) {
                    ImageIO.write(i, "JPEG", o);
                } else if (extension.equals("gif")) {
                    ImageIO.write(i, "GIF", o);
                }
            } catch (Exception ex) {
                error(ex);
            }

            if (!compilerRunning()) {
                loadedSketch.rescanFileTree();
                updateTree();
            }

        }
    }

    public void createNewSketchFile(String extension) {

        if (extension.equals("png")) { createNewGraphicFile(extension); return; }
        if (extension.equals("jpg")) { createNewGraphicFile(extension); return; }
        if (extension.equals("gif")) { createNewGraphicFile(extension); return; }


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

        String className = FileType.getEditor(name + "." + extension);

        String content = null;
        if(className != null) {
            if (className.equals("org.uecide.editors.code")) {
                content = org.uecide.editors.code.emptyFile();
            } else if (className.equals("org.uecide.editors.bitmap")) {
                content = org.uecide.editors.bitmap.emptyFile();
            } else if (className.equals("org.uecide.editors.object")) {
                content = org.uecide.editors.object.emptyFile();
            } else if (className.equals("org.uecide.editors.markdown")) {
                content = org.uecide.editors.markdown.emptyFile();
            } else if (className.equals("org.uecide.editors.ardublock")) {
                content = org.uecide.editors.ardublock.emptyFile();
            } else if (className.equals("org.uecide.editors.text")) {
                content = org.uecide.editors.text.emptyFile();
            }
        }

        loadedSketch.createNewFile(name + "." + extension, content);
    }

    File importFileDefaultDir = null;

    public void importFile(String type) {
        JFileChooser fc = new JFileChooser();
        javax.swing.filechooser.FileFilter filter;

        if(type.equals("source")) {
            filter = new SourceFileFilter();
            fc.setFileFilter(filter);
        } else if(type.equals("header")) {
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

            if(type.equals("source") || type.equals("header")) {
                dest = new File(loadedSketch.getFolder(), src.getName());
            } else if(type.equals("binary")) {
                dest = new File(loadedSketch.getBinariesFolder(), src.getName());
            } else {
                return;
            }

            File dp = dest.getParentFile();

            if(!dp.exists()) {
                dp.mkdirs();
            }

            try {
                if (src.getCanonicalPath().equals(dest.getCanonicalPath())) {
                    JOptionPane.showMessageDialog(this, 
                        Base.i18n.string("err.samefile", src.getName(), dest.getName()), 
                        Base.i18n.string("err.samefile.title"),
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            } catch (Exception eee) { }

            try {
                Files.copy(src.toPath(), dest.toPath(), REPLACE_EXISTING);
            } catch (IOException ex) {
                error(ex);
            }

            if(type.equals("source") || type.equals("header")) {
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

    public static void broadcastWarning(String msg) {
        for(Editor e : editorList) {
            e.warning(msg);
        }
    }

    public static void broadcastError(String msg) {
        for(Editor e : editorList) {
            e.error(msg);
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

    public static boolean closeAllEditors() throws IOException {
        ArrayList<Editor>localList = new ArrayList<Editor>();
        localList.addAll(editorList);
        for(Editor e : localList) {
            if(e.askCloseWindow() == false) {
                return false;
            }
        }

        return true;
    }

    public void handleOpenPrompt() throws IOException {
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

    public void loadSketch(String f) throws IOException {
        loadSketch(new File(f));
    }

    public void loadSketch(File f) throws IOException {
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
        testConsole.getTermPanel().setAntiAliasing(true);
        testConsole.getTermPanel().setFont(Preferences.getFont("theme.console.fonts.command"));
        consoleScroll.setShadow(
            Preferences.getInteger("theme.console.shadow.top"),
            Preferences.getInteger("theme.console.shadow.bottom")
        );
    }

    public static void releasePorts(String n) {
        for(Editor e : editorList) {
            e.releasePort(n);
        }
    }

    public static void refreshAllEditors() throws IOException {
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

        if (Preferences.getBoolean("editor.layout.minimal")) {
            enableMinimalMode();
        } else {
            disableMinimalMode();
        }

    }

    public void handleAbout() {
        new About(this);
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
                        message(Base.i18n.string("msg.analyzing.install", lib));
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

                    message(Base.i18n.string("msg.analyzing.updating.start"));
                    Base.gatherLibraries();
                    Editor.updateAllEditors();
                    message(Base.i18n.string("msg.analyzing.updating.done"));
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

    static public class LibCatObject {
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

    public void compile() throws IOException {
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

    public void program() throws IOException {
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
                    Base.tryDelete(archiveFile);
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
                goToLineInFile(new File(m.group(2)), line);

            } catch (Exception e) {
            }
        }
    }

    public void goToLineInFile(File f, int l) {
        int tab = openOrSelectFile(f);
        if (tab != -1) {
            EditorBase eb = getTab(tab);
            eb.gotoLine(l);
            eb.requestFocus();
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
        }, 0, 250);

    }

    int spinPos = 1;
    public void tickUpdateBlocker() {
        spinPos++;
        if (spinPos == 5) spinPos = 1;
        String iname = "circle" + spinPos;
        ImageIcon i = Base.getIcon("spinner", iname, 48);
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

    void updateSplits() {
/*
        Dimension windowSize = getSize(null);
        float splitDividerSize = Base.preferences.getFloat("editor.layout.splits.sidebar", 0.9F);
        if (splitDividerSize > 1F || splitDividerSize < 0F) splitDividerSize = 0.9F;
//        sidebarSplit.setDividerLocation((int)(windowSize.width * splitDividerSize));
        sidebarSplit.setResizeWeight(splitDividerSize);

        splitDividerSize = Base.preferences.getFloat("editor.layout.splits.console", 0.7F);
        if (splitDividerSize > 1F || splitDividerSize < 0F) splitDividerSize = 0.7F;
//        topBottomSplit.setDividerLocation((int)(windowSize.height * splitDividerSize));
        topBottomSplit.setResizeWeight(splitDividerSize);

        windowSize = leftRightSplit.getSize(null);

        splitDividerSize = Base.preferences.getFloat("editor.layout.splits.tree", 0.1F);
        if (splitDividerSize > 1F || splitDividerSize < 0F) splitDividerSize = 0.1F;
//        leftRightSplit.setDividerLocation((int)(windowSize.width * splitDividerSize));
        leftRightSplit.setResizeWeight(splitDividerSize);
*/
    }

    public boolean getUpdateFlag() {
        boolean update = false;
        int ntabs = editorTabs.getTabCount();

        for(int i = 0; i < ntabs; i++) {
            EditorBase eb = getTab(i);
            if (eb.getUpdateFlag()) {
                update = true;
            }
        }
        return update;
    }

    public void enableMinimalMode() {
        toolbar.setVisible(false);
        leftRightSplit.hideOne();
        sidebarSplit.hideTwo();
        miniBar.setVisible(true);
    }

    public void disableMinimalMode() {
        toolbar.setVisible(true);
        leftRightSplit.showOne();
        sidebarSplit.showTwo();
        miniBar.setVisible(false);
    }

    public void openImageConversionSettings(String filename) {

        ImageConversionSettings settings = new ImageConversionSettings(this, filename);
        int res = JOptionPane.showConfirmDialog(this, settings, "Image Conversion Settings", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            int selectedConversionOption = settings.getConversionType();
            String prefix = settings.getPrefix();
            Color color = settings.getTransparency();
            String dataType = settings.getDataType();
            int threshold = settings.getThreshold();
            if (prefix.trim().equals("")) {
                JOptionPane.showMessageDialog(this, "You haven't specified a valid variable prefix!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            loadedSketch.set("binary." + filename + ".prefix", prefix);
            loadedSketch.set("binary." + filename + ".transparency", color);
            loadedSketch.set("binary." + filename + ".conversion", selectedConversionOption);
            loadedSketch.set("binary." + filename + ".datatype", dataType);
            loadedSketch.set("binary." + filename + ".threshold", threshold);
        }
    }


    public void openBinaryConversionSettings(String filename) {

        BasicConversionSettings settings = new BasicConversionSettings(this, filename);
        int res = JOptionPane.showConfirmDialog(this, settings, "Binary Conversion Settings", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            int selectedConversionOption = settings.getConversionType();
            String prefix = settings.getPrefix();
            if (prefix.trim().equals("")) {
                JOptionPane.showMessageDialog(this, "You haven't specified a valid variable prefix!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            loadedSketch.set("binary." + filename + ".prefix", prefix);
            loadedSketch.set("binary." + filename + ".conversion", selectedConversionOption);
        }
    }

    public void attachPluginTabs() {
        removePluginTabs(editorTabs);
        removePluginTabs(projectTabs);
        removePluginTabs(sidebarTabs);
        removePluginTabs(consoleTabs);
        attachPluginTabs(editorTabs, Plugin.TABS_EDITOR);
        attachPluginTabs(projectTabs, Plugin.TABS_PROJECT);
        attachPluginTabs(sidebarTabs, Plugin.TABS_SIDEBAR);
        attachPluginTabs(consoleTabs, Plugin.TABS_CONSOLE);
    }

    public void removePluginTabs(JTabbedPane p) {
        ArrayList<JSTab> existingTabs = new ArrayList<JSTab>();
        for (int i = 0; i < p.getTabCount(); i++) {
            Component c = p.getComponentAt(i);
            if (c instanceof JSTab) {
                existingTabs.add((JSTab)c);
            }
        }

        for (JSTab c : existingTabs) {
            p.remove(c);
            c.getPlugin().call("detachFromTab", this.loadedSketch.getContext(), this);
        }
    }

    public void attachPluginTabs(JTabbedPane p, int location) {
        for (JSPlugin plugin : jsplugins) {
            String tabName = (String)plugin.call("getTabName", this.loadedSketch.getContext(), this, new Object[]{location});

            if (tabName != null) {
                JSTab t = new JSTab(plugin);
                plugin.call("attachToTab", this.loadedSketch.getContext(), this, new Object[]{p, t, location});
                p.add(tabName, t);
            }
        }
    }
}

