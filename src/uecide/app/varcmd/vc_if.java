package uecide.app.varcmd;

import uecide.app.*;

public class vc_if {
    public static String main(Sketch sketch, String args) {
        String[] bits = args.split(",");

        if(bits.length != 3) {
            return "Syntax error in if - bad arg count";
        } else {
            String condition = bits[0];
            String trueVal = bits[1];
            String falseVal = bits.length == 3 ? bits[2] : "";

            String[] conditionBits = condition.split("=");

            if(conditionBits.length != 2) {
                return "Syntax Error in if - bad comparison";
            } else {
                String leftVal = conditionBits[0].trim();
                String rightVal = conditionBits[1].trim();

                if(leftVal.equals(rightVal)) {
                    return trueVal;
                } else {
                    return falseVal;
                }
            }
        }
    }
}
