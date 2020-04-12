package org.uecide;

import java.net.*;
import java.io.*;
import java.util.*;

import net.straylightlabs.hola.sd.Instance;

public class mDNSProgrammer extends Programmer {

    Board _board;
    Instance _info;
    InetAddress _ip;
    Programmer _programmer;

    @SuppressWarnings("unchecked")
    public mDNSProgrammer(Instance info, Board b) {
        _info = info;
        _board = b;

        InetAddress[] ips = _info.getAddresses().toArray(new InetAddress[0]);
        
        _ip = ips[0];

        _programmer = org.uecide.Programmer.getProgrammer(_board.get("mdns.programmer"));

        if (_programmer == null) {
            return;
        }
        getProperties().mergeData(_programmer.getProperties());
        
        Map<String,String> txt = _info.getAttributes();
        for (String k : txt.keySet()) {
            set("mdns.data." + k, txt.get(k));
        }

        setRelatedObject(_board);

        set("name", _programmer.getName() + "@" + _info.getName() + ":" + _info.getPort());
        set("description", _board.getDescription() + " on " + _info.getName() + "(" + _ip.getHostAddress() + ")");
        set("mdns.created", "true");
        set("hidden", "false");
    }

    public File getFolder() {
        return _programmer.getFolder();
    }

    public String getName() {
        return get("name");
    }

    @Override
    public String getDescription() {
        return _board.getDescription() + " on " + _ip.getHostAddress() + " (" + _info.getName() + ")";
    }

    public boolean programFile(Context ctx, String file) {
        set("ip", _ip.getHostAddress());
        set("hostname", _info.getName());
        set("port", String.valueOf(_info.getPort()));
        return _programmer.programFile(ctx, file);
    }

    @Override
    public int compareTo(Object o) {
        if(o == null) {
            return 0;
        }

        UObject ob = (UObject)o;
        return getDescription().compareTo(ob.getDescription());
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public void onSelected(Context ctx) {
        ctx.action("SetBoard", _board);
    }
}
