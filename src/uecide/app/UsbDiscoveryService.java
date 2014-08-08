package uecide.app;

import java.io.*;
import java.util.*;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

import org.usb4java.*;

import javax.usb.*;
import javax.usb.event.*;

public class UsbDiscoveryService {

    static Thread serviceDiscoveryThread = null;
    static boolean serviceDiscoveryThreadRunning = true;
    static UsbServices services = null;
    static UsbHub rootHub = null;

    static public void startDiscoveringBoards() {
        serviceDiscoveryThread = new Thread() {
            public void run() {
                try {
                    services = UsbHostManager.getUsbServices();
                    rootHub = services.getRootUsbHub();
                    serviceDiscoveryThreadRunning = true;

                    UsbServicesListener listener = new UsbServicesListener() {
                        public void usbDeviceAttached(UsbServicesEvent evt) {
                            UsbDiscoveryService.deviceAttached(evt.getUsbDevice());
                        }
                        public void usbDeviceDetached(UsbServicesEvent evt) {
                            UsbDiscoveryService.deviceDetached(evt.getUsbDevice());
                        }
                    
                    };
                    services.addUsbServicesListener(listener);

                    ArrayList<UsbDevice> devs = getAllUsbDevices(rootHub);

                    for (UsbDevice dev : devs) {
                        deviceAttached(dev);
                    }

                    while (serviceDiscoveryThreadRunning) {
                        Thread.sleep(5000);
                    }
                    services.removeUsbServicesListener(listener);
                } catch (Exception ee) {
                    Base.error(ee);
                }
            }
        };
        serviceDiscoveryThread.start();
    }

    static public void stopDiscoveringBoards() {
        serviceDiscoveryThreadRunning = false;
    }

    public static ArrayList<UsbDevice>  getAllUsbDevices(UsbDevice device) {
        ArrayList<UsbDevice> devs = new ArrayList<UsbDevice>();
        try {
            devs.add(device);

            if(device.isUsbHub()) {
                final UsbHub hub = (UsbHub) device;

                for(UsbDevice child : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
                    ArrayList<UsbDevice> subs = getAllUsbDevices(child);
                    devs.addAll(subs);
                }
            }
        } catch (Exception e) {
            Base.error(e);
        }
        return devs;
    }

    public static void deviceAttached(UsbDevice dev) {
        try {
            Board b = getBoardByDevice(dev);
            if (b != null) {
                System.err.println("Connect: " + b.getDescription());
                DiscoveredBoard db = new DiscoveredBoard();
                db.board = b;
                db.name = String.format("%04x:%04x", dev.getUsbDeviceDescriptor().idVendor(), dev.getUsbDeviceDescriptor().idProduct());
                db.location = dev;
                db.programmer = b.get("usb.programmer");
                db.version = "1";
                db.type = DiscoveredBoard.USB;
                Base.discoveredBoards.put(dev, db);
                
            }
        } catch (Exception exex) {
            Base.error(exex);
        }
    }
    public static void deviceDetached(UsbDevice dev) {
        try {
            DiscoveredBoard b = Base.discoveredBoards.get(dev);
            if (b != null) {
                System.err.println("Disconnect: " + b.board.getDescription());
                Base.discoveredBoards.remove(dev);
            }
        } catch (Exception exex) {
            Base.error(exex);
        }
    }

    public static Board getBoardByDevice(UsbDevice dev) {
        try {
            UsbDeviceDescriptor desc = dev.getUsbDeviceDescriptor();
            short vid = desc.idVendor();
            short pid = desc.idProduct();
            for (Board b : Base.boards.values()) {
                String bvids = b.get("usb.vid");
                if (bvids == null) {
                    continue;
                }
                String bpids = b.get("usb.pid");
                if (bpids == null) {
                    continue;
                }
                short bvid = 0;
                short bpid = 0;
                try {
                    bvid = (short)Integer.parseInt(bvids, 16);
                    bpid = (short)Integer.parseInt(bpids, 16);
                } catch (Exception pie) {
                    continue;
                }
                if (vid == bvid && pid == bpid) {
                    return b;
                }
            }
        } catch (Exception e) {
            Base.error(e);
        }
        return null;
    } 
}
