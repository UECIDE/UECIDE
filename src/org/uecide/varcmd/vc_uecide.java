package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;
import java.util.UUID;

public class vc_uecide implements VariableCommand {
    public String main(Sketch sketch, String args) {
        if (args.equals("name")) {
            return Base.theme.get("product");
        }
        if (args.equals("version")) {
            return Base.systemVersion.toString();
        }
        if (args.equals("uuid")) {
            return UUID.randomUUID().toString();
        }
        return "unknown";
    }
}
