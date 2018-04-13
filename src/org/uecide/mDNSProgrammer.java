package org.uecide;

import java.net.*;
import java.io.*;
import java.util.*;
import net.posick.mDNS.*;


public class mDNSProgrammer extends Programmer {

    Board _board;
    ServiceInstance _info;
    InetAddress _ip;
    Programmer _programmer;

    @SuppressWarnings("unchecked")
    public mDNSProgrammer(ServiceInstance info, Board b) {
        _info = info;
        _board = b;

        InetAddress[] ips = _info.getAddresses();
        
        _ip = ips[0];

        _programmer = Base.programmers.get(_board.get("mdns.programmer"));

        if (_programmer == null) {
            return;
        }
        getProperties().mergeData(_programmer.getProperties());
        
        Map<String,String> txt = _info.getTextAttributes();
        for (String k : txt.keySet()) {
            set("mdns.data." + k, txt.get(k));
        }

        setRelatedObject(_board);

        set("name", _programmer.getName() + "-" + _ip.getHostAddress());
        set("description", _board.getDescription() + " on " + _ip.getHostAddress() + " (" + _info.getName().getInstance() + ")");
        set("mdns.created", "true");
        unset("hidden");
    }

    public File getFolder() {
        return _programmer.getFolder();
    }

    public String getName() {
        return get("name");
    }

    public String getDescription() {
        return get("description");
    }

    public boolean programFile(Context ctx, String file) {
        set("ip", _ip.getHostAddress());
        set("port", String.valueOf(_info.getPort()));
        return _programmer.programFile(ctx, file);
    }

    public int compareTo(Object o) {
        if(o == null) {
            return 0;
        }

        UObject ob = (UObject)o;
        return get("description").compareTo(ob.getDescription());
    }

    public String toString() {
        return get("description");
    }

    public void onSelected(Editor e) {
        e.getSketch().setBoard(_board);
    }
}
