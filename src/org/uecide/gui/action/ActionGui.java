package org.uecide.gui.action;

import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.Debug;
import org.uecide.Message;
import org.uecide.SketchFile;
import org.uecide.gui.Gui;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ActionGui extends Gui implements ContextEventListener {

    public ActionGui(Context c) {
        super(c);
        c.listenForEvent("message", this);
    }

    public void open() {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

            String line = null;
            while ((line = r.readLine()) != null) {

                line = line.trim();
                if (line.equals("")) continue;

                String[] parts = line.split(" ");
                String[] args = new String[parts.length - 1];
                String command = parts[0];

                for (int i = 1; i < parts.length; i++) {
                    args[i - 1] = parts[i];
                }

                ctx.action(command, (Object[])args);
            }

        } catch (Exception e) {
            Debug.exception(e);
            ctx.error(e.getMessage());
        }
    };


    public void close() {
        System.exit(0);
    };

    public void streamMessage(String m) {
        System.out.print(m);
    };
    public void streamWarning(String m) {
        System.out.print(m);
    };
    public void streamError(String m) {
        System.err.print(m);
    };
    public void message(String m) {
        System.out.println(m);
    };
    public void warning(String m) {
        System.out.println(m);
    };
    public void error(String m) {
        System.err.println(m);
    };
    public void error(Throwable m) {
        m.printStackTrace();
    };
    public void heading(String m) {
        System.out.println(m);
    };
    public void command(String m) {
        System.out.println(m);
    };
     public void bullet(String m) {
        System.out.println(m);
    };
    public void bullet2(String m) {
        System.out.println(m);
    };
    public void bullet3(String m) {
        System.out.println(m);
    };
    public void openSplash() {};
    public void closeSplash() {};
    public void splashMessage(String message, int percent) {};
    public void openSketchFileEditor(SketchFile f) {};
    public void closeSketchFileEditor(SketchFile f) {};
    public String askString(String question, String defaultValue) { return null; };
    public File askSketchFilename(String question, File location) { return null; };
    public boolean askYesNo(String question) { return false; };
    public int askYesNoCancel(String question) { return 2; };
    public File askOpenSketch(String question, File location) { return null; };
    public void navigateToLine(SketchFile f, Integer lineno) {};

    public static void init() {
    }

    public static void endinit() {
    }

    @Override
    public void contextEventTriggered(ContextEvent e) {
        if (e.getEvent().equals("message")) {
            Message m = (Message)e.getObject();

            switch (m.getMessageType()) {
                case Message.HEADING: heading(m.getText()); break;
                case Message.BULLET1: bullet(m.getText()); break;
                case Message.BULLET2: bullet2(m.getText()); break;
                case Message.BULLET3: bullet3(m.getText()); break;
                case Message.COMMAND: command(m.getText()); break;
                case Message.NORMAL: message(m.getText()); break;
                case Message.WARNING: warning(m.getText()); break;
                case Message.ERROR: error(m.getText()); break;
            }
        }
    }

}
