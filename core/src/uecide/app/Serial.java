/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

package uecide.app;

import uecide.app.debug.MessageConsumer;

//import gnu.io.*;
import jssc.*;

import java.io.*;
import java.util.*;

import java.lang.reflect.Method;

public class Serial {

  //PApplet parent;

  // properties can be passed in for default values
  // otherwise defaults to 9600 N81

  // these could be made static, which might be a solution
  // for the classloading problem.. because if code ran again,
  // the static class would have an object that could be closed

  SerialPort port;

  int rate;
  int parity;
  int databits;
  int stopbits;

  // read buffer and streams 

  InputStream input;
  OutputStream output;

  byte buffer[] = new byte[32768];
  int bufferIndex;
  int bufferLast;
  
  MessageConsumer consumer;

    static HashMap<String, Object> allocatedPorts = new HashMap<String, Object>();
    static String[] portList;

    public static SerialPort requestPort(String name, Object ob) {
        Class[] cArg = new Class[1];
        cArg[0] = new String().getClass();

        boolean validPort = false;
        for (String p : portList) {
            if (p.equals(name)) {
                validPort = true;
            }
        }
        if (validPort == false) {
            Base.error("Port not found: " + name);
            return null;
        }


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
            Base.error(e);
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

    static public String[] getPortList() {
        return portList;
    }
}
