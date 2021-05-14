/*
 * Copyright (c) 2015, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide.varcmd;

import org.uecide.Context;
import java.util.Stack;

public class vc_math extends VariableCommand {
    public String main(Context sketch, String args) throws VariableCommandException {
        String[] bits = args.split(",");

        Stack<Double> stack = new Stack<Double>();

        for (String bit : bits) {
            Double val = 0.0d;
            boolean isNumber = false;

            try {
                val = Double.parseDouble(bit);
                isNumber = true;
            } catch (Exception e) {
                isNumber = false;
            }

            if (isNumber) {
                stack.push(val);
            } else {
                if (bit.equals("+")) {
                    Double a = stack.pop();
                    Double b = stack.pop();
                    stack.push(a + b);
                } else if (bit.equals("-")) {
                    Double a = stack.pop();
                    Double b = stack.pop();
                    stack.push(a - b);
                } else if (bit.equals("/")) {
                    Double a = stack.pop();
                    Double b = stack.pop();
                    stack.push(a / b);
                } else if (bit.equals("*")) {
                    Double a = stack.pop();
                    Double b = stack.pop();
                    stack.push(a * b);
                } else if (bit.equals("sin")) {
                    Double a = stack.pop();
                    stack.push(Math.sin(a));
                } else if (bit.equals("cos")) {
                    Double a = stack.pop();
                    stack.push(Math.cos(a));
                }
            }
        }

        String out = "" + stack.pop();

        return out;
    }
}
