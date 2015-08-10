package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;
import java.util.UUID;

public class vc_uecide implements VariableCommand {
    public String main(Context sketch, String args) {
        if (args.equals("name")) {
            return Base.theme.get("product");
        }
        if (args.equals("version")) {
            return Base.systemVersion.toString();
        }
        if (args.equals("uuid")) {
            return UUID.randomUUID().toString();
        }
        if (args.equals("root")) {
            File jar = Base.getJarLocation();
            File idir = jar.getParentFile();
            return idir.getAbsolutePath();
        }
        if (args.equals("jar")) {
            File jar = Base.getJarLocation();
            return jar.getAbsolutePath();
        }
        return "unknown";
    }
}
