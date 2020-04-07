package org.uecide.actions;

import org.uecide.*;
import java.io.File;

public class SetDeviceAction extends Action {

    public SetDeviceAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "SetDevice <device>"
        };
    }

    public String getCommand() { return "setdevice"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        try {

            if (args.length != 1) {
                throw new SyntaxErrorActionException();
            }

            if (ctx == null) return false;

            if (args[0] == null) return false;

            if (args[0] instanceof CommunicationPort) {
                ctx.setDevice((CommunicationPort)args[0]);
                ctx.action("setPref", "board." + ctx.getBoard().getName() + ".port", ctx.getDevice().toString());
                return true;
            } else if (args[0] instanceof String) {
                String name = (String)args[0];

                for (CommunicationPort dev : UECIDE.communicationPorts) {
                    if (dev.toString().equals(name)) {
                        ctx.setDevice(dev);
                        ctx.action("setPref", "board." + ctx.getBoard().getName() + ".port", ctx.getDevice().toString());
                        return true;
                    }
                }
                if (UECIDE.isPosix()) {
                    File f = new File(name);
                    if (f.exists() && !f.isDirectory()) {
                        SerialCommunicationPort p = new SerialCommunicationPort(f.getAbsolutePath());
                        UECIDE.communicationPorts.add(p);
                        ctx.setDevice(p);
                        ctx.action("setPref", "board." + ctx.getBoard().getName() + ".port", ctx.getDevice().toString());
                        return true;
                    }
                }

//                ctx.error("Unknown device: " + (String)args[0]);
                return false;
            }
            throw new BadArgumentActionException();
        } catch (Exception ex) {
            Debug.exception(ex);
            throw new ActionException(ex.getMessage());
        }
    }
}
