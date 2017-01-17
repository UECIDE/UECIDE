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

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.reflect.*;

import javax.script.*;

import java.util.regex.*;

import org.uecide.builtin.BuiltinCommand;
import org.uecide.varcmd.VariableCommand;



// Simple context container to hold the current context while running scripts and things.
// It is here that all execution, messaging, string parsing, etc should happen.

public class Context {
    Board board = null;
    Core core = null;
    Compiler compiler = null;
    Programmer programmer = null;
    Sketch sketch = null;
    Editor editor = null;
    CommunicationPort port = null;

    ContextListener listener = null;

    StringBuilder buffer = null;

    HashMap<String, String> varcmds = new HashMap<String, String>();

    boolean bufferError = false;

    PropertyFile settings = null;
    PropertyFile sketchSettings = null;

    Process runningProcess = null;
    ThreadRet runningThread = null;

    PropertyFile savedSettings = null;

    DataStreamParser parser = null;

    public boolean silence = false;

    // Make a new empty context.

    public Context() {
        settings = new PropertyFile();
        sketchSettings = new PropertyFile();
        updateSystem();
    }

    class ThreadRet extends Thread {
        public Object retval;
    }

    // At least one of these should be called to configure the context:

    public void setProgrammer(Programmer p) { programmer = p; updateSystem(); }
    public void setBoard(Board b) { board = b; updateSystem(); }
    public void setCore(Core c) { core = c; updateSystem(); }
    public void setCompiler(Compiler c) { compiler = c; updateSystem(); }
    public void setSketch(Sketch s) { sketch = s; updateSystem(); }
    public void setEditor(Editor e) { editor = e; updateSystem(); }
    public void setDevice(CommunicationPort p) { 
        port = p; 
        if (port != null) {
            set("port", port.getProgrammingPort());
            set("port.base", port.getBaseName());
            set("ip", port.getProgrammingAddress());
        }

    }

    public void loadVarCmdsFromDirectory(File vcdir) {
        if (vcdir.exists() && vcdir.isDirectory()) {
            File[] flist = vcdir.listFiles();
            for (File f : flist) {
                if (f.getName().endsWith(".jvc")) {
                    String src = Base.getFileAsString(f);
                    String fn = f.getName();
                    fn = fn.substring(0, fn.length() - 4);
                    varcmds.put(fn, src);
                }
            }
        }
    }

    public synchronized void updateSystem() {
        // Load varcmds:
        varcmds = new HashMap<String, String>();
        loadVarCmdsFromDirectory(new File(Base.getDataFolder(), "usr/share/uecide/system/vc"));

        if (compiler != null && compiler.get("system.varcmd") != null) {
            loadVarCmdsFromDirectory(new File(compiler.getFolder(), compiler.get("system.varcmd")));
        }
        if (core != null && core.get("system.varcmd") != null) {
            loadVarCmdsFromDirectory(new File(core.getFolder(), core.get("system.varcmd")));
        }
        if (board != null && board.get("system.varcmd") != null) {
            loadVarCmdsFromDirectory(new File(board.getFolder(), board.get("system.varcmd")));
        }
        if (programmer != null && programmer.get("system.varcmd") != null) {
            loadVarCmdsFromDirectory(new File(programmer.getFolder(), programmer.get("system.varcmd")));
        }
    }

    // Getters for all the above.

    public Programmer getProgrammer() { return programmer; }
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
        if (programmer != null) { pf.mergeData(programmer.getProperties()); }
        if (compiler != null) { pf.mergeData(compiler.getProperties()); }
        if (core != null) { pf.mergeData(core.getProperties()); }
        if (board != null) { pf.mergeData(board.getProperties()); }
        pf.mergeData(sketchSettings);
        pf.mergeData(settings);
        return pf;
    }

    public String getMerged(String k) {
        PropertyFile pf = new PropertyFile();
        if (programmer != null) { pf.mergeData(programmer.getProperties()); }
        if (compiler != null) { pf.mergeData(compiler.getProperties()); }
        if (core != null) { pf.mergeData(core.getProperties()); }
        if (board != null) { pf.mergeData(board.getProperties()); }
        pf.mergeData(sketchSettings);
        pf.mergeData(settings);
        return pf.get(k);
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
        if (uri.startsWith("programmer:")) {
            return programmer.getEmbedded(uri.substring(6));
        }
        if (uri.startsWith("sketch:")) {
            return sketchSettings.getEmbedded(uri.substring(7));
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

    public void parsedMessage(String e) {
        if (editor != null) {
            editor.parsedMessage(e);
            return;
        }
        if (sketch != null) {
            sketch.parsedMessage(e);
            return;
        }
        printParsed(e);
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

    public void bullet3(String e) {
        if (!e.endsWith("\n")) { e += "\n"; }
        if (editor != null) {
            editor.bullet3(e);
            return;
        }
        if (sketch != null) {
            sketch.bullet3(e);
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
        
    public void rawMessageStream(String e) {
        if (editor != null) {
            editor.outputMessageStream(e);
            return;
        }
        if (sketch != null) {
            sketch.outputMessageStream(e);
            return;
        }
        System.err.print(e);
    }

    public void rawErrorStream(String e) {
        if (editor != null) {
            editor.outputErrorStream(e);
            return;
        }
        if (sketch != null) {
            sketch.outputErrorStream(e);
            return;
        }
        System.err.print(e);
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
        
    public void printParsed(String message) {

        Pattern pat = Pattern.compile("\\{\\\\(\\w+)\\s*(.*)\\}");

        int openBracketLocation = message.indexOf("{\\");

        // No open sequence means there's no parsing to do - just
        // append the text as BODY and leave it at that.
        if (openBracketLocation == -1) {
            System.out.print(message);
            return;
        }

        while (openBracketLocation >= 0) {
            String leftChunk = message.substring(0, openBracketLocation);
            String rightChunk = message.substring(openBracketLocation);
            int closeBracketLocation = rightChunk.indexOf("}");
            if (closeBracketLocation == -1) {
                // Oops - something went wrong! No close bracket!
                System.err.println("Badly formatted message: " + message);
                return;
            }
            String block = rightChunk.substring(0, closeBracketLocation + 1);
            String remainder = rightChunk.substring(closeBracketLocation + 1);

            if (!leftChunk.equals("")) {
                System.out.print(leftChunk);
            }

            Matcher m = pat.matcher(block);
            if (m.find()) {
                String type = m.group(1);
                String text = m.group(2);
                if (type.equals("body")) {
                    System.out.print(text);
                } else if (type.equals("warning")) {
                    System.out.print("[36m" + text + "[0m");
                } else if (type.equals("error")) {
                    System.out.print("[31m" + text + "[0m");
                } else if (type.equals("command")) {
                    System.out.print(text);
                } else if (type.equals("heading")) {
                    System.out.print(text);
                } else if (type.equals("bullet")) {
                    System.out.print(" * " + text);
                } else if (type.equals("bullet2")) {
                    System.out.print("   * " + text);
                } else if (type.equals("link")) {
                    System.out.print(text);
                }
            }

            message = remainder;
            openBracketLocation = message.indexOf("{\\");
        }
        if (!message.equals("")) {
            System.out.print(message);
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
                val[0].startsWith("programmer:") || 
                val[0].startsWith("sketch:") ||
                val[0].startsWith("merged:")
            ) {
                String script = getResource(val[0]);
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
        return executeJavaScript(null, script, function, args);
    }

    public Object executeJavaScript(String filename, String script, String function, Object[] args) {
        if (function == null) {
            return false;
        }
        Object ret = false;
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            if (filename != null) {
                engine.put(ScriptEngine.FILENAME, filename);
            }

            if (script == null) { return false; }
            if (script.equals("")) { return false; }

            engine.put("ctx", this);
            engine.eval(script);

            Invocable inv = (Invocable)engine;
            if (inv == null) {
                error(Base.i18n.string("err.invocable"));
                return false;
            }

            if (Preferences.getBoolean("compiler.verbose_compile") && !silence) {
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
                    error(Base.i18n.string("err.syntax", key, lineno));
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
                    error(Base.i18n.string("err.syntax", key, lineno));
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
        if (varcmds.get(command) != null) {
            Object[] pars = {this, param};
            Object ret = executeJavaScript(command, varcmds.get(command), "main", pars);
            if (ret instanceof Boolean) {
                if ((Boolean)ret == false) {
                    return "ERR";
                } else {
                    return "OK";
                }
            } else {
                return (String)ret;
            }
        }


        try {
            Class<?> c = Class.forName("org.uecide.varcmd.vc_" + command);

            if(c == null) {
                return "";
            }

            Constructor<?> ctor = c.getConstructor();
            VariableCommand  p = (VariableCommand)(ctor.newInstance());

            if (p == null) {
                return "";
            }

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
            try {
                return (String)m.invoke(p, args);
            } catch (Exception e2) {
            }
            return "";
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
            if (Preferences.getBoolean("compiler.verbose_compile") && !silence) {
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
            final BuiltinCommand  p = (BuiltinCommand)(ctor.newInstance());

            if(c == null) {
                return false;
            }

            Class<?>[] param_types = new Class<?>[2];
            param_types[0] = org.uecide.Context.class;
            param_types[1] = String[].class;
            
            final Method m = c.getMethod("main", param_types);
            final Method k = c.getMethod("kill");

            Object[] args = new Object[2];
            args[0] = this;
            args[1] = arg;

            final Object[] aa = args;

            try {
                runningThread = new ThreadRet() {
                    public void run() {
                        try {
                            retval = m.invoke(p, aa);
                        } catch (Exception e3) {
                            retval = "";
                        } 
                    }
                };
                runningThread.start();
                runningThread.join();
                return runningThread.retval;
            } catch (Exception e2) {
                runningThread = null;
                return "";
            }


        } catch(Exception e) {
            Base.error(e);
        }

        runningThread = null;
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

        if (!silence) {
            Debug.message("Execute: " + sb.toString());
        }
        if (Preferences.getBoolean("compiler.verbose_compile") && !silence) {
            command(sb.toString());
        }

        try {
            runningProcess = process.start();
        } catch(Exception e) {
            error(Base.i18n.string("err.process"));
            error(e);
            return false;
        }

        Base.processes.add(runningProcess);

        InputStream in = runningProcess.getInputStream();
        InputStream err = runningProcess.getErrorStream();
        boolean running = true;
        int result = -1;

        byte[] tmp = new byte[1];


        String outline = "";
        String errline = "";

        while(isProcessRunning(runningProcess)) {
            try {
                while(in.available() > 0) {
                    int i = in.read(tmp, 0, 1);

                    if(i < 0)break;
                    
                    String inch = new String(tmp, 0, i);

                    if (parser != null) {
                        if (((inch.charAt(0) >= ' ') && (inch.charAt(0) <= (char)127)) || (inch.charAt(0) == '\n')) {
                            outline += inch;
                        }
                        if (inch.equals("\n")) {
                            rawMessageStream(outline);
                            outline = parser.parseStreamMessage(this, outline);
                            outline = parser.parseStreamError(this, outline);
                            if (buffer != null) {
                                buffer.append(inch);
                            } else {                        
                                messageStream(inch);
                            }
                            outline = "";
                        }
                    } else {
                        if (buffer != null) {
                            buffer.append(inch);
                        } else {                        
                            messageStream(inch);
                        }
                    }
                }

                while(err.available() > 0) {
                    int i = err.read(tmp, 0, 1);

                    if(i < 0)break;
                    
                    String inch = new String(tmp, 0, i);
                    if (parser != null) {
                        if (((inch.charAt(0) >= ' ') && (inch.charAt(0) <= (char)127)) || (inch.charAt(0) == '\n')) {
                            errline += inch;
                        }
                        if (inch.equals("\n")) {
                            rawErrorStream(errline);
                            errline = parser.parseStreamMessage(this, errline);
                            errline = parser.parseStreamError(this, errline);
                            if (buffer != null) {
                                buffer.append(inch);
                            } else {                        
                                errorStream(inch);
                            }
                            errline = "";
                        }
                    } else {
                        if (buffer != null) {
                            buffer.append(inch);
                        } else {                        
                            errorStream(inch);
                        }
                    }
                }

                Thread.sleep(1);

            } catch(Exception ignored) {
                Base.error(ignored);
            }
        }

        try {
            while(in.available() > 0) {
                int i = in.read(tmp, 0, 1);

                if(i < 0)break;
                
                String inch = new String(tmp, 0, i);
                if (((inch.charAt(0) >= ' ') && (inch.charAt(0) <= (char)127)) || (inch.charAt(0) == '\n')) {
                    outline += inch;
                }

                if (inch.equals("\n")) {
                    if (parser != null) {
                        rawMessageStream(outline);
                        outline = parser.parseStreamMessage(this, outline);
                        outline = parser.parseStreamError(this, outline);
                    }
                    if (buffer != null) {
                        buffer.append(outline);
                    } else {                        
                        messageStream(outline);
                    }
                    outline = "";
                }
            }

            if (!outline.equals("")) {
                if (parser != null) {
                    rawMessageStream(outline);
                    outline = parser.parseStreamMessage(this, outline);
                    outline = parser.parseStreamError(this, outline);
                }
                if (buffer != null) {
                    buffer.append(outline);
                } else {                        
                    messageStream(outline);
                }
            }

            while(err.available() > 0) {
                int i = err.read(tmp, 0, 1);

                if(i < 0)break;
                
                String inch = new String(tmp, 0, i);
                if (((inch.charAt(0) >= ' ') && (inch.charAt(0) <= (char)127)) || (inch.charAt(0) == '\n')) {
                    errline += inch;
                }

                if (inch.equals("\n")) {
                    if (parser != null) {
                        rawErrorStream(errline);
                        errline = parser.parseStreamMessage(this, errline);
                        errline = parser.parseStreamError(this, errline);
                    }
                    if (bufferError) {
                        if (buffer != null) {
                            buffer.append(errline);
                        } else {                        
                            errorStream(errline);
                        }
                    } else {
                        errorStream(errline);
                    }
                    errline = "";
                }
            }

            if (!errline.equals("")) {
                if (parser != null) {
                    rawErrorStream(errline);
                    errline = parser.parseStreamMessage(this, errline);
                    errline = parser.parseStreamError(this, errline);
                }
                if (bufferError) {
                    if (buffer != null) {
                        buffer.append(errline);
                    } else {                        
                        errorStream(errline);
                    }
                } else {
                    errorStream(errline);
                }
            }

        } catch(Exception ignored) {
            String igm = ignored.getMessage();
            if (igm != null) {
                if (igm.equals("Stream closed")) {
                    error(Base.i18n.string("misc.cancelled"));
                } else {
                    error(ignored);
                }
            }
        }

        if (runningProcess != null) {
            result = runningProcess.exitValue();
        }

        Base.processes.remove(runningProcess);

        if(result == 0) {
            return true;
        }

        return false;
    }

    public static boolean isProcessRunning(Process process)
    {
        if (process == null) {
            return false;
        }
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
        if (runningThread != null) {
            runningThread.stop();
        }
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

    public void addDataStreamParser(DataStreamParser p) {
        parser = p;
    }

    public void removeDataStreamParser() {
        parser = null;
    }

    public void loadSketchSettings(File pf) {
        if (pf.exists()) {
            sketchSettings = new PropertyFile(pf);
        } else {
            sketchSettings = new PropertyFile();
        }
    }

    public void saveSketchSettings() {
        if (sketch == null) {
            return;
        }
        File f = new File(sketch.getFolder(), "sketch.cfg");
        sketchSettings.save(f);
    }

    public PropertyFile getSketchSettings() {
        return sketchSettings;
    }

    public int getParsedInteger(String k) {
        return getParsedInteger(k, 0);
    }

    public int getParsedInteger(String k, int d) {
        PropertyFile props = getMerged();
        String v = props.get(k);
        if (v == null) return d;
        if (v.equals("")) return d;
        v = parseString(v);
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
        }
        return d;
    }
}
