/*
 * Copyright (c) 2016, Majenko Technologies
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

package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class port implements BuiltinCommand {
    static CommunicationPort comPort = null;

    public boolean main(Context ctx, String[] arg) {
        if (arg.length == 0) {
            return false;
        }
        try {
            String action = arg[0];
            if (action.equals("open")) {


                if (comPort != null) {
                    comPort.closePort();
                }
                comPort = ctx.getDevice();
                comPort.openPort();
                if (arg.length >= 2) {
                    int speed = Integer.parseInt(arg[1]);
                    comPort.setSpeed(speed);
                }
                return true;
            } else if (action.equals("close")) {
                if (comPort != null) {
                    comPort.closePort();
                    comPort = null;
                }
                return true;
            } else if (action.equals("print")) {
                if (comPort != null) {
                    comPort.print(arg[1]);
                }
                return true;
            } else if (action.equals("println")) {
                if (comPort != null) {
                    comPort.println(arg[1]);
                }
                return true;
            } else if (action.equals("pulse")) {
                if (comPort != null) {
                    comPort.pulseLine();
                }
                return true;
            }
            

        
   
        } catch (Exception e) {
            Base.error(e);
        }
        return false;
    }
}
