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
import java.io.*;
import java.util.*;
import java.net.*;

public class NetworkDiscoveryService extends Service {

    public NetworkDiscoveryService() {
        setInterval(2000);
        setName("Network Discovery");
    }

    JmDNS jmdns = null;

    public void setup() {
        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
        } catch (UnknownHostException ex) {
        } catch (IOException ex) {
        }
        if (jmdns == null) {
            System.err.println("JmDNS service unable to start");
            stop();
        }
    }

    public void cleanup() {
    }

    public void loop() {
        // First we want a list of all the possible services to look for.
        ArrayList<String> fullServiceList = getServices();

        HashMap<Object, DiscoveredBoard> passBoards = new HashMap<Object, DiscoveredBoard>();
        HashMap<String, Board> boards = new HashMap<String, Board>();

        for(String service : fullServiceList) {
            if (service == null) {
                continue;
            }

            ServiceInfo[] foundServices = jmdns.list(service, 1000);

            if(foundServices != null) {
                for(ServiceInfo info : foundServices) {

                    InetAddress[] ips = info.getInetAddresses();
                    Board foundBoard = getBoardByService(info);

                    if(foundBoard != null) {

                        DiscoveredBoard db = new DiscoveredBoard();
                        db.board = foundBoard;
                        db.name = info.getName();
                        db.location = ips[0];
                        db.programmer = foundBoard.get("mdns.programmer");
                        db.version = info.getPropertyString(foundBoard.get("mdns.version"));
                        db.type = DiscoveredBoard.NETWORK;

                        if (foundBoard.get("mdns.class") != null) {
                            if (foundBoard.get("mdns.class").equals("ssh")) {
                                String uri = "ssh://";

                                byte[] ip = ips[0].getAddress();

                                String url = String.format("ssh://%d.%d.%d.%d:22",
                                     (int)ip[0] & 0xFF,
                                     (int)ip[1] & 0xFF,
                                     (int)ip[2] & 0xFF,
                                     (int)ip[3] & 0xFF);

                                boards.put(url, foundBoard);
                            }
                        }


                        for(Enumeration<String> e = info.getPropertyNames(); e.hasMoreElements();) {
                            String prop = (String)e.nextElement();
                            db.properties.set(prop, info.getPropertyString(prop));
                        }

                        passBoards.put(ips[0], db);
                    }
                }
            }
        }

        // Now let's look for any existing found network boards that aren't in our list and remove them

        ArrayList<CommunicationPort> toAdd = new ArrayList<CommunicationPort>();
        ArrayList<CommunicationPort> toRemove = new ArrayList<CommunicationPort>();

        for (CommunicationPort port : Base.communicationPorts) {
            if (port instanceof SSHCommunicationPort) {
                String name = port.toString();
                if (!boards.keySet().contains(name)) {
                    System.err.println("Removing " + port);
                    toRemove.add(port);
                }
            }
        }

        for (String name : boards.keySet()) {
            boolean found = false;
            for (CommunicationPort port : Base.communicationPorts) {
                if (port instanceof SSHCommunicationPort) {
                    String pname = port.toString();
                    if (pname.equals(name)) {
                        found = true;
                    }
                }
            }
            if (found == false) {
                System.err.println("Adding " + name);
                toAdd.add(new SSHCommunicationPort(name, boards.get(name)));
            }
        }

        for (CommunicationPort port : toRemove) {
            Base.communicationPorts.remove(port);
        }

        for (CommunicationPort port : toAdd) {
            Base.communicationPorts.add(port);
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
