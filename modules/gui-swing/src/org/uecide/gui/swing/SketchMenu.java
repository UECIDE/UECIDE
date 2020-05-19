package org.uecide.gui.swing;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Toolkit;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import org.uecide.Context;
import org.uecide.FileType;

public class SketchMenu extends JMenu {
    
    Context ctx;

    JMenu filesMenu;
    
    FileListMenu sourceFiles;
    FileListMenu headerFiles;
    JMenuItem compileMenu;
    JMenuItem uploadMenu;
    JMenu createMenu;
    CreateFileMenu createSketchFile;
    CreateFileMenu createCppFile;
    CreateFileMenu createCFile;
    CreateFileMenu createHeaderFile;
    CreateFileMenu createAssemblyFile;
    JMenuItem importMenu;
    LibrariesMenu librariesMenu;

    public SketchMenu(Context c) {
        super("Sketch");
        ctx = c;

        int defaultModifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        filesMenu = new JMenu("Files");

        sourceFiles = new FileListMenu(ctx, "Source", FileType.GROUP_SOURCE);
        filesMenu.add(sourceFiles);

        headerFiles = new FileListMenu(ctx, "Header", FileType.GROUP_HEADER);
        filesMenu.add(headerFiles);

        add(filesMenu);

        compileMenu = new JMenuItem("Compile");
        compileMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, defaultModifiers));
        compileMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.actionThread("build");
            }
        });
        add(compileMenu);

        uploadMenu = new JMenuItem("Compile & Upload");
        uploadMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, defaultModifiers));
        uploadMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.actionThread("buildandupload");
            }
        });
        add(uploadMenu);

        addSeparator();

        createMenu = new JMenu("Create New");

        createSketchFile = new CreateFileMenu(ctx, "Sketch File (.ino)", "ino");
        createMenu.add(createSketchFile);
        createCppFile = new CreateFileMenu(ctx, "C++ Source (.cpp)", "cpp");
        createMenu.add(createCppFile);
        createCFile = new CreateFileMenu(ctx, "C Source (.c)", "c");
        createMenu.add(createCFile);
        createHeaderFile = new CreateFileMenu(ctx, "Header File (.h)", "h");
        createMenu.add(createHeaderFile);
        createAssemblyFile = new CreateFileMenu(ctx, "Assembler Source (.S)", "S");
        createMenu.add(createAssemblyFile);

        add(createMenu);

        importMenu = new JMenuItem("Import File...");
        importMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importSketchFile();
            }
        });
        add(importMenu);

        librariesMenu = new LibrariesMenu(ctx);
        add(librariesMenu);

    }

    public void importSketchFile() {
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Source code files", "ino", "pde", "cpp", "c", "h", "S", "hpp", "hh"
        );
        fc.setFileFilter(filter);
        int ret = fc.showOpenDialog(stepUpToFrame());
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f.exists()) {
                if (!(f.isDirectory())) {
                    ctx.action("importsource", f);
                }
            }
        }
    }

    public JFrame stepUpToFrame() {
        Component c = (Component)this;
        while ((c != null) && (!(c instanceof JFrame))) {
            c = (Component)c.getParent();
        }
        return (JFrame)c;
    }
}
