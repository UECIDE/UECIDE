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

            add(compileButton);

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

            add(uploadButton);


            add(new ToolbarSpacer());

            newSketchButton = new ToolbarButton(
                "New Sketch", 
                "main.new",
                new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        ctx.action("newSketch");
                    }
                }
            );

            add(newSketchButton);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void contextEventTriggered(String event, Context ctx) {
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
