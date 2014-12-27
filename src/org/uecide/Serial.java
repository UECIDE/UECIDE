/*
 * Copyright (c) 2014, Majenko Technologies
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

import org.uecide.debug.MessageConsumer;

//import gnu.io.*;
import jssc.*;

import java.io.*;
import java.util.*;

import java.lang.reflect.Method;

import java.awt.*;
import javax.swing.*;

public class Serial {
    static ArrayList<String> extraPorts = new ArrayList<String>();
    static String[] portList;
    static HashMap<String, SerialPort> serialPorts = new HashMap<String, SerialPort>();

    public static SerialPort requestPort(String name) {

        SerialPort port = serialPorts.get(name);

        if(port == null) {
            updatePortList();
            port = serialPorts.get(name);
        }

        if(port == null) {
            JOptionPane.showMessageDialog(new Frame(), "The port could not be found.\nCheck you have the right port\nselected in the Hardware menu.", "Port not found", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        try {

            Debug.message("Request for port " + name);

            if(port.isOpened()) {
                port.purgePort(1);
                port.purgePort(2);
                port.closePort();
                Debug.message("Purged and closed " + name);
            }

            Editor.releasePorts(name);
            Debug.message("Released " + name + " in all plugins");

            try {
                Thread.sleep(100); // Arduino has this, so I guess we should too.
            } catch(Exception e) {
                Base.error(e);
            }

            port.openPort();
            Debug.message("Re-opened port");

            if(!port.isOpened()) {
                JOptionPane.showMessageDialog(new Frame(), "The port could not be opened.\nCheck you have the right port\nselected in the Hardware menu.", "Port didn't open", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            return port;

        } catch(Exception e) {
            Base.error(e);
        }

        return null;
    }

    public static void closePort(SerialPort p) {
        Debug.message("Request to close port");

        if(p == null)
            return;

        if(!p.isOpened())
            return;

        try {
            Debug.message("Purged port");
            p.purgePort(1);
            p.purgePort(2);
            p.closePort();
            Debug.message("Port closed OK");
        } catch(Exception e) {
            Base.error(e);
        }

    }

    public static SerialPort requestPort(String name, int baudRate) {
        SerialPort nsp = requestPort(name);

        if(nsp == null) {
            return null;
        }

        try {
            if(nsp.setParams(baudRate, 8, 1, 0)) {
                return nsp;
            }
        } catch(Exception e) {
            Base.error(e);
        }

        return null;
    }

    public static ArrayList<String> getPortListLinux() {
        ArrayList<String> names = new ArrayList<String>();
        File dev = new File("/dev");
        File[] devs = dev.listFiles();

        for(File devfile : devs) {
            if(devfile.getName().startsWith("ttyACM")) {
                names.add(devfile.getAbsolutePath());
                continue;
            }

            if(devfile.getName().startsWith("ttyUSB")) {
                names.add(devfile.getAbsolutePath());
                continue;
            }

            if(devfile.getName().startsWith("ttyAMA")) {
                names.add(devfile.getAbsolutePath());
                continue;
            }

            if(devfile.getName().startsWith("rfcomm")) {
                names.add(devfile.getAbsolutePath());
                continue;
            }
        }

        return names;
    }

    public static ArrayList<String> getPortListOSX() {
        ArrayList<String> names = new ArrayList<String>();
        File dev = new File("/dev");
        File[] devs = dev.listFiles();

        for(File devfile : devs) {
            if(devfile.getName().startsWith("tty.serial")) {
                names.add(devfile.getAbsolutePath());
                continue;
            }

            if(devfile.getName().startsWith("tty.usbserial")) {
                names.add(devfile.getAbsolutePath());
                continue;
            }

            if(devfile.getName().startsWith("tty.usbmodem")) {
                names.add(devfile.getAbsolutePath());
                continue;
            }
        }

        return names;
    }

    public static ArrayList<String> getPortListDefault() {
        ArrayList<String> names = new ArrayList<String>();
        SerialPortList spl = new SerialPortList();
        String[] nlist = spl.getPortNames();

        for(String n : nlist) {
            names.add(n);
        }

        return names;
    }

    public static void updatePortList() {
        ArrayList<String>names = null;

        if(Base.isLinux()) {
            names = getPortListLinux();
        } else if(Base.isMacOS()) {
            names = getPortListOSX();
        } else {
            names = getPortListDefault();
        }

        if (Base.isUnix()) {
            for(String p : extraPorts) {
                if(names.indexOf(p) == -1) {
                    File fp = new File(p);
                    if (fp.exists()) {
                        names.add(p);
                        try {
                            String dst = fp.getCanonicalPath();
                            if (!dst.equals(p)) { // Sym link
                                names.remove(dst);
                            }
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        } else {
            for(String p : extraPorts) {
                if(names.indexOf(p) == -1) {
                    names.add(p);
                }
            }
        }

        portList = names.toArray(new String[0]);

        for(String port : names) {
            if(serialPorts.get(port) == null) {
                try {
                    serialPorts.put(port, new SerialPort(port));
                } catch(Exception e) {
                }
            }
        }

        ArrayList<String>toRemove = new ArrayList<String>();

        for(String port : serialPorts.keySet()) {
            if(names.indexOf(port) == -1) {
                toRemove.add(port);
            }
        }

        for(String port : toRemove) {
            serialPorts.remove(port);
        }
    }

    static public ArrayList<String> getPortList() {
        ArrayList<String> pl = new ArrayList<String>();

        for(String p : portList) {
            pl.add(p);
        }

        Collections.sort(pl);
        return pl;
    }

    static public ArrayList<String> getExtraPorts() {
        return extraPorts;
    }

    static public void clearExtraPorts() {
        extraPorts.clear();
    }

    static public void addExtraPort(String port) {
        extraPorts.add(port);
    }

    static public void fillExtraPorts() {
        int pnum = 0;
        clearExtraPorts();
        String pname = Base.preferences.get("serial.ports." + Integer.toString(pnum));

        while(pname != null) {
            addExtraPort(pname);
            pnum++;
            pname = Base.preferences.get("serial.ports." + Integer.toString(pnum));
        }
    }

    static String getNameLinux(SerialPort port) {
        try {
            String pn = port.getPortName();
            File f = new File(pn);
            pn = f.getCanonicalPath();
            pn = pn.substring(pn.lastIndexOf("/") + 1);

            File classFolder = new File("/sys/class/tty", pn);

            if(classFolder == null || !classFolder.exists()) {
                return "";
            }

            File dev = new File(classFolder.getCanonicalPath());

            if(dev.getAbsolutePath().indexOf("/usb") == -1) {
                return "";
            }

            File root = dev;
            File prodFile = new File(root, "product");

            while(!root.getName().startsWith("usb") && !prodFile.exists()) {
                root = root.getParentFile();
                prodFile = new File(root, "product");
            }

            if(!prodFile.exists()) {
                return "";
            }

            File mfgFile = new File(root, "manufacturer");

            if(!mfgFile.exists()) {
                return "";
            }

            BufferedReader reader = new BufferedReader(new FileReader(prodFile));
            String product = reader.readLine();
            reader = new BufferedReader(new FileReader(mfgFile));
            String manufacturer = reader.readLine();

            return manufacturer + " " + product;
            /*

                    File deviceFolder = new File(classFolder, "device");
                    if (deviceFolder == null || !deviceFolder.exists()) {
                        return "";
                    }

                    File uevent = new File(deviceFolder, "uevent");
                    if (uevent == null || !uevent.exists()) {
                        return "";
                    }

                    PropertyFile ueventData = new PropertyFile(uevent);
                    return ueventData.get("PRODUCT");
            */
        } catch(Exception e) {
            Base.error(e);
            return "";
        }
    }

    static String getName(SerialPort port) {
        if(port == null) {
            return "";
        }

        if(Base.isLinux()) {
            return getNameLinux(port);
        }

        return "";
    }

    static String getName(String port) {
        return getName(serialPorts.get(port));
    }
}
