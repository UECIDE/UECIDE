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

public class Serial {
    static ArrayList<String> extraPorts = new ArrayList<String>();
    static HashMap<String, Object> allocatedPorts = new HashMap<String, Object>();
    static String[] portList;

    public static SerialPort requestPort(String name, Object ob) {
        Class[] cArg = new Class[1];
        cArg[0] = new String().getClass();

        boolean validPort = false;

        if (allocatedPorts.get(name) != null) {
            try {
                Boolean released = false;
                Method m = null;
                try {
                    m = ob.getClass().getMethod("releasePort", cArg);
                } catch (Exception ee) {
                }
                if (m != null) {
                    released = (Boolean)m.invoke(ob, name);
                }
                if (released == true) {
                    allocatedPorts.remove(name);
                    Thread.sleep(10);
                    System.gc();
                }
            } catch (Exception ex) {
                Base.error(ex);
            }
        }

        try {
            SerialPort nsp = new SerialPort(name);
            if (nsp != null) {
                allocatedPorts.put(name, ob); 
                nsp.openPort();
                return nsp;
            }
        } catch (Exception e) {
            //Base.error(e);
        }
        return null;
    }

    public static SerialPort requestPort(String name, Object ob, int baudRate) {
        SerialPort nsp = requestPort(name, ob);
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

    public static void updatePortList() {
        SerialPortList spl = new SerialPortList();
        portList = spl.getPortNames();
    }

    public static boolean releasePort(SerialPort port) {
        String pn = port.getPortName();
        try {
            if (port.closePort()) {
                allocatedPorts.remove(pn);
                return true;
            }
        } catch (Exception e) {
            Base.error(e);
        }
        return false;
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
