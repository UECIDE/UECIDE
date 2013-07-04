
/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Target - represents a hardware platform
  Part of the Arduino project - http://www.arduino.cc/

  Copyright (c) 2009 David A. Mellis

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  
  $Id$
*/

package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.Preferences;
import uecide.app.Base;
import uecide.plugin.Plugin;
import uecide.plugin.BasePlugin;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import java.util.regex.*;

import uecide.app.Serial;
import uecide.app.SerialException;
import uecide.app.SerialNotFoundException;


public class Board implements MessageConsumer {
    private String name;
    private String longname;
    private Core core;
    private String group;
    private File folder;
    private boolean valid;
    private boolean runInVerboseMode;
    private Map boardPreferences;

    static Logger logger = Logger.getLogger(Base.class.getName());
  
    public Board(File folder) {
        this.folder = folder;

        File boardFile = new File(folder,"board.txt");
        try {
            valid = false;
            if (boardFile.exists()) {
                boardPreferences = new LinkedHashMap();
                Preferences.load(new FileInputStream(boardFile), boardPreferences);
            }
            this.name = folder.getName();
            this.longname = (String) boardPreferences.get("name");
            this.core = Base.cores.get(boardPreferences.get("build.core"));
            this.group = (String) boardPreferences.get("group");
            if (this.core != null) {
                valid = true;
            }
        } catch (Exception e) {
            System.err.print("Bad board file format: " + folder);
        }
    }

    public void setVerbose(boolean v) {
        runInVerboseMode = v;
    }

    public String getGroup() {
        return group;
    }

    public File getFolder() {
        return folder;
    }

    public Core getCore() {
        return core;
    }
  
    public String getName() { 
        return name; 
    }

    public String getLongName() {
        return longname;
    }

    public boolean isValid() {
        return valid;
    }

    public String get(String k) {
        return (String) boardPreferences.get(k);

    }

    public void set(String k, String d) {
        boardPreferences.put(k, d);
    }

    public String get(String k, String d) {
        if ((String) boardPreferences.get(k) == null) {
            return d;
        }
        return (String) boardPreferences.get(k);
    }

    public void assertDTRRTS(boolean dtr, boolean rts) {
        try {
            Serial serialPort = new Serial();
            serialPort.setDTR(dtr);
            serialPort.setRTS(rts);
            serialPort.dispose();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public boolean upload(String filename) {
        String uploadCommand;

        set("filename", filename);
        set("filename.elf", filename + ".elf");
        set("filename.hex", filename + ".hex");
        set("filename.eep", filename + ".eep");

        boolean isJava = true;
        uploadCommand = get("upload.command.java");
        if (uploadCommand == null) {
            uploadCommand = core.get("upload.command.java");
        }
        if (uploadCommand == null) {
            isJava = false;
            uploadCommand = get("upload.command." + Base.osNameFull());
        }
        if (uploadCommand == null) {
            uploadCommand = get("upload.command." + Base.osName());
        }
        if (uploadCommand == null) {
            uploadCommand = get("upload.command");
        }
        if (uploadCommand == null) {
            uploadCommand = core.get("upload.command." + Base.osNameFull());
        }
        if (uploadCommand == null) {
            uploadCommand = core.get("upload.command." + Base.osName());
        }
        if (uploadCommand == null) {
            uploadCommand = core.get("upload.command");
        }

        if (uploadCommand == null) {
            System.err.println("No upload command defined for board");
            return false;
        }

        if (isJava) {
            Plugin uploader;
            uploader = Base.plugins.get(uploadCommand);
            if (uploader == null) {
                System.err.println("Upload class " + uploadCommand + " not found.");
                return false;
            }
            try {
                if ((uploader.flags() & BasePlugin.LOADER) == 0) {
                    System.err.println(uploadCommand + "is not a valid loader plugin.");
                    return false;
                }
                uploader.run();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        String[] spl;
        spl = parseString(uploadCommand).split("::");

        String executable = spl[0];
        if (Base.isWindows()) {
            executable = executable + ".exe";
        }

        File exeFile = new File(folder, executable);
        File tools;
        if (!exeFile.exists()) {
            tools = new File(folder, "tools");
            exeFile = new File(tools, executable);
        }
        if (!exeFile.exists()) {
            exeFile = new File(core.getFolder(), executable);
        }
        if (!exeFile.exists()) {
            tools = new File(core.getFolder(), "tools");
            exeFile = new File(tools, executable);
        }
        if (!exeFile.exists()) {
            exeFile = new File(executable);
        }
        if (exeFile.exists()) {
            executable = exeFile.getAbsolutePath();
        }

        spl[0] = executable;

        // Parse each word, doing string replacement as needed, trimming it, and
        // generally getting it ready for executing.

        String commandString = executable;
        for (int i = 1; i < spl.length; i++) {
            String tmp = spl[i];
            tmp = tmp.trim();
            if (tmp.length() > 0) {
                commandString += "::" + tmp;
            }
        }

        boolean dtr = false;
        boolean rts = false;
        if (get("upload.dtr", core.get("upload.dtr", "")).equals("yes")) {
            dtr = true;
        }
        if (get("upload.rts", core.get("upload.rts", "")).equals("yes")) {
            rts = true;
        }

        if (get("upload.using", core.get("upload.using", "serial")).equals("serial"))
        {
            if (dtr || rts) {
                assertDTRRTS(dtr, rts);
            }
        }
        boolean res = execAsynchronously(commandString);
        if (get("upload.using", core.get("upload.using", "serial")).equals("serial"))
        {
            if (dtr || rts) {
                assertDTRRTS(false, false);
            }
        }
        return res;
    }

    public void message(String m) {
        message(m, 1);
    }

    public void message(String m, int chan) {
        if (m.trim() != "") {
            if (chan == 2) {
                System.err.print(m);
            } else {
                if (runInVerboseMode) {
                    System.out.print(m);
                }
            }
        }
    }

    public File getLDScript() {
        String fn = get("ldscript", "");
        File found;

        if (fn == null) {
            return null;
        }

        found = new File(folder, fn);
        if (found != null) {
            if (found.exists()) {
                return found;
            }
        }

        found = new File(core.getAPIFolder(), fn);
        if (found != null) {
            if (found.exists()) {
                return found;
            }
        }

        System.err.print("Link script not found: " + fn);

        return null;
    }

    public boolean execAsynchronously(String command) {
        if (command == null) return true;
        String[] commandArray = command.split("::");
        List<String> stringList = new ArrayList<String>();
        Process process;

        for(String string : commandArray) {
            string = string.trim();
            if(string != null && string.length() > 0) {
                stringList.add(string);
            }
        }

        if (runInVerboseMode) {
            System.out.println(command.replace("::"," "));
        }
        commandArray = stringList.toArray(new String[stringList.size()]);

        int result = -1;
        try {
            process = Runtime.getRuntime().exec(commandArray);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }

        MessageSiphon in = new MessageSiphon(process.getInputStream(), this);
        MessageSiphon err = new MessageSiphon(process.getErrorStream(), this);
        in.setChannel(1);
        err.setChannel(2);
        boolean running = true;
        while (running) {
            try {
                if (in.thread != null)
                    in.thread.join();
                if (err.thread != null)
                    err.thread.join();
                result = process.waitFor();
                running = false;
            } catch (InterruptedException ignored) { }
        }

        if (result == 0) {
            return true;
        }
        return false;
    }

    public String parseString(String in)
    {
        int iStart;
        int iEnd;
        int iTest;
        String out;
        String start;
        String end;
        String mid;

        out = in;

        if (out == null) {
            return null;
        }

        iStart = out.indexOf("${");
        if (iStart == -1) {
            return out;
        }

        iEnd = out.indexOf("}", iStart);
        iTest = out.indexOf("${", iStart+1);
        while ((iTest > -1) && (iTest < iEnd)) {
            iStart = iTest;
            iTest = out.indexOf("${", iStart+1);
        }

        while (iStart != -1) {
            start = out.substring(0, iStart);
            end = out.substring(iEnd+1);
            mid = out.substring(iStart+2, iEnd);

            if (mid.equals("core.root")) {
                mid = core.getFolder().getAbsolutePath();
            } else if ((mid.length() > 5) && (mid.substring(0,5).equals("find:"))) {
                String f = mid.substring(5);

                File found;
                found = new File(folder, f);
                if (!found.exists()) {
                    found = new File(core.getAPIFolder(), f);
                }
                if (!found.exists()) {
                    mid = "NOTFOUND";
                } else {
                    mid = found.getAbsolutePath();
                }
            } else if (mid.equals("board.root")) {
                mid = folder.getAbsolutePath();
            } else if (mid.equals("verbose")) {
                if (runInVerboseMode)
                    mid = get("upload.verbose", core.get("upload.verbose", ""));
                else 
                    mid = get("upload.quiet", core.get("upload.quiet", ""));
            } else if (mid.equals("port")) {
                if (Base.isWindows()) 
                    mid = "\\\\.\\" + Preferences.get("serial.port");
                else 
                    mid = Preferences.get("serial.port");
            } else {
                mid = get(mid, core.get(mid, ""));
            }

            out = start + mid + end;
            iStart = out.indexOf("${");
            iEnd = out.indexOf("}", iStart);
            iTest = out.indexOf("${", iStart+1);
            while ((iTest > -1) && (iTest < iEnd)) {
                iStart = iTest;
                iTest = out.indexOf("${", iStart+1);
            }
        }

        // This shouldn't be needed as the methodology should always find any tokens put in
        // by other token replaceements.  But just in case, eh?
        if (out != in) {
            out = parseString(out);
        }

        return out;
    }
}
