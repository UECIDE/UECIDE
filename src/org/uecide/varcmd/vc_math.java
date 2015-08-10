package org.uecide.varcmd;

import org.uecide.*;
import java.util.*;

public class vc_math implements VariableCommand {
    public String main(Context sketch, String args) {
        String[] bits = args.split(",");

        Stack<Float> stack = new Stack<Float>();

        for (String bit : bits) {
            Float val = 0.0f;
            boolean isNumber = false;

            try {
                val = Float.parseFloat(bit);
                isNumber = true;
            } catch (Exception e) {
                isNumber = false;
            }

            if (isNumber) {
                stack.push(val);
            } else {
                if (bit.equals("+")) {
                    Float a = stack.pop();
                    Float b = stack.pop();
                    stack.push(a + b);
                } else if (bit.equals("-")) {
                    Float a = stack.pop();
                    Float b = stack.pop();
                    stack.push(a - b);
                } else if (bit.equals("/")) {
                    Float a = stack.pop();
                    Float b = stack.pop();
                    stack.push(a / b);
                } else if (bit.equals("*")) {
                    Float a = stack.pop();
                    Float b = stack.pop();
                    stack.push(a * b);
                } 
            }
        }

        String out = "" + stack.pop();

        return out;
    }
}
