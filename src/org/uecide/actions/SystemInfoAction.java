package org.uecide.actions;

import org.uecide.*;
import java.util.TreeMap;

public class SystemInfoAction extends Action {

    public SystemInfoAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "SystemInfo"
        };
    }

    public String getCommand() { return "systeminfo"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        ctx.heading("System Information");
        ctx.bullet("Operating system: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version"));
        ctx.bullet("Java version: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
        ctx.bullet("UECIDE version: " + UECIDE.getVersion());
        return true;
    }
}
