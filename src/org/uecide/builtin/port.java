/*
 * Copyright (c) 2016, Majenko Technologies
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

package org.uecide.builtin;

import org.uecide.*;
import java.io.*;
import java.util.*;

import org.usb4java.*;

import javax.usb.*;
import javax.usb.event.*;


public class port implements BuiltinCommand {
    static CommunicationPort comPort = null;

    public boolean main(org.uecide.Context ctx, String[] arg) {
        if (arg.length == 0) {
            return false;
        }
        try {
            String action = arg[0];
            if (action.equals("open")) {


                if (comPort != null) {
                    comPort.closePort();
                }
                comPort = ctx.getDevice();
                comPort.openPort();
                if (arg.length >= 2) {
                    int speed = Integer.parseInt(arg[1]);
                    comPort.setSpeed(speed);
                }
                return true;
            } else if (action.equals("close")) {
                if (comPort != null) {
                    comPort.closePort();
                    comPort = null;
                }
                return true;
            } else if (action.equals("print")) {
                if (comPort != null) {
                    comPort.print(arg[1]);
                }
                return true;
            } else if (action.equals("println")) {
                if (comPort != null) {
                    comPort.println(arg[1]);
                }
                return true;
            } else if (action.equals("pulse")) {
                if (comPort != null) {
                    comPort.pulseLine();
                }
                return true;
            } else if (action.equals("set")) {
                if (arg[1].equals("dtr")) {
                    if (comPort != null) {
                        if (comPort instanceof SerialCommunicationPort) {
                            SerialCommunicationPort sport = (SerialCommunicationPort)comPort;
                            sport.setDTR(true);
                        }
                    }
                } else if (arg[1].equals("rts")) {
                    if (comPort != null) {
                        if (comPort instanceof SerialCommunicationPort) {
                            SerialCommunicationPort sport = (SerialCommunicationPort)comPort;
                            sport.setRTS(true);
                        }
                    }
                }
                return true;
            } else if (action.equals("clear")) {
                if (arg[1].equals("dtr")) {
                    if (comPort != null) {
                        if (comPort instanceof SerialCommunicationPort) {
                            SerialCommunicationPort sport = (SerialCommunicationPort)comPort;
                            sport.setDTR(false);
                        }
                    }
                } else if (arg[1].equals("rts")) {
                    if (comPort != null) {
                        if (comPort instanceof SerialCommunicationPort) {
                            SerialCommunicationPort sport = (SerialCommunicationPort)comPort;
                            sport.setRTS(false);
                        }
                    }
                }
                return true;
            } else if (action.equals("find")) {
                // Find a new USB COM port that has the specified
                // VID/PID pair. The port should not exist when the
                // command is called, and the command will block
                // until the port appears, or it times out.
                //
                // port::find::403::a662

                int vid = Integer.parseInt(arg[1], 16);
                int pid = Integer.parseInt(arg[2], 16);

                ArrayList<String> currentList = Serial.getPortList();

                long start = System.currentTimeMillis();
                while (true) {
                    Serial.updatePortList();
                    Thread.sleep(100);
                    ArrayList<String> newList = Serial.getPortList();
                    for (String port : newList) {
                        if (currentList.indexOf(port) < 0) {
                            CommunicationPort cp = Serial.getPortByName(port);
                            if (cp != null) {
                                if (cp instanceof SerialCommunicationPort) {
                                    SerialCommunicationPort sp = (SerialCommunicationPort)cp;
                                    int newvid = sp.getVID();
                                    int newpid = sp.getPID();

                                    if (Base.isWindows()) { // Can't do it yet
                                        ctx.set("port.found", port);
                                        return true;
                                    }

                                    if ((newvid == vid) && (newpid == pid)) {
                                        ctx.set("port.found", port);
                                        return true;
                                    }

                                }
                            }
                        }
                    }

                    if (System.currentTimeMillis() - start > 10000) {
                        ctx.error("Timeout looking for port");
                        return false;
                    }
                    currentList = newList;
                }
            }
        } catch (Exception e) {
            Base.exception(e);
            Base.error(e);
        }
        return false;
    }

    public void kill() {
    }


    public ArrayList<UsbDevice> getAllUsbDevices() {
        try {
            UsbServices services = UsbHostManager.getUsbServices();
            UsbHub rootHub = services.getRootUsbHub();
            return getAllUsbDevices(rootHub);
        } catch (Exception e) { 
            Base.exception(e);
            Base.error(e);
        }
        return null;
    }

    // Recurses
    public ArrayList<UsbDevice> getAllUsbDevices(UsbDevice device) {
        ArrayList<UsbDevice> devs = new ArrayList<UsbDevice>();
        try {
            devs.add(device);

            if(device.isUsbHub()) {
                final UsbHub hub = (UsbHub) device;

                @SuppressWarnings("unchecked")
                List<UsbDevice>devlist = (List<UsbDevice>)hub.getAttachedUsbDevices();

                for(UsbDevice child : devlist) {
                    ArrayList<UsbDevice> subs = (ArrayList<UsbDevice>) getAllUsbDevices(child);
                    devs.addAll(subs);
                }
            }
        } catch (Exception e) {
            Base.exception(e);
            Base.error(e);
        }
        return devs;
    }

    public UsbDevice getDeviceByVidPid(int vid, int pid) {
        ArrayList<UsbDevice> devs = getAllUsbDevices();

        for (UsbDevice dev : devs) {
            UsbDeviceDescriptor desc = dev.getUsbDeviceDescriptor();
            if ((desc.idVendor() == vid) && (desc.idProduct() == pid)) {
                return dev;
            }
        }
        return null;
    }
        
}
