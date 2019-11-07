package org.uecide.gui.swing;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

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
        add(compileMenu);

        uploadMenu = new JMenuItem("Compile & Upload");
        uploadMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, defaultModifiers));
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
        add(importMenu);

        librariesMenu = new LibrariesMenu(ctx);
        add(librariesMenu);

    }
}
