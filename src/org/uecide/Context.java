package org.uecide;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.reflect.*;

import javax.script.*;

import org.uecide.builtin.BuiltinCommand;
import org.uecide.varcmd.VariableCommand;



// Simple context container to hold the current context while running scripts and things.
// It is here that all execution, messaging, string parsing, etc should happen.

public class Context {
    Board board = null;
    Core core = null;
    Compiler compiler = null;
    Sketch sketch = null;
    Editor editor = null;
    CommunicationPort port = null;

    ContextListener listener = null;

    StringBuilder buffer = null;

    boolean bufferError = false;

    PropertyFile settings = null;

    Process runningProcess = null;

    PropertyFile savedSettings = null;

    // Make a new empty context.

    public Context() {
        settings = new PropertyFile();
    }

    // At least one of these should be called to configure the context:

    public void setBoard(Board b) { board = b; System.err.println("setBoard(" + board + ")"); }
    public void setCore(Core c) { core = c; System.err.println("setCore(" + core + ")"); }
    public void setCompiler(Compiler c) { compiler = c; System.err.println("setCompiler(" + compiler + ")"); }
    public void setSketch(Sketch s) { sketch = s; }
    public void setEditor(Editor e) { editor = e; }
    public void setDevice(CommunicationPort p) { 
        port = p; 
        if (port != null) {
            set("port", port.getProgrammingPort());
            set("port.base", port.getBaseName());
            set("ip", port.getProgrammingAddress());
        }

    }

    // Getters for all the above.

    public Board getBoard() { return board; }
    public Core getCore() { return core; }
    public Compiler getCompiler() { return compiler; }
    public Sketch getSketch() { return sketch; }
    public Editor getEditor() { return editor; }
    public CommunicationPort getDevice() { return port; }


    // Settings can be manipulated with these:

    public void set(String k, String v) {
        settings.set(k, v);
    }

    public String get(String k) {
        return settings.get(k);
    }

    public void mergeSettings(PropertyFile pf) {
        settings.mergeData(pf);
    }

    // Utility function to merge all the property files together in order.

    public PropertyFile getMerged() {
        PropertyFile pf = new PropertyFile();
        if (compiler != null) { pf.mergeData(compiler.getProperties()); }
        if (core != null) { pf.mergeData(core.getProperties()); }
        if (board != null) { pf.mergeData(board.getProperties()); }
        pf.mergeData(settings);
        return pf;
    }

    // Find a resource by its URI.  A URI is not a normal Java URI but a UECIDE
    // specific one.  It may be any one of:
    //
    // res:/path/to/resource - get a resource as a string
    // file:/path/to/file - get a file from disk as a string
    // compiler:name.of.file - Get an embedded file from the compiler
    // core:name.of.file - Get an embedded file from the core
    // board:name.of.file - Get an embedded file from the board
    // merged:name.of.file - Get am embedded file from the merged properties

    public String getResource(String uri) {
        if (uri.startsWith("res:")) {
            return getResourceAsString(uri.substring(4));
        }
        if (uri.startsWith("file:")) {
            return getFileAsString(uri.substring(5));
        }
        if (uri.startsWith("compiler:")) {
            return compiler.getEmbedded(uri.substring(9));
        }
        if (uri.startsWith("core:")) {
            return core.getEmbedded(uri.substring(5));
        }
        if (uri.startsWith("board:")) {
            return board.getEmbedded(uri.substring(6));
        }
        if (uri.startsWith("merged:")) {
            PropertyFile pf = getMerged();
            return pf.getEmbedded(uri.substring(7));
        }
        return null;
    }

    public String getResourceAsString(String res) {
        String out = "";
        try {
            InputStream from = Context.class.getResourceAsStream(res);
            StringBuilder sb = new StringBuilder();
            String line = null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(from));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            out = sb.toString();

            from.close();
            from = null;
        } catch(Exception e) {
            error(e);
        }
        return out;
    }

    public String getFileAsString(String res) {
        return null;
    }


    // Reporting and messaging functions.

    public void error(Exception e) {
        if (editor != null) {
            editor.error(e);
            return;
        }
        if (sketch != null) {
            sketch.error(e);
            return;
        }
        e.printStackTrace();
    }

    public void error(String e) {
        if (listener != null) {
            listener.contextError(e);
        } else {
            if (!e.endsWith("\n")) { e += "\n"; }
            if (editor != null) {
                editor.error(e);
                return;
            }
            if (sketch != null) {
                sketch.error(e);
                return;
            }
            System.err.print(e);
        }
    }

    public void warning(String e) {
        if (listener != null) {
            listener.contextWarning(e);
        } else {
            if (!e.endsWith("\n")) { e += "\n"; }
            if (editor != null) {
                editor.warning(e);
                return;
            }
            if (sketch != null) {
                sketch.warning(e);
                return;
            }
            System.out.print(e);
        }
    }
        
    public void message(String e) {
        if (listener != null) {
            listener.contextMessage(e);
        } else {
            if (!e.endsWith("\n")) { e += "\n"; }
            if (editor != null) {
                editor.message(e);
                return;
            }
            if (sketch != null) {
                sketch.message(e);
                return;
            }
            System.out.print(e);
        }
    }

    public void link(String e) {
        if (!e.endsWith("\n")) { e += "\n"; }
        if (editor != null) {
            editor.link(e);
            return;
        }
        if (sketch != null) {
            sketch.link(e);
            return;
        }
        System.out.print(e);
    }

    public void command(String e) {
        if (!e.endsWith("\n")) { e += "\n"; }
        if (editor != null) {
            editor.command(e);
            return;
        }
        if (sketch != null) {
            sketch.command(e);
            return;
        }
        System.out.print(e);
    }
        
    public void bullet(String e) {
        if (!e.endsWith("\n")) { e += "\n"; }
        if (editor != null) {
            editor.bullet(e);
            return;
        }
        if (sketch != null) {
            sketch.bullet(e);
            return;
        }
        System.out.print(" * " + e);
    }
        
    public void bullet2(String e) {
        if (!e.endsWith("\n")) { e += "\n"; }
        if (editor != null) {
            editor.bullet2(e);
            return;
        }
        if (sketch != null) {
            sketch.bullet2(e);
            return;
        }
        System.out.print("   + " + e);
    }

    public void heading(String e) {
        if (!e.endsWith("\n")) { e += "\n"; }
        if (editor != null) {
            editor.heading(e);
            return;
        }
        if (sketch != null) {
            sketch.heading(e);
            return;
        }
        System.out.print(e);
        for (int i = 0; i < e.trim().length(); i++) {
            System.out.print("=");
        }
        System.out.println();
    }
        
    public void errorStream(String e) {
        if  (listener != null) {
            listener.contextError(e);
        } else {
            if (editor != null) {
                editor.errorStream(e);
                return;
            }
            if (sketch != null) {
                sketch.errorStream(e);
                return;
            }
            System.err.print(e);
        }
    }

    public void warningStream(String e) {
        if  (listener != null) {
            listener.contextWarning(e);
        } else {
            if (editor != null) {
                editor.warningStream(e);
                return;
            }
            if (sketch != null) {
                sketch.warningStream(e);
                return;
            }
            System.out.print(e);
        }
    }
        
    public void messageStream(String e) {
        if  (listener != null) {
            listener.contextMessage(e);
        } else {
            if (editor != null) {
                editor.messageStream(e);
                return;
            }
            if (sketch != null) {
                sketch.messageStream(e);
                return;
            }
            System.out.print(e);
        }
    }
        


    // Command and script execution

    // Execute a key as a script in whatever way is needed.

    public Object executeKey(String key) {
        PropertyFile props = getMerged();
    
        // If there is a platform specific version of the key then we should switch to that instead.
        key = props.getPlatformSpecificKey(key);

        // If the key is just a plain key and starts with a URI indicator then run it as a javascript file
        if (props.get(key) != null) {
            String data = parseString(props.get(key));
            String[] val = data.split("::");
            if (
                val[0].startsWith("res:") || 
                val[0].startsWith("file:") || 
                val[0].startsWith("compiler:") || 
                val[0].startsWith("core:") || 
                val[0].startsWith("board:") || 
                val[0].startsWith("merged:")
            ) {
                System.err.println("Script key value: " + val[0]);
                String script = getResource(val[0]);
                System.err.println("Script: " + script);
                String function = val[1];
                String[] args = Arrays.copyOfRange(val, 2, val.length);

                return executeJavaScript(script, function, args);
            }
        }

        // If the key has a sub-key of .0 then run it as a UECIDE Script
        if (props.get(key + ".0") != null) {
            return executeUScript(key);
        }

        // Otherwise try and run it as a command (either built in or system).
        if (props.get(key) != null) {
            return executeCommand(parseString(props.get(key)), parseString(props.get(key + ".environment")));
        }

        return false;
    }

    public Object executeJavaScript(String script, String function, Object[] args) {
        if (function == null) {
            return false;
        }
        Object ret = false;
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");

            if (script == null) { return false; }
            if (script.equals("")) { return false; }

            engine.put("ctx", this);
            engine.eval(script);

            Invocable inv = (Invocable)engine;
            if (inv == null) {
                error("Unable to create invocable");
                return false;
            }

            if (Preferences.getBoolean("compiler.verbose_compile")) {
                String argstr = "";
                for (Object o : args) {
                    String s = o.toString();
                    if (!argstr.equals("")) {
                        argstr += ", ";
                    }
                    argstr += s;
                }
                command(function + "(" + argstr + ")");
            }

            if (args == null) {
                ret = inv.invokeFunction(function);
            } else {
                ret = inv.invokeFunction(function, args);
            }

        } catch (Exception e) {
            error(e);
        }

        return ret;
    }


    public Object executeCommand(String command, String env) {
        if(command.startsWith("__builtin_")) {
            return runBuiltinCommand(command);
        } else {
            return runSystemCommand(command, env);
        }
    }


    public Object executeUScript(String key) {
        PropertyFile props = getMerged();
        PropertyFile script = props.getChildren(key);
        ArrayList<String>lines = script.keySet();
        int lineno = 0;

        Object res = false;

        while(script.keyExists(Integer.toString(lineno))) {
            String lk = String.format("%s.%d", key, lineno);
            String linekey = props.keyForOS(lk);
            String ld = props.get(linekey);

            ld = ld.trim();

            if(ld.startsWith("goto::")) {
                ld = parseString(ld);
                String num = ld.substring(6);

                try {
                    lineno = Integer.parseInt(num);
                    continue;
                } catch(Exception e) {
                    error("Syntax error in " + key + " at line " + lineno);
                    error(ld);
                    if (script.keyExists("fail")) {
                        String failKey = String.format("%s.fail", key);
                        executeKey(failKey);
                    }
                    return false;
                }
            }

            if(ld.startsWith("set::")) {
                ld = parseString(ld);
                String param = ld.substring(5);
                int epos = param.indexOf("=");

                if(epos == -1) {
                    error("Syntax error in " + key + " at line " + lineno);
                    error(ld);
                    if (script.keyExists("fail")) {
                        String failKey = String.format("%s.fail", key);
                        res = executeKey(failKey);
                    }
                    return false;
                }

                String kk = param.substring(0, epos);
                String vv = param.substring(epos + 1);
                settings.set(kk, vv);
                lineno++;
                continue;
            }

            if(ld.equals("fail")) {
                if (script.keyExists("fail")) {
                    String failKey = String.format("%s.fail", key);
                    res = executeKey(failKey);
                }
                return false;
            }

            if(ld.equals("end")) {
                if (script.keyExists("end")) {
                    String endKey = String.format("%s.end", key);
                    res = executeKey(endKey);
                }
                return res;
            }

            res = executeKey(lk);

            if (res instanceof Boolean) {
                if((Boolean)res == false) {
                    if (script.keyExists("fail")) {
                        String failKey = String.format("%s.fail", key);
                        res = executeKey(failKey);
                    }
                    return false;
                }
            }

            lineno++;
        }
        if (script.keyExists("end")) {
            String endKey = String.format("%s.end", key);
            res = executeKey(endKey);
        }

        return res;
    }



    public String parseString(String in) {
        PropertyFile tokens = getMerged();
        
        int iStart;
        int iEnd;
        int iTest;
        String out;
        String start;
        String end;
        String mid;

        if(in == null) {
            return null;
        }

        out = in;

        iStart = out.indexOf("${");

        if(iStart == -1) {
            return out;
        }

        iEnd = out.indexOf("}", iStart);
        iTest = out.indexOf("${", iStart + 1);

        while((iTest > -1) && (iTest < iEnd)) {
            iStart = iTest;
            iTest = out.indexOf("${", iStart + 1);
        }

        while(iStart != -1) {
            start = out.substring(0, iStart);
            end = out.substring(iEnd + 1);
            mid = out.substring(iStart + 2, iEnd);

            // Compatability hack for old format roots
            if (mid.equals("board.root")) { mid = "board:root"; }
            if (mid.equals("core.root")) { mid = "core:root"; }
            if (mid.equals("compiler.root")) { mid = "compiler:root"; }

            if(mid.indexOf(":") > -1) {
                String command = mid.substring(0, mid.indexOf(":"));
                String param = mid.substring(mid.indexOf(":") + 1);

                mid = runFunctionVariable(command, param);
            } else {
                String tmid = tokens.get(mid);

                if(tmid == null) {
                    tmid = "";
                }

                mid = tmid;
            }


            if(mid != null) {
                out = start + mid + end;
            } else {
                out = start + end;
            }

            iStart = out.indexOf("${");
            iEnd = out.indexOf("}", iStart);
            iTest = out.indexOf("${", iStart + 1);

            while((iTest > -1) && (iTest < iEnd)) {
                iStart = iTest;
                iTest = out.indexOf("${", iStart + 1);
            }
        }

        // This shouldn't be needed as the methodology should always find any tokens put in
        // by other token replacements.  But just in case, eh?
        if(out != in) {
            out = parseString(out);
        }

        return out;
    }

    public String runFunctionVariable(String command, String param) {
        try {
            Class<?> c = Class.forName("org.uecide.varcmd.vc_" + command);

            if(c == null) {
                return "";
            }

            Constructor<?> ctor = c.getConstructor();
            VariableCommand  p = (VariableCommand)(ctor.newInstance());

            Class[] param_types = new Class<?>[2];
            param_types[0] = org.uecide.Context.class;
            param_types[1] = String.class;
            Method m = c.getMethod("main", param_types);

            if(m == null) {
                return "";
            }

            Object[] args = new Object[2];
            args[0] = this;
            args[1] = param;
            return (String)m.invoke(p, args);
        } catch(Exception e) {
            error(e);
        }

        return "";
    }

    public Object runBuiltinCommand(String commandline) {
        try {
            String[] split = commandline.split("::");
            int argc = split.length - 1;

            String cmdName = split[0];

            String[] arg = new String[argc];

            for(int i = 0; i < argc; i++) {
                arg[i] = split[i + 1];
            }

            if(!cmdName.startsWith("__builtin_")) {
                return false;
            }

            cmdName = cmdName.substring(10);
            if (Preferences.getBoolean("compiler.verbose_compile")) {
                String argstr = "";
                for (String s : arg) {
                    if (!argstr.equals("")) {
                        argstr += " ";
                    }
                    argstr += s;
                }
                command(cmdName + " " + argstr);
            }

            Class<?> c = Class.forName("org.uecide.builtin." + cmdName);

            Constructor<?> ctor = c.getConstructor();
            BuiltinCommand  p = (BuiltinCommand)(ctor.newInstance());

            if(c == null) {
                return false;
            }

            Class<?>[] param_types = new Class<?>[2];
            param_types[0] = org.uecide.Context.class;
            param_types[1] = String[].class;
            Method m = c.getMethod("main", param_types);

            Object[] args = new Object[2];
            args[0] = this;
            args[1] = arg;

            return m.invoke(p, args);


        } catch(Exception e) {
            Base.error(e);
        }

        return false;
    }



    public Object runSystemCommand(String command, String env) {
        PropertyFile props = getMerged();

        Object res;

        if(command == null) {
            return true;
        }

        String[] commandArray = command.split("::");
        ArrayList<String> stringList = new ArrayList<String>();

        for(String string : commandArray) {
            string = string.trim();

            if(string != null && string.length() > 0) {
                stringList.add(string);
            }
        }

        stringList.set(0, stringList.get(0).replace("//", "/"));

        ProcessBuilder process = new ProcessBuilder(stringList);
        runningProcess = null;

        if (env != null) {
            Map<String, String> environment = process.environment();
            for(String ev : env.split("::")) {
                String[] bits = ev.split("=");

                if(bits.length == 2) {
                    environment.put(bits[0], parseString(bits[1]));
                }
            }
        }

        if (props.get("build.path") != null) {
            process.directory(props.getFile("build.path"));
        }

        StringBuilder sb = new StringBuilder();

        for(String component : stringList) {
            sb.append(component);
            sb.append(" ");
        }

        Debug.message("Execute: " + sb.toString());
        if (Preferences.getBoolean("compiler.verbose_compile")) {
            command(sb.toString());
        }

        try {
            runningProcess = process.start();
        } catch(Exception e) {
            error("Unable to start process");
            error(e);
            return false;
        }

        Base.processes.add(runningProcess);

        InputStream in = runningProcess.getInputStream();
        InputStream err = runningProcess.getErrorStream();
        boolean running = true;
        int result = -1;

        byte[] tmp = new byte[1024];


        while(isProcessRunning(runningProcess)) {
            try {
                while(in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);

                    if(i < 0)break;
                    
                    String line = new String(tmp, 0, i);

                    if (buffer != null) {
                        buffer.append(line);
                    } else {                        
                        messageStream(line);
                    }
                }

                while(err.available() > 0) {
                    int i = err.read(tmp, 0, 20);

                    if(i < 0)break;

                    String line = new String(tmp, 0, i);

                    if (bufferError) {
                        if (buffer != null) {
                            buffer.append(line);
                        } else {                        
                            errorStream(line);
                        }
                    } else {
                        errorStream(line);
                    }
                }
                Thread.sleep(1);

            } catch(Exception ignored) {
                Base.error(ignored);
            }
        }

        try {
            while(in.available() > 0) {
                int i = in.read(tmp, 0, 20);

                if(i < 0)break;

                String line = new String(tmp, 0, i);

                if (buffer != null) {
                    buffer.append(line);
                } else {                        
                    messageStream(line);
                }
            }

            while(err.available() > 0) {
                int i = err.read(tmp, 0, 20);

                if(i < 0)break;

                String line = new String(tmp, 0, i);

                if (bufferError) {
                    if (buffer != null) {
                        buffer.append(line);
                    } else {                        
                        errorStream(line);
                    }
                } else {
                    errorStream(line);
                }
            }

        } catch(Exception ignored) {
            if (ignored.getMessage().equals("Stream closed")) {
                error("Cancelled");
            } else {
                error(ignored);
            }
        }

        result = runningProcess.exitValue();

        Base.processes.remove(runningProcess);

        if(result == 0) {
            return true;
        }

        return false;
    }

    public static boolean isProcessRunning(Process process)
    {
        try
        {
            process.exitValue();
            return false;
        }
        catch(IllegalThreadStateException e)
        {
            return true;
        }
    }

    public void killRunningProcess() {
        if(runningProcess != null) {
            runningProcess.destroy();
            Base.processes.remove(runningProcess);
        }
    }

    public void startBuffer() {
        startBuffer(false);
    }

    public void startBuffer(boolean be) {
        bufferError = be;
        buffer = new StringBuilder();
    }

    public String endBuffer() {
        String b = buffer.toString();
        buffer = null;
        return b;
    }

    public void addContextListener(ContextListener l) {
        listener = l;
    }

    public void removeContextListener() {
        listener = null;
    }

    public void debugDump() {
        PropertyFile pf = getMerged();
        pf.debugDump();
    }

    public void snapshot() {
        savedSettings = settings;
        settings = new PropertyFile();
    }

    public void rollback() {
        settings = savedSettings;
    }
}
