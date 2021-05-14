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
import org.uecide.Utils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class vc_if extends VariableCommand {
    public String main(Context sketch, String args) throws VariableCommandException {
        String[] bits = args.split(",");

        if(bits.length != 3) {
            throw new VariableCommandException("Syntax Error");
        } 

        String condition = bits[0];
        String trueVal = bits[1];
        String falseVal = bits[2];


        Pattern pat = Pattern.compile("^\\s*(\\w+)\\s*([=!<>]+)\\s*(\\w+)\\s*$");
        Matcher mat = pat.matcher(condition);

        if (!mat.find()) {
            throw new VariableCommandException("Syntax Error");
        }

        String leftVal = mat.group(1);
        String comparison = mat.group(2);
        String rightVal = mat.group(3);

        boolean result = false;

        int leftNum = Utils.s2i(leftVal);
        int rightNum = Utils.s2i(rightVal);

        switch (comparison) {
            case "=":
            case "==":
                result = leftVal.equals(rightVal);
                break;
            case "!=":
            case "=!=":
            case "!==":
                result = !leftVal.equals(rightVal);
                break;
            case "<":
                result = leftNum < rightNum;
                break;
            case ">":
                result = leftNum > rightNum;
                break;
            case "<=":
                result = leftNum <= rightNum;
                break;
            case ">=":
                result = leftNum >= rightNum;
                break;
            default:
                throw new VariableCommandException("Syntax Error");
        }
        if (result == true) {
            return trueVal;
        }
        return falseVal;
    }
}
