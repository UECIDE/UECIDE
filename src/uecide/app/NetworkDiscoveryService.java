package uecide.app;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.*;
import java.util.*;
import java.net.*;

public class NetworkDiscoveryService {

    static Thread serviceDiscoveryThread = null;
    static boolean serviceDiscoveryThreadRunning = true;

    static public void startDiscoveringBoards() {
        serviceDiscoveryThread = new Thread() {
            public void run() {
                JmDNS jmdns = null;
                try {
                    jmdns = JmDNS.create(InetAddress.getLocalHost());
                } catch (Exception e) {
                    Base.error(e);
                    return;
                }
                while (serviceDiscoveryThreadRunning) {
                    try {
                        Thread.sleep(5000);
                        // First we want a list of all the possible services to look for.
                        ArrayList<String> fullServiceList = getServices();

                        HashMap<Object, DiscoveredBoard> passBoards = new HashMap<Object, DiscoveredBoard>();
                        for (String service : fullServiceList) {
                            ServiceInfo[] foundServices = jmdns.list(service, 1000);
                            if (foundServices != null) {
                                for (ServiceInfo info : foundServices) {
                                    InetAddress[] ips = info.getInetAddresses();
                                    Board foundBoard = getBoardByService(info);
                                    if (foundBoard != null) {
                                        DiscoveredBoard db = new DiscoveredBoard();
                                        db.board = foundBoard;
                                        db.name = info.getName();
                                        db.location = ips[0];
                                        db.version = info.getPropertyString(foundBoard.get("mdns.version"));
                                        db.type = DiscoveredBoard.NETWORK;
                                        for (Enumeration<String> e = info.getPropertyNames(); e.hasMoreElements();) {
                                            String prop = (String)e.nextElement();
                                            db.properties.set(prop, info.getPropertyString(prop));
                                        }
                
                                        passBoards.put(ips[0], db);
                                    }
                                }
                            }
                        }

                        // Now let's look for any existing found network boards that aren't in our list and remove them
                        ArrayList<Object> removableBoards = new ArrayList<Object>();
                        for (Object ob : Base.discoveredBoards.keySet()) {
                            if (!(ob instanceof InetAddress)) {
                                continue;
                            }
                            if (passBoards.get(ob) == null) {
                                removableBoards.add(ob);
                                Debug.message("Lost board " + Base.discoveredBoards.get(ob));
                            }
                        }
                        for (Object ob : removableBoards) {
                            Base.discoveredBoards.remove(ob);
                        }
                        for (Object ob : passBoards.keySet()) { 
                            if (Base.discoveredBoards.get(ob) == null) {
                                Base.discoveredBoards.put(ob, passBoards.get(ob));
                                Debug.message("Detected network board " + passBoards.get(ob));
                                Editor.broadcast("Detected network board " + passBoards.get(ob));
                            }

                        }
                    } catch (Exception e) {
                        Base.error(e);
                    }
                    
                }
            }
        };

        serviceDiscoveryThread.start();
    }

    public static ArrayList<String> getServices() {
        ArrayList<String>serviceList = new ArrayList<String>();
        for (Board board : Base.boards.values()) {
            String service = board.get("mdns.service");
            if (service == null) {
                continue;
            }
            if (serviceList.indexOf(service) == -1) {
                serviceList.add(service);
            }
        }
        return serviceList;
    }

    public static Board getBoardByService(ServiceInfo info) {
        for (Board board : Base.boards.values()) {
            String service = board.get("mdns.service");
            if (service == null) {
                continue;
            }
            if (service.equals(info.getTypeWithSubtype())) {
                String key = board.get("mdns.model.key");
                String value = board.get("mdns.model.value");
                String btype = info.getPropertyString(key);
                if (btype.equals(value)) {
                    return board;
                }
            }
        }
        return null;
    }
}
