package org.uecide.actions;

import org.uecide.*;
import java.util.TreeMap;

public class PrintPropertiesAction extends Action {

    public PrintPropertiesAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "PrintProperties"
        };
    }

    public String getCommand() { return "printproperties"; }

    public boolean actionPerformed(Object[] args) throws ActionException {

        PropertyFile pf = ctx.getMerged();
        TreeMap<String, String> map = pf.toTreeMap(true);

        int maxklen = 0;
        int maxvlen = 0;
        for (String k : map.keySet()) {
            int l = k.length();
            if (l > maxklen) maxklen = l;
            l = map.get(k).length();
            if (l > maxvlen) maxvlen = l;
        }

        String format = "%" + maxklen + "s | %s";

        ctx.message(String.format(format, "Key", "Value"));

        String lines = "";
        for (int i = 0; i < (maxklen); i++) {
            lines += "-";
        }
        lines += "-|-";
        for (int i = 0; i < (maxvlen); i++) {
            lines += "-";
        }
        ctx.message(lines);

        for (String k : map.keySet()) {
            ctx.message(String.format(format, k, map.get(k)));
        }

        return true;
    }
}
