package org.uecide.varcmd;

import org.uecide.*;

public class vc_foreach implements VariableCommand {
    public String main(Sketch sketch, String args) {
        int commaPos = args.indexOf(',');

        if(commaPos > 0) {
            String data = args.substring(0, commaPos);
            String format = args.substring(commaPos + 1);
            String[] each = data.split("::");
            String outString = "";

            for(String chunk : each) {
                String ns = format.replaceAll("%0", chunk);

                if(outString.equals("")) {
                    outString = ns;
                } else {
                    outString = outString + "::" + ns;
                }
            }

            return outString;
        } else {
            return "Syntax error in foreach";
        }
    }
}
