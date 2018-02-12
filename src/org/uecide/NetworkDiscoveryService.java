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

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceEvent;
import java.io.*;
import java.util.*;
import java.net.*;

public class NetworkDiscoveryService extends Service {


    static class BoardServiceListener implements ServiceListener {
        public void serviceResolved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();

            String board = info.getPropertyString("board");
            if (board == null) {
                return;
            }
            Board foundBoard = Base.boards.get(board);
            if (foundBoard == null) {
                return;
            }

            String protocol = info.getPropertyString("protocol");
            if (protocol == null) {
                return;
            }

            if (protocol.equals("ssh")) {

                InetAddress[] ips = info.getInetAddresses();
                byte[] ip = ips[0].getAddress();
                String[] urls = info.getURLs();

                SSHCommunicationPort newPort = new SSHCommunicationPort(info.getName(), foundBoard, ips[0], info.getPort());
                for (Enumeration<String> e = info.getPropertyNames(); e.hasMoreElements();) {
                    String k = e.nextElement();
                    newPort.set(k, info.getPropertyString(k));
                }


                int exist = -1;
                synchronized(Base.communicationPorts) {

                    int i = 0;
        
                    for (CommunicationPort cp : Base.communicationPorts) {
                        if (cp instanceof SSHCommunicationPort) {
                            if (cp.toString().equals(newPort.toString())) {
                                exist = i;
                                break;
                            }
                        }
                        i++;
                    }

                    if (exist == -1) {
                        Base.communicationPorts.add(newPort);
                    }
                }
                if (exist == -1) {
                    Editor.broadcast(String.format("Found %s (%d.%d.%d.%d)",
                        info.getName(),
                        (int)ip[0] & 0xFF,
                        (int)ip[1] & 0xFF,
                        (int)ip[2] & 0xFF,
                        (int)ip[3] & 0xFF
                    ));
                }
            }
        }
        public void serviceAdded(ServiceEvent event) {
        }
        public void serviceRemoved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            String key = info.getName();

            synchronized(Base.communicationPorts) {
                CommunicationPort fp = null;
                for (CommunicationPort cp  : Base.communicationPorts) {
                    if (cp instanceof SSHCommunicationPort) {
                        SSHCommunicationPort scp = (SSHCommunicationPort)cp;
                        if (scp.getKey().equals(key)) {
                            fp = cp;
                        }
                    }
                }
                if (fp != null) {
                    Base.communicationPorts.remove(fp);
                }
            }
        }
    }

    BoardServiceListener boardListener = new BoardServiceListener();

    public NetworkDiscoveryService() {
        setInterval(10000);
        setName("Network Discovery");
    }

    JmDNS jmdns = null;

    public void setup() {
        try {
            byte[] ipAll = {0, 0, 0, 0};
            jmdns = JmDNS.create(InetAddress.getByAddress(ipAll));
        } catch (UnknownHostException ex) {
        } catch (IOException ex) {
        }
        if (jmdns == null) {
            System.err.println("JmDNS service unable to start");
            stop();
        }
        ArrayList<String> fullServiceList = getServices();

        jmdns.addServiceListener("_uecide._tcp.local.", boardListener);

//        for (String service : fullServiceList) {
//            jmdns.addServiceListener(service, boardListener);
//        }
    }

    public void cleanup() {
    }

    public void loop() {
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

    public Board getBoardByService(ServiceInfo info) {
        for(Board board : Base.boards.values()) {
            String service = board.get("mdns.service");

            if(service == null) {
                continue;
            }

            if(service.equals(info.getTypeWithSubtype())) {
                String key = board.get("mdns.model.key");
                String value = board.get("mdns.model.value");
                String btype = info.getPropertyString(key);
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
