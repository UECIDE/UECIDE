package org.uecide.builtin;

import org.uecide.Context;
import org.uecide.CommunicationPort;
import org.uecide.Debug;
import org.uecide.SerialCommunicationPort;
import org.uecide.Serial;
import org.uecide.UECIDE;

import java.util.ArrayList;
import java.util.List;

import javax.usb.*;

import org.usb4java.*;

/* Perform various operations on the currently selected serial port
 * 
 * Usage: 
 *     __builtin_port::open[::baud]
 *     __builtin_port::close
 *     __builtin_port::print::message
 *     __builtin_port::println::message
 *     __builtin_port::pulse
 *     __builtin_port::set::{rts|dtr}
 *     __builtin_port::clear::{rts|dtr}
 *     __builtin_port::find::vid::pid
 */

public class port extends BuiltinCommand {
    public port(Context c) { super(c); }

    static CommunicationPort comPort = null;

    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length == 0) {
            throw new BuiltinCommandException("Syntax Error");
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

                                    if (UECIDE.isWindows()) { // Can't do it yet
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
                        throw new BuiltinCommandException("Timeout On Port");
                    }
                    currentList = newList;
                }
            }
        } catch (Exception e) {
            Debug.exception(e);
            throw new BuiltinCommandException(e.getMessage());
        }
        return true;
    }

    public void kill() {
    }


    public ArrayList<UsbDevice> getAllUsbDevices() {
        try {
            UsbServices services = UsbHostManager.getUsbServices();
            UsbHub rootHub = services.getRootUsbHub();
            return getAllUsbDevices(rootHub);
        } catch (Exception e) { 
            Debug.exception(e);
            UECIDE.error(e);
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
            Debug.exception(e);
            UECIDE.error(e);
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
