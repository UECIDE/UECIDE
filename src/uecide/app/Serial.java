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

package uecide.app;

import uecide.app.debug.MessageConsumer;

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

    public static SerialPort requestPort(String name) {
        Editor.releasePorts(name);

        try {
            Thread.sleep(100); // Arduino has this, so I guess we should too.
        } catch (Exception e) {
            Base.error(e);
        }

        try {
            SerialPort nsp = new SerialPort(name);
            if (nsp != null) {
                if (nsp.isOpened()) {
                    Base.error("For some reason the port " + name + " never got released properly.");
                    return null;
                }
                nsp.openPort();
                return nsp;
            }
        } catch (SerialPortException se) {
            if (se.getExceptionType().equals(SerialPortException.TYPE_PORT_NOT_FOUND)) {

                JOptionPane.showMessageDialog(new Frame(), "The port could not be found.\nCheck you have the right port\nselected in the Hardware menu.", "Port not found", JOptionPane.ERROR_MESSAGE);

            } else {
                Base.error(se);
                return null;
            }
        } catch (Exception e) {
            Base.error(e);
            return null;
        }
        Base.error("Something went wrong with the port opening.  There was no exception, but I still didn't open the port.");
        return null;
    }

    public static SerialPort requestPort(String name, int baudRate) {
        SerialPort nsp = requestPort(name);
        if (nsp == null) {
            return null;
        }

        try {
            if(nsp.setParams(baudRate, 8, 1, 0)) {
                return nsp;
            }
        } catch (Exception e) {
            Base.error(e);
        }
        return null;
    }

    public static void updatePortListLinux() {
        ArrayList<String> names = new ArrayList<String>();
        File dev = new File("/dev");
        File[] devs = dev.listFiles();
        for (File devfile : devs) {
            if (devfile.getName().startsWith("ttyACM")) { names.add(devfile.getAbsolutePath()); continue; }
            if (devfile.getName().startsWith("ttyUSB")) { names.add(devfile.getAbsolutePath()); continue; }
            if (devfile.getName().startsWith("ttyAMA")) { names.add(devfile.getAbsolutePath()); continue; }
            if (devfile.getName().startsWith("rfcomm")) { names.add(devfile.getAbsolutePath()); continue; }
        }
        portList = names.toArray(new String[names.size()]);
    }

    public static void updatePortListOSX() {
        ArrayList<String> names = new ArrayList<String>();
        File dev = new File("/dev");
        File[] devs = dev.listFiles();
        for (File devfile : devs) {
            if (devfile.getName().startsWith("tty.serial")) { names.add(devfile.getAbsolutePath()); continue; }
            if (devfile.getName().startsWith("tty.usbserial")) { names.add(devfile.getAbsolutePath()); continue; }
            if (devfile.getName().startsWith("tty.usbmodem")) { names.add(devfile.getAbsolutePath()); continue; }
        }
        portList = names.toArray(new String[names.size()]);
    }

    public static void updatePortList() {

        if (Base.isLinux()) {
            updatePortListLinux();
            return;
        }
        if (Base.isMacOS()) {
            updatePortListOSX();
            return;
        }
        SerialPortList spl = new SerialPortList();
        portList = spl.getPortNames();
    }

    static public ArrayList<String> getPortList() {
        ArrayList<String> pl = new ArrayList<String>();

        for (String p : portList) {
            pl.add(p);
        }

        for (String p : extraPorts) {
            if (pl.indexOf(p) == -1) {
                pl.add(p);
            }
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
        while (pname != null) {
            addExtraPort(pname);
            pnum++;
            pname = Base.preferences.get("serial.ports." + Integer.toString(pnum));
        }
    }
}
