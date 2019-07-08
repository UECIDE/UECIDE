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

//import gnu.io.*;
import com.fazecast.jSerialComm.*;

import java.io.*;
import java.util.*;

import java.lang.reflect.Method;

import java.awt.*;
import javax.swing.*;
import java.lang.management.*;

public class Serial {
    static ArrayList<String> extraPorts = new ArrayList<String>();
    static String[] portList;
    static HashMap<String, SerialPort> serialPorts = new HashMap<String, SerialPort>();

    public static boolean waitLock(String name) {
        if (Base.isLinux()) {
            int timeout = 1000;
            String bn = new File(name).getName();
            File lock = new File("/var/lock/", "LCK.." + bn);
            while (lock.exists()) {
                timeout--;
                if (timeout == 0) {
                    Debug.message("Timeout waiting for lock to clear");
                    return false;
                }
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                }
            }
        }
        return true;
    }

    public static void lockPort(String name) {
        if (Base.isLinux()) {
            String bn = new File(name).getName();
            File lock = new File("/var/lock/", "LCK.." + bn);
            String procName = ManagementFactory.getRuntimeMXBean().getName();
            String[] bits = procName.split("@");
            try {
                PrintWriter pw = new PrintWriter(lock);
                pw.println(bits[0]);
                pw.close();
            } catch (Exception e) {
            }
        }
    }

    public static void unlockPort(String name) {
        if (Base.isLinux()) {
            String bn = new File(name).getName();
            File lock = new File("/var/lock/", "LCK.." + bn);
            if (lock.exists()) {
                lock.delete();
            }
        }
    }

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

            if(port.isOpen()) {
//                port.purgePort(1);
//                port.purgePort(2);
                port.closePort();
                unlockPort(name);
                Debug.message("Purged and closed " + name);
            } else {
                if (!waitLock(name)) {
                    Base.error("Timeout waiting for lock on port");
                    return null;
                }
            }

            Editor.releasePorts(name);
            Debug.message("Released " + name + " in all plugins");

            try {
                Thread.sleep(100); // Arduino has this, so I guess we should too.
            } catch(Exception e) {
                Base.error(e);
            }

            // If we're on linux then check for a lock on the port:


            port.openPort();
            Debug.message("Re-opened port");

            if(!port.isOpen()) {
                JOptionPane.showMessageDialog(new Frame(), "The port could not be opened.\nCheck you have the right port\nselected in the Hardware menu.", "Port didn't open", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            lockPort(name);

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

        if(!p.isOpen())
            return;

        try {
//            p.purgePort(1);
//            p.purgePort(2);
            p.clearDTR();
            p.clearRTS();
            p.closePort();
            unlockPort(p.getSystemPortName());
            Debug.message("Port closed OK");
        } catch(Exception e) {
            Base.error(e);
        }

    }

    public static CommunicationPort getPortByName(String name) {
        for (CommunicationPort cp : Base.communicationPorts) {
            if (cp.toString().equals(name)) {
                return cp;
            }
        }
        return null;
    }
    
    public static SerialPort requestPort(String name, int baudRate) {
        SerialPort nsp = requestPort(name);

        if(nsp == null) {
            return null;
        }

        try {
            nsp.setBaudRate(baudRate);
            nsp.setParity(SerialPort.NO_PARITY);
            nsp.setNumStopBits(SerialPort.ONE_STOP_BIT);
            nsp.setNumDataBits(8);
            return nsp;
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
            if(devfile.getName().startsWith("cu.")) {
                names.add(devfile.getAbsolutePath());
                continue;
            }

//            if(devfile.getName().startsWith("tty.serial")) {
//                names.add(devfile.getAbsolutePath());
//                continue;
//            }

//            if(devfile.getName().startsWith("tty.usbserial")) {
//                names.add(devfile.getAbsolutePath());
//                continue;
//            }

//            if(devfile.getName().startsWith("tty.usbmodem")) {
//                names.add(devfile.getAbsolutePath());
//                continue;
//            }
        }

        return names;
    }

    public static ArrayList<String> getPortListDefault() {
        ArrayList<String> names = new ArrayList<String>();
        SerialPort[] spl = SerialPort.getCommPorts();

        for(SerialPort p : spl) {
            names.add(p.getSystemPortName());
        }

        return names;
    }

    public static void updatePortList() {
        ArrayList<String>names = null;

        fillExtraPorts();

        if(Base.isLinux()) {
            names = getPortListLinux();
        } else if(Base.isMacOS()) {
            names = getPortListOSX();
        } else {
            names = getPortListDefault();
        }

        ArrayList<CommunicationPort> toAdd = new ArrayList<CommunicationPort>();
        ArrayList<CommunicationPort> toRemove = new ArrayList<CommunicationPort>();

        names.addAll(extraPorts);

        for (CommunicationPort port : Base.communicationPorts) {
            if (port instanceof SerialCommunicationPort) {
                String name = port.toString();
                if (names.indexOf(name) == -1) {
                    toRemove.add(port);
                }
            }
        }

        for (String name : names) {
            boolean found = false;
            for (CommunicationPort port : Base.communicationPorts) {
                if (port instanceof SerialCommunicationPort) {
                    String pname = port.toString();
                    if (pname.equals(name)) {
                        found = true;
                    }
                }
            }
            if (found == false) {
                toAdd.add(new SerialCommunicationPort(name));
            }
        }

        for (CommunicationPort port : toRemove) {
            Base.communicationPorts.remove(port);
        }

        for (CommunicationPort port : toAdd) {
            Base.communicationPorts.add(port);
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
                    serialPorts.put(port, SerialPort.getCommPort(port));
                } catch(Exception e) {
                }
            }
        }

        ArrayList<String>toRemoveb = new ArrayList<String>();

        for(String port : serialPorts.keySet()) {
            if(names.indexOf(port) == -1) {
                toRemoveb.add(port);
            }
        }

        for(String port : toRemoveb) {
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

        PropertyFile sub = Base.preferences.getChildren("editor.serial.port");
        for (String k : sub.keySet()) {
            String pname = sub.get(k);
            if (Base.isPosix()) {
                File f = new File(pname);
                if (f.exists()) {
                    addExtraPort(pname);
                } 
            } else {
                addExtraPort(pname);
            }
        }
    }

    static String getNameLinux(SerialPort port) {
        try {
            String pn = port.getSystemPortName();
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
            reader.close();
            reader = new BufferedReader(new FileReader(mfgFile));
            String manufacturer = reader.readLine();
            reader.close();

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

        return port.getPortDescription();

    }

    static String getName(String port) {
        return getName(serialPorts.get(port));
    }
}
