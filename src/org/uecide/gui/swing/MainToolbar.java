package org.uecide.gui.swing;

import org.uecide.*;
import org.uecide.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class MainToolbar extends JToolBar implements ContextEventListener {

    SwingGui gui;
    Context ctx;

    ToolbarToggleButton compileButton;
    ToolbarToggleButton uploadButton;

    ToolbarButton newSketchButton;
    ToolbarButton openSketchButton;
    ToolbarButton saveSketchButton;

    public MainToolbar(SwingGui g) {
        super();
        gui = g;
        ctx = g.getContext();

        ctx.listenForEvent("buildStart", this);
        ctx.listenForEvent("buildFail", this);
        ctx.listenForEvent("buildFinished", this);
        ctx.listenForEvent("uploadStart", this);
        ctx.listenForEvent("uploadFail", this);
        ctx.listenForEvent("uploadFinished", this);

        ctx.listenForEvent("setBoard", this);
        ctx.listenForEvent("setCore", this);
        ctx.listenForEvent("setCompiler", this);
        ctx.listenForEvent("setProgrammer", this);

        setFloatable(false);
        setBorderPainted(false);

        try {
            compileButton = new ToolbarToggleButton(
                "Compile the sketch (Shift: clean compile)",
                "main.compile", "main.spin",
                new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        if ((ev.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) {
                            ctx.action("purge");
                        }
                        ctx.actionThread("build");
                    }
                },
                new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        ctx.action("abort", "build");
                        unlockAll();
                    }
                }
            );

            uploadButton = new ToolbarToggleButton(
                "Compile and upload the sketch (Shift: clean compile)",
                "main.program", "main.spin",
                new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        if ((ev.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) {
                            ctx.action("purge");
                        }
                        ctx.actionThread("buildAndUpload");
                    }
                },
                new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        ctx.action("abort", "buildAndUpload");
                        unlockAll();
                    }
                }
            );

            newSketchButton = new ActionToolbarButton(ctx, "New Sketch", "main.new", "newSketch");
            openSketchButton = new ActionToolbarButton(ctx, "Open Sketch", "main.open", "openSketch");
            saveSketchButton = new ActionToolbarButton(ctx, "Save Sketch", "main.save", "saveSketch");

            updateIcons();

        } catch (IOException ex) {
            Debug.exception(ex);
            ex.printStackTrace();
        }

    }

    public void updateIcons() {
        removeAll();

        add(compileButton);
        add(uploadButton);
        add(new ToolbarSpacer());
        add(newSketchButton);
        add(openSketchButton);
        add(saveSketchButton);
        add(new ToolbarSpacer());
        addUObjectIcons();

        revalidate();
        repaint();
    }

    public void addUObjectIcons() {
        PropertyFile props = ctx.getMerged();
        String[] scripts = props.childKeysOf("script");
        for (String script : scripts) {
            String name = props.get("script." + script + ".name");
            String icon = props.get("script." + script + ".icon");
            if (name == null) continue;
            if (icon == null) continue;

            ActionToolbarButton button = new ActionToolbarButton(ctx, name, icon, "runKey", "script." + script);
            add(button);
        }
        for (Tool t : Tool.getTools().values()) {
            if (t.worksWith(ctx.getCore())) {
                scripts = t.getScripts();
                for (String s : scripts) {
                    if (t.get("tool." + s + ".icon") != null) {
                        ToolScriptToolbarButton button = new ToolScriptToolbarButton(ctx, t, t.get("tool." + s + ".name"), t.get("tool." + s + ".icon"), s);
                        add(button);
                    }
                }
            }
        }
    }

    public void contextEventTriggered(ContextEvent e) {
        String event = e.getEvent();
        if (event.equals("buildFail") || event.equals("buildFinished") || event.equals("uploadFail") || event.equals("uploadFinished")) {
            compileButton.setSelected(false);
            uploadButton.setSelected(false);
            unlockAll();
        }

        if (event.equals("buildStart")) {
            uploadButton.setSelected(false);
            compileButton.setSelected(true);
            lockAll();
            compileButton.setEnabled(true);
        }

        if (event.equals("uploadStart")) {
            uploadButton.setSelected(true);
            compileButton.setSelected(false);
            lockAll();
            uploadButton.setEnabled(true);
        }

        if (event.equals("setBoard") || event.equals("setCore") || event.equals("setCompiler") || event.equals("setProgrammer")) {
            updateIcons();
        }
    }

    public void unlockAll() {
        compileButton.setEnabled(true);
        uploadButton.setEnabled(true);
    }

    public void lockAll() {
        compileButton.setEnabled(false);
        uploadButton.setEnabled(false);
    }
}
