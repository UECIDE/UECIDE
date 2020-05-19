package org.uecide.gui.none;

import org.uecide.gui.*;
import org.uecide.*;
import org.uecide.actions.*;

import java.util.*;
import java.io.*;

import org.uecide.Compiler;
import org.uecide.Package;

public class NoneGui extends Gui {

    public NoneGui(Context c) {
        super(c);
    }

    @Override
    public void open() {
    }

    @Override
    public void init() {
    }

    @Override
    public void endinit() {
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }

    @Override
    public void close() {
        System.exit(0);
    }

    @Override
    public void streamMessage(String m) {
        System.out.print(m);
    }

    @Override
    public void message(String m) {
        System.out.println(m);
    }

    @Override
    public void streamWarning(String m) {
        if (!UECIDE.isWindows()) {
            System.out.print("[33m" + m + "[0m");
        } else {
            System.out.print(m);
        }
    }

    @Override
    public void warning(String m) {
        if (!UECIDE.isWindows()) {
            System.out.println("[33m" + m + "[0m");
        } else {
            System.out.println(m);
        }
    }

    @Override
    public void error(String m) {
        if (!UECIDE.isWindows()) {
            System.err.println("[31m" + m + "[0m");
        } else {
            System.err.println(m);
        }
    }

    @Override
    public void streamError(String m) {
        if (!UECIDE.isWindows()) {
            System.err.print("[31m" + m + "[0m");
        } else {
            System.err.print(m);
        }
    }

    @Override
    public void error(Throwable m) {
        if (!UECIDE.isWindows()) {
            System.err.print("[31m");
        } 
        m.printStackTrace();
        if (!UECIDE.isWindows()) {
            System.err.print("[0m");
        } 
    }

    @Override
    public void heading(String m) {
        System.out.println(m);
        for (int i = 0; i < m.length(); i++) {
            System.out.print("=");
        }
        System.out.println();
    }

    @Override
    public void command(String m) {
        if (!UECIDE.isWindows()) {
            System.out.println("[32m" + m + "[0m");
        } else {
            System.out.println(m);
        }
    }

    @Override
    public void bullet(String m) {
        System.out.println(" * " + m);
    }

    @Override
    public void bullet2(String m) {
        System.out.println("   * " + m);
    }

    @Override
    public void bullet3(String m) {
        System.out.println("     * " + m);
    }

    @Override
    public void openSplash() {
        System.out.println("UECIDE version " + UECIDE.getVersion());
        System.out.println("(c) 2019 Majenko Technologies");
    }

    @Override
    public void closeSplash() {
        System.out.println();
    }

    @Override
    public void splashMessage(String message, int percent) {
        System.out.println(" * " + message);
    }

    @Override
    public boolean askYesNo(String question) {
        System.out.print(question + " (Y/N) ");
        try {
            int ch = System.in.read();
            if ((ch == 'n') || (ch == 'N')) {
                System.out.println("No");
                return false;
            }
            if ((ch == 'y') || (ch == 'Y')) {
                System.out.println("Yes");
                return true;
            }
        } catch (IOException ex) {
            Debug.exception(ex);
            ctx.error(ex);
        }
        return false;
    }

    @Override
    public int askYesNoCancel(String question) {
        System.out.print(question + " (Y/N/C) ");
        try {
            int ch = System.in.read();
            if ((ch == 'y') || (ch == 'Y')) {
                System.out.println("Yes");
                return 0;
            }
            if ((ch == 'n') || (ch == 'N')) {
                System.out.println("No");
                return 1;
            }
            if ((ch == 'c') || (ch == 'C')) {
                System.out.println("Cancel");
                return 2;
            }
        } catch (IOException ex) {
            Debug.exception(ex);
            ctx.error(ex);
        }
        return -1;
    }

    @Override
    public File askSketchFilename(String question, File location) {
        System.out.print(question + " ");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String fn = r.readLine();
            if (fn == null) return null;
            if (fn.equals("")) return null;
            File outFile = new File(location, fn);
            return outFile;
        } catch (IOException ex) {
            Debug.exception(ex);
            ctx.error(ex);
        }
        return null;
    }

    @Override
    public File askOpenSketch(String question, File location) {
        System.out.print(question + " ");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String fn = r.readLine();
            if (fn == null) return null;
            if (fn.equals("")) return null;
            File outFile = new File(location, fn);
            return outFile;
        } catch (IOException ex) {
            Debug.exception(ex);
            ctx.error(ex);
        }
        return null;
    }

    @Override
    public String askString(String question, String def) {
        System.out.print(question + " ");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String fn = r.readLine();
            return fn;
        } catch (IOException ex) {
            Debug.exception(ex);
            ctx.error(ex);
        }
        return "";
    }

    @Override
    public void openSketchFileEditor(SketchFile f) {
    }

    @Override
    public void closeSketchFileEditor(SketchFile f) {
    }

    @Override
    public void navigateToLine(SketchFile f, Integer l) {
    }

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
                case Message.STREAM_MESSAGE: streamMessage(m.getText()); break;
                case Message.STREAM_WARNING: streamWarning(m.getText()); break;
                case Message.STREAM_ERROR: streamError(m.getText()); break;
            }
        }
    }

    @Override
    public SketchFile getActiveSketchFile() {
        return null;
    }


    @Override
    public String askPassword(String prompt, String def) {
        return askString(prompt, def);
    }

}
