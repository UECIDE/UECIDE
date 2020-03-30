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
        setInterval(5000); // Irelevant for this service really.
        setName("USB Device Discovery");
    }

    public void setup() {
        try {
            services = UsbHostManager.getUsbServices();
        } catch (UsbException ex) {
            Base.exception(ex);
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
            Base.exception(ex);
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
            Base.exception(e);
            Base.error(e);
        }
        return devs;
    }

    public void deviceAttached(UsbDevice dev) {
        try {
            Board b = getBoardByDevice(dev);
            if (b != null) {
                UsbHidDevice newPort = new UsbHidDevice(b, dev);
                synchronized(Base.communicationPorts) {
                    Base.communicationPorts.add(newPort);
                }
            }
        } catch (Exception exex) {
            Base.exception(exex);
            Base.error(exex);
        }
    }
    public void deviceDetached(UsbDevice dev) {
        try {
            UsbDeviceDescriptor desc = dev.getUsbDeviceDescriptor();
            short vid = desc.idVendor();
            short pid = desc.idProduct();
            String key = String.format("%04x:%04x", vid, pid);
            synchronized(Base.communicationPorts) {
                CommunicationPort fp = null;
                for (CommunicationPort cp  : Base.communicationPorts) {
                    if (cp instanceof UsbHidDevice) {
                        UsbHidDevice scp = (UsbHidDevice)cp;
                        if (scp.getKey().equals(key)) {
                            fp = cp;
                        }
                    }
                }
                if (fp != null) {
                    Base.communicationPorts.remove(fp);
                }
            }
        } catch (Exception exex) {
            Base.exception(exex);
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
                if (b.get("usb.programmer") == null) {
                    continue;
                }
                if (!b.get("usb.programmer").equals("hid")) {
                    continue;
                }
                short bvid = 0;
                short bpid = 0;
                try {
                    bvid = (short)Integer.parseInt(bvids, 16);
                    bpid = (short)Integer.parseInt(bpids, 16);
                } catch (Exception pie) {
                    Base.exception(pie);
                    continue;
                }
                if (vid == bvid && pid == bpid) {
                    Editor.broadcast("Found device " + b.getDescription());
                    return b;
                }
            }
        } catch (Exception e) {
            Base.exception(e);
            Base.error(e);
        }
        return null;
    } 
}
