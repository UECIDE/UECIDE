package org.uecide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.tree.*;
import java.io.*;
import java.util.*;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.modes.*;
import org.fife.ui.rtextarea.*;

public class ExampleBrowser extends JDialog {
    Editor editor;

    JSplitPane leftRight;
    JSplitPane topBottom;

    JScrollPane treeScroll;
    JScrollPane descScroll;
    JScrollPane codeScroll;
    
    JTree exampleTree;
    DefaultMutableTreeNode treeRoot;
    DefaultTreeModel treeModel;

    ExampleSketch selectedExample = null;

    RSyntaxTextArea edPane = new RSyntaxTextArea();
    MarkdownPane readme = new MarkdownPane();

    JButton openSketch = new JButton(Base.i18n.string("toolbar.open"));

    class ExampleSketch {
        File folder;
        String name;
        String codebody;
        String desc;

        public ExampleSketch(File f, File s) {
            folder = f;
            name = f.getName();
            codebody = Base.getFileAsString(s);
            File rm = new File(f, "README.md");
            if (rm.exists()) {
                desc = Base.getFileAsString(rm);
            } else {
                desc = "No Description Available\n========================\n\nSorry.";
            }
        }
        public String toString() {
            return name;
        }
        public void updateDisplay() {
            edPane.setText(codebody);
            readme.setText(desc);
            edPane.setCaretPosition(0);
        }
        public File getFolder() {
            return folder;
        }
    }

    public ExampleBrowser(Editor e) {
        editor = e;
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle(Base.i18n.string("win.example"));
        setLayout(new BorderLayout());

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        add(buttons, BorderLayout.SOUTH);

        buttons.add(openSketch);
        openSketch.setEnabled(false);

        openSketch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedExample != null) {
                    editor.loadSketch(selectedExample.getFolder());
                    closeBrowser();
                }
            }
        });

        treeRoot = new DefaultMutableTreeNode(Base.i18n.string("menu.file.examples"));
        treeModel = new DefaultTreeModel(treeRoot);
        exampleTree = new JTree(treeModel);
        populateTree();

        descScroll = new JScrollPane(readme);
        codeScroll = new JScrollPane(edPane);
        treeScroll = new JScrollPane(exampleTree);

        topBottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT, descScroll, codeScroll);

        leftRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, topBottom);

        topBottom.setDividerLocation(300);
        leftRight.setDividerLocation(300);

        add(leftRight, BorderLayout.CENTER);

        exampleTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                selectNode(me);
            }
        });

        edPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_EXTENDABLE_CPLUSPLUS);
        edPane.setEditable(false);

        setSize(800, 700);
        setLocationRelativeTo(editor);
        setVisible(true);
    }

    public void closeBrowser() {
        dispose();
    }

    public void selectNode(MouseEvent me) {
        try {
            TreePath tp = exampleTree.getPathForLocation(me.getX(), me.getY());
            if (tp != null) {
                Object ob = tp.getLastPathComponent();
                if (ob instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)ob;

                    Object uo = node.getUserObject();
                    if (uo instanceof ExampleSketch) {
                        ExampleSketch es = (ExampleSketch)uo;
                        selectedExample = es;
                        if (es != null) {
                            es.updateDisplay();
                            openSketch.setEnabled(true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void populateTree() {
        treeRoot.removeAllChildren();

        DefaultMutableTreeNode compilers = new DefaultMutableTreeNode(Base.i18n.string("menu.hardware.compilers"));
        for (Compiler c : Base.compilers.values()) {
            File f = c.getExamplesFolder();
            if (f != null && f.exists() && f.isDirectory()) {
                DefaultMutableTreeNode cmp = new DefaultMutableTreeNode(c.getName());
                addExamplesFromFolder(cmp, f);
                if (cmp.getChildCount() > 0) {
                    compilers.add(cmp);
                }
            }
        }


        DefaultMutableTreeNode cores = new DefaultMutableTreeNode(Base.i18n.string("menu.hardware.cores"));
        for (Core c : Base.cores.values()) {
            File f = c.getExamplesFolder();
            DefaultMutableTreeNode cmp = new DefaultMutableTreeNode(c.getName());
            if ((f != null) && f.exists() && f.isDirectory()) {
                addExamplesFromFolder(cmp, f);
                if (cmp.getChildCount() > 0) {
                    cores.add(cmp);
                }
            }

            DefaultMutableTreeNode clib = new DefaultMutableTreeNode(Base.i18n.string("tree.libraries"));
            for (String cat : Library.getLibraryCategories()) {
                DefaultMutableTreeNode libcat = new DefaultMutableTreeNode(Library.getCategoryName(cat));

                TreeSet<Library> libs = Library.getLibraries(cat);
                if (libs != null) {
                    for (Library lib : libs) {
                        if (lib.getCore().equals(c.getName())) {
                            DefaultMutableTreeNode sub = new DefaultMutableTreeNode(lib.getName());
                            File ex = lib.getExamplesFolder();
                            if (ex != null) {
                                addExamplesFromFolder(sub, ex);
                            }
                            if (sub.getChildCount() > 0) {
                                libcat.add(sub);
                            }
                        }
                    }
                }
                if (libcat.getChildCount() > 0) {
                    clib.add(libcat);
                }
            }
            if (clib.getChildCount() > 0) {
                cmp.add(clib);
            }

        }
        DefaultMutableTreeNode boards = new DefaultMutableTreeNode(Base.i18n.string("menu.hardware.boards"));
        for (Board c : Base.boards.values()) {
            File f = c.getExamplesFolder();
            if ((f != null) && f.exists() && f.isDirectory()) {
                DefaultMutableTreeNode cmp = new DefaultMutableTreeNode(c.getName());
                addExamplesFromFolder(cmp, f);
                if (cmp.getChildCount() > 0) {
                    boards.add(cmp);
                }
            }
        }

        DefaultMutableTreeNode libraries = new DefaultMutableTreeNode(Base.i18n.string("tree.libraries"));

        for (String cat : Library.getLibraryCategories()) {
            DefaultMutableTreeNode libcat = new DefaultMutableTreeNode(Library.getCategoryName(cat));
            TreeSet<Library> libs = Library.getLibraries(cat);
            if (libs != null) {
                for (Library lib : libs) {
                    if (lib.getCore().equals("all")) {
                        DefaultMutableTreeNode sub = new DefaultMutableTreeNode(lib.getName());
                        File ex = lib.getExamplesFolder();
                        if (ex != null) {
                            addExamplesFromFolder(sub, ex);
                        }
                        if (sub.getChildCount() > 0) {
                            libcat.add(sub);
                        }
                    }
                }
            }
            if (libcat.getChildCount() > 0) {
                libraries.add(libcat);
            }
        }

        if (compilers.getChildCount() > 0) { treeRoot.add(compilers); }
        if (cores.getChildCount() > 0) { treeRoot.add(cores); }
        if (boards.getChildCount() > 0) { treeRoot.add(boards); }
        if (libraries.getChildCount() > 0) { treeRoot.add(libraries); }

        treeModel.reload();
    }

    public void addExamplesFromFolder(DefaultMutableTreeNode root, File folder) {
        File[] flist = folder.listFiles();
        if (flist == null) return;
        for (File f : flist) {
            String name = f.getName();
            File ino = new File(f, name + ".ino");
            File pde = new File(f, name + ".pde");
            if (ino.exists()) {
                DefaultMutableTreeNode example = new DefaultMutableTreeNode(name);
                example.setUserObject(new ExampleSketch(f, ino));
                root.add(example);
            } else if (pde.exists()) {
                DefaultMutableTreeNode example = new DefaultMutableTreeNode(name);
                example.setUserObject(new ExampleSketch(f, pde));
                root.add(example);
            } else {
                DefaultMutableTreeNode sub = new DefaultMutableTreeNode(name);
                addExamplesFromFolder(sub, f);
                if (sub.getChildCount() > 0) {
                    root.add(sub);
                }
            }
        }
    }
}
