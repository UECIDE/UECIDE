package org.uecide;

import java.lang.*;
import java.io.*;
import java.util.*;
import javax.script.*;

public class JSPlugin {

    String program;
    ScriptEngine engine;
    String progname;

    public JSPlugin(File f) {
        program = Base.getFileAsString(f);
        progname = f.getName();
        initEngine();
    }

    public JSPlugin(String s) {
        program = s;
        progname = "stdin";
        initEngine();
    }

    void initEngine() {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName("JavaScript");
            engine.put(ScriptEngine.FILENAME, progname);

            engine.put("plugin", this);
            engine.put("engine", engine.getFactory().getNames());
            engine.eval(program);
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public Object call(String function) {
        return call(function, null, null, null);
    }

    public Object call(String function, Context ctx) {
        return call(function, ctx, null, null);
    }

    public Object call(String function, Context ctx, Editor ed) {
        return call(function, ctx, ed, null);
    }

    public Object call(String function, Context ctx, Editor ed, Object[] args) {
        Object ret = null;

        try {
            engine.put("context", ctx);
            engine.put("editor", ed);

            Invocable inv = (Invocable)engine;

            if (args == null) {
                ret = inv.invokeFunction(function);
            } else {
                ret = inv.invokeFunction(function, args);
            }
        } catch (NoSuchMethodException ee) {
        
        } catch (Exception e) {
            if (ctx != null) {
                ctx.error(e);
            } else {
                Base.error(e);
            }
        }

        return ret;
    }

    public String getVersion() {
        return (String)call("getVersion");
    }

    public Object onBoot() {
        return call("onBoot");
    }

    public ArrayList<JSAction> getMainToolbarIcons() {
        ArrayList<JSAction> icons = new ArrayList<JSAction>();
        Object[] args = { this, icons };
        
        call("getMainToolbarIcons", null, null, args);

        return icons;
    }

    public ArrayList<JSAction> getEditorToolbarIcons() {
        ArrayList<JSAction> icons = new ArrayList<JSAction>();
        Object[] args = { this, icons };
        
        call("getEditorToolbarIcons", null, null, args);

        return icons;
    }

    public ArrayList<JSAction> getMenuActions(int menu) {
        ArrayList<JSAction> icons = new ArrayList<JSAction>();
        Object[] args = { this, menu, icons };
        
        call("getMenuActions", null, null, args);

        return icons;
    } 

}
