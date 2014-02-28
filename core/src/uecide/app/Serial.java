/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

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
                System.err.println("Set to " + baudRate + ",8N1");
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
        System.err.println("Closing port " + pn);
        try {
            if (port.closePort()) {
                allocatedPorts.remove(pn);
                System.err.println("Port closed OK");
                return true;
            }
        } catch (Exception e) {
            Base.error(e);
        }
        System.err.println("Close port failed");
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
