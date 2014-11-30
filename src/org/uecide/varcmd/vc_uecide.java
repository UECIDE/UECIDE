package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

public class vc_uecide implements VariableCommand {
    public String main(Sketch sketch, String args) {
        if (args.equals("name")) {
            return Base.theme.get("product");
        }
        if (args.equals("version")) {
            return Base.systemVersion.toString();
        }
        return "unknown";
    }
}
