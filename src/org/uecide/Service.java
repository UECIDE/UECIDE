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

package org.uecide;

public abstract class Service implements Runnable {
    int interval;
    Thread myThread;
    String name;

    boolean running = false;
    boolean active = false;

    public Thread start() {
        running = true;
        myThread = new Thread(this);
        myThread.start();
        return myThread;
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean wait) {
        running = false;
        if (wait) {
            while (active) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
                continue;
            }
        }
    }

    public Thread restart() {
        stop(true);
        return start();
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int i) {
        interval = i;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    // Heh, let's model it after the Arduino structure.
    // I mean, why not, eh?

    public void run() {
        active = true;
        System.err.println("Service '" + getName() + "' started");
        setup();
        while (running) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                continue;
            }
            loop();
        }
        cleanup();
        System.err.println("Service '" + getName() + "' stopped");
        active = false;
    }

    public String getKey() {
        String className = null;
        Class<?> enclosingClass = getClass().getEnclosingClass();
        if (enclosingClass != null) {
            className = enclosingClass.getName();
        } else {
            className = getClass().getName();
        }

        className = className.substring(className.lastIndexOf(".") + 1);
        return className.toLowerCase();
    }

    public boolean isAutoStart() {
        return Preferences.getBoolean("service." + getKey() + ".autostart");
    }

    public void setAutoStart(boolean b) {
        Preferences.setBoolean("service." + getKey() + ".autostart", b);
    }

    public abstract void setup();
    public abstract void loop();
    public abstract void cleanup();
}

