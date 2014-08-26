package org.uecide;

import java.io.*;
import java.util.*;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

import org.usb4java.*;

import javax.usb.*;
import javax.usb.event.*;

public class UsbDiscoveryService extends Service {

    UsbServices services = null;
    UsbHub rootHub = null;
    UsbServicesListener listener = null;

    public UsbDiscoveryService() {
        setInterval(1000); // Irelevant for this service really.
        setName("USB Device Discovery");
    }

    public void setup() {
        try {
            services = UsbHostManager.getUsbServices();
        } catch (UsbException ex) {
            stop();
            return;
        }
        if (services == null) {
            stop();
            return;
        }

        try {
            rootHub = services.getRootUsbHub();
        } catch (UsbException ex) {
            stop();
            return;
        }
        if (rootHub == null) {
            stop();
            return;
        }

        listener = new UsbServicesListener() {
            public void usbDeviceAttached(UsbServicesEvent evt) {
                deviceAttached(evt.getUsbDevice());
            }
            public void usbDeviceDetached(UsbServicesEvent evt) {
                deviceDetached(evt.getUsbDevice());
            }
        
        };
        services.addUsbServicesListener(listener);

        ArrayList<UsbDevice> devs = (ArrayList<UsbDevice>) getAllUsbDevices(rootHub);

        for (UsbDevice dev : devs) {
            deviceAttached(dev);
        }
    }

    public void loop() {
        // Nothing gets done here
    }

    public void cleanup() {
        if (services != null) {
            services.removeUsbServicesListener(listener);
        }
    }

    public ArrayList<UsbDevice>  getAllUsbDevices(UsbDevice device) {
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
            Base.error(e);
        }
        return devs;
    }

    public void deviceAttached(UsbDevice dev) {
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
    public void deviceDetached(UsbDevice dev) {
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

    public Board getBoardByDevice(UsbDevice dev) {
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
