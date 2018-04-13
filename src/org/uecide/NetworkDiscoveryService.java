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

import net.posick.mDNS.*;
import org.xbill.DNS.Type;


public class NetworkDiscoveryService extends Service {

    public NetworkDiscoveryService() {
        setInterval(15000);
        setName("Network Discovery");
    }

    public void setup() {
    }

    public void cleanup() {
    }

    @SuppressWarnings("unchecked")
    public void loop() {
        Lookup lookup = null;
        try {
            ArrayList<String> fullServiceList = getServices();
            for (String svc : fullServiceList) {
                ServiceName service = new ServiceName(svc);
                lookup = new Lookup(service, Type.PTR);
                ServiceInstance[] services = lookup.lookupServices();
                for (ServiceInstance s : services) {
                    InetAddress[] addresses = s.getAddresses();
                    int port = s.getPort();
                    Map<String, String>txt = s.getTextAttributes();
                    String serviceName = s.getName().getFullType() + "." + s.getName().getDomain();
                    Board b = getBoardByService(serviceName, txt);
                    if (b != null) {
                        mDNSProgrammer p = new mDNSProgrammer(s, b);
                        Programmer oldP = Base.programmers.get(p.getName());
                        if (oldP == null) {
                            Base.programmers.put(p.getName(), p);
                            Editor.broadcast("Found " + p.getDescription());
                        }
                    }
                }
                lookup.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getServices() {
        ArrayList<String>serviceList = new ArrayList<String>();

        for(Board board : Base.boards.values()) {
            String service = board.get("mdns.service");

            if(service == null) {
                continue;
            }

            if(serviceList.indexOf(service) == -1) {
                serviceList.add(service);
            }
        }

        return serviceList;
    }

    //public static Board getBoardByService(ServiceInfo info) {
    public static Board getBoardByService(String svc, Map<String,String>txt) {
        for(Board board : Base.boards.values()) {
            String service = board.get("mdns.service");

            if(service == null) {
                continue;
            }

            if(service.equals(svc)) { //info.getTypeWithSubtype())) {
                String key = board.get("mdns.model.key");
                String value = board.get("mdns.model.value");
                String btype = txt.get(key); //info.getPropertyString(key);
                if (btype != null) {
                    if(btype.equals(value)) {
                        return board;
                    }
                }
            }
        }

        return null;
    }
}
