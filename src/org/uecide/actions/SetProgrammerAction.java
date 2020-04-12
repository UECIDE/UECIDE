package org.uecide.actions;

import org.uecide.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.net.InetAddress;
import net.straylightlabs.hola.sd.Instance;

public class SetProgrammerAction extends Action {

    public SetProgrammerAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "SetProgrammer <codename>"
        };
    }

    public String getCommand() { return "setprogrammer"; }

    static boolean inhibitUpdate = false;

    public boolean actionPerformed(Object[] args) throws ActionException {
        if (inhibitUpdate) return false; // I hate this!
        try {
            if (args.length != 1) {
                throw new SyntaxErrorActionException();
            }

            if (args[0] instanceof Programmer) {
                Programmer prog = (Programmer)args[0];
                ctx.setProgrammer(prog);
                inhibitUpdate = true;
                prog.onSelected(ctx);
                inhibitUpdate = false;
                ctx.action("setPref", "board." + ctx.getBoard().getName() + ".programmer", ctx.getProgrammer().getName());
                return true;
            } else if (args[0] instanceof String) {
                String s = (String)args[0];
                Programmer b = Programmer.getProgrammer(s);
                if (b == null) {
                    if (s.contains("@")) { // It's a network programmer
                        String[] parts = s.split("@");
                        String progname = parts[0];
                        String hostspec = parts[1];
                        String hostname = hostspec;
                        int port = 8266;
                        if (hostspec.contains(":")) {
                            parts = hostspec.split(":");
                            hostname = parts[0];
                            port = Utils.s2i(parts[1]);
                        }
                        HashMap<String, String> attribs = new HashMap<String, String>();
                        attribs.put("board", "\"" + ctx.getBoard().getName() + "\"");

                        // TODO: This slows down the booting of the system when a network programmer is selected by default.
                        InetAddress[] ips = InetAddress.getAllByName(hostname + ".local");
                        List<InetAddress> iplist = Arrays.asList(ips);
                        
                        Instance i = new Instance(hostname, iplist, port, attribs);
                        b = new mDNSProgrammer(i, ctx.getBoard());
                        Programmer.addProgrammer(s, b);
                        
                    } else {
                        throw new ActionException("Unknown Programmer");
                    }
                }
                ctx.setProgrammer(b);
                inhibitUpdate = true;
                b.onSelected(ctx);
                inhibitUpdate = false;
                ctx.action("setPref", "board." + ctx.getBoard().getName() + ".programmer", ctx.getProgrammer().getName());
                return true;
            }
            throw new BadArgumentActionException();
        } catch (Exception ex) {
            Debug.exception(ex);
            throw new ActionException(ex.getMessage());
        }
    }
}
