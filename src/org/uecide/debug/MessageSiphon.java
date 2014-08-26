/*
 * Copyright (c) 2014, Majenko Technologies
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

package org.uecide.debug;

import java.io.*;


/**
 * Slurps up messages from compiler.
 */
public class MessageSiphon implements Runnable {
    BufferedReader streamReader;
    public Thread thread;
    MessageConsumer consumer;
    int channel;


    public MessageSiphon(InputStream stream, MessageConsumer consumer) {
        this.streamReader = new BufferedReader(new InputStreamReader(stream));
        this.consumer = consumer;

        thread = new Thread(this, "Message Siphon");
        // don't set priority too low, otherwise exceptions won't
        // bubble up in time (i.e. compile errors have a weird delay)
        //thread.setPriority(Thread.MIN_PRIORITY);
        thread.setPriority(Thread.MAX_PRIORITY - 1);
        thread.start();
        channel = 0;
    }

    public void setChannel(int c) {
        channel = c;
    }

    public void run() {
        try {
            // process data until we hit EOF; this will happily block
            // (effectively sleeping the thread) until new data comes in.
            // when the program is finally done, null will come through.
            //
            String currentLine;

            while((currentLine = streamReader.readLine()) != null) {

                System.err.println(channel + ": " + currentLine);
                switch(channel) {
                default:
                case 0:
                    consumer.message(currentLine);
                    break;

                case 1:
                    consumer.warning(currentLine);
                    break;

                case 2:
                    consumer.error(currentLine);
                    break;
                }
            }

            //EditorConsole.systemOut.println("messaging thread done");
            thread = null;

        } catch(NullPointerException npe) {
            // Fairly common exception during shutdown
            thread = null;

        } catch(Exception e) {
            // On Linux and sometimes on Mac OS X, a "bad file descriptor"
            // message comes up when closing an applet that's run externally.
            // That message just gets supressed here..
            String mess = e.getMessage();

            if((mess != null) &&
                    (mess.indexOf("Bad file descriptor") != -1)) {
                //if (e.getMessage().indexOf("Bad file descriptor") == -1) {
                //System.err.println("MessageSiphon err " + e);
                //e.printStackTrace();
            } else {
                e.printStackTrace();
            }

            thread = null;
        }
    }


    public Thread getThread() {
        return thread;
    }
}
