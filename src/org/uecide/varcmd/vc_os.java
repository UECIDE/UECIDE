package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

public class vc_os implements VariableCommand {
    public String main(Sketch sketch, String args) {
        if (args.equals("name")) {
            return Base.getOSName();
        }
        if (args.equals("arch")) {
            return Base.getOSArch();
        }
        if (args.equals("version")) {
            return Base.getOSVersion();
        }
        if (args.equals("flavour")) {
            return Base.getOSFlavour();
        }
        if (args.equals("flavor")) {
            return Base.getOSFlavour();
        }
        return "unknown";
    }
}
