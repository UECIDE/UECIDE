package org.uecide.varcmd;

import org.uecide.*;
import java.util.Random;

public class vc_random implements VariableCommand {
    public String main(Sketch sketch, String args) {
        String[] bits = args.split(",");
        if (bits.length != 2) {
            return "ERR";
        }
        try {
            int low = Integer.parseInt(bits[0]);
            int high = Integer.parseInt(bits[1]);
            Random r = new Random();
            int val = Math.abs(r.nextInt());
            val = val % (high - low);
            val += low;
            return Integer.toString(val);
        } catch (Exception ex) {
        }
        return "ERR";

    }
}
