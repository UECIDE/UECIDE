package org.uecide.varcmd;

import org.uecide.*;
import java.util.HashMap;

public class vc_arduino implements VariableCommand {

    public static HashMap<String, PropertyFile> propCache = new HashMap<String, PropertyFile>();

    public String main(Sketch sketch, String args) {
        String[] bits = args.split(",");
        if (bits.length != 2) {
            return "ERR";
        }
        PropertyFile props = propCache.get(bits[0]);
        if (props == null) {
            props = PropertyFile.parseArduinoFile(bits[0]);
            propCache.put(bits[0], props);
        }
        return props.get(bits[1]);
    }
}
