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
import java.net.*;

import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.sd.Instance;
import net.straylightlabs.hola.sd.Query;
import net.straylightlabs.hola.sd.Service;


public class NetworkDiscoveryService extends org.uecide.Service {

    public NetworkDiscoveryService() {
        setInterval(5000);
        setName("Network Discovery");
    }

    public void setup() {
    }

    public void cleanup() {
    }

    @SuppressWarnings("unchecked")
    public void loop() {
        try {
            ArrayList<String> fullServiceList = getServices();
            for (String svc : fullServiceList) {
                Service service = Service.fromName(svc);

                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress myAddress = enumIpAddr.nextElement();
                        try {
                            Query query = Query.createFor(service, Domain.LOCAL);
                            Set<Instance> instances = query.runOnceOn(myAddress);
                            for (Instance i : instances) {
                                Board b = getBoardByService(svc, i);
                                if (b != null) {
                                    mDNSProgrammer p = new mDNSProgrammer(i, b);
                                    Programmer oldP = Programmer.programmers.get(p.getName());
                                    if (oldP == null) {
                                        Programmer.programmers.put(p.getName(), p);
                                    }
                                }
                            }
                        } catch (Exception exc) {
                            if (!exc.getMessage().equals("Network is unreachable")) {
                                Debug.exception(exc);
                            }
                        }
                    }
                }
            }
        } catch (Exception exc) {
            Debug.exception(exc);
        }
    }

    public ArrayList<String> getServices() {
        ArrayList<String>serviceList = new ArrayList<String>();

        for(Board board : Board.boards.values()) {
            String service = board.get("mdns.service");

            if(service == null) {
                continue;
            }

            if (service.endsWith(".local.")) {
                service = service.substring(0, service.length() - 7);
            }

            if(serviceList.indexOf(service) == -1) {
                serviceList.add(service);
            }
        }

        return serviceList;
    }

    //public static Board getBoardByService(ServiceInfo info) {
    public static Board getBoardByService(String svc, Instance i) {
        for(Board board : Board.boards.values()) {
            String service = board.get("mdns.service");
            if(service == null) {
                continue;
            }
            if (service.endsWith(".local.")) {
                service = service.substring(0, service.length() - 7);
            }

            if(service.equals(svc)) { 
                String key = board.get("mdns.model.key");
                String value = board.get("mdns.model.value");
                String btype = i.lookupAttribute(key); //info.getPropertyString(key);
                if (btype != null) {
                    btype = btype.replaceAll("^\"|\"$", "");
                    if(btype.equals(value)) {
                        return board;
                    }
                }
            }
        }

        return null;
    }
}
