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

import java.util.*;

public class IOKitNode {

    public static class Flags {
        public static final int REGISTERED = 0x0001;
        public static final int MATCHED = 0x0002;
        public static final int ACTIVE = 0x0004;
        public static final int BUSY = 0x0008;
    }


    HashMap<String, String> attributes;
    ArrayList<IOKitNode> children;
    IOKitNode parent;

    String nodeClass;
    String name;
    int id;
    int flags;
    int busy;
    int busyms;
    int retain;

    public IOKitNode(String nc, IOKitNode p) {
        attributes = new HashMap<String, String>();
        children = new ArrayList<IOKitNode>();
        parent = p;
        nodeClass = null;
        name = nc;
        id = 0;
        flags = 0;
        busy = 0;
        busyms = 0;
        retain = 0;
    }

    public ArrayList<IOKitNode> getChildren() {
        return children;
    }

    public String toString() {
        StringBuilder o = new StringBuilder();
        o.append(name);
        o.append("  <class ");
        o.append(nodeClass);
        o.append(", id ");
        o.append(id);
        o.append(", ");

        if (!isRegistered()) { o.append("!"); }
        o.append("registered, ");

        if (!isMatched()) { o.append("!"); }
        o.append("matched, ");

        if (!isActive()) { o.append("!"); }
        o.append("active, ");

        o.append("busy ");
        o.append(busy);
        if (busyms > 0) {
            o.append(" (");
            o.append(busyms);
            o.append(" ms)");
        }
        o.append(", retain ");
        o.append(retain);
        o.append(">");

        return o.toString();
    }

    public void set(String k, String v) {
        attributes.put(k, v);
    }

    public HashMap<String, String>getAttributes() {
        return attributes;
    }

    public String get(String k) {
        return attributes.get(k);
    }

    public IOKitNode getParentNode() {
        return parent;
    }

    public void setFlag(int f) {
        flags |= f;
    }

    public void clearFlag(int f) {
        flags &= ~f;
    }

    public int getFlags() {
        return flags;
    }

    public boolean isRegistered() {
        return (flags & Flags.REGISTERED) != 0;
    }

    public boolean isMatched() {
        return (flags & Flags.MATCHED) != 0;
    }

    public boolean isActive() {
        return (flags & Flags.ACTIVE) != 0;
    }

    public void setNodeClass(String c) {
        nodeClass = c;
    }

    public String getNodeClass() {
        return nodeClass;
    }

    public void add(IOKitNode n) {
        children.add(n);
    }

    public void setId(int i) {
        id = i;
    }

    public int getId() {
        return id;
    }

    public void setBusy(int b) {
        busy = b;
    }

    public int getBusy() {
        return busy;
    }

    public void setBusyTime(int b) {
        busyms = b;
    }

    public int getBusyTime() {
        return busyms;
    }

    public void setRetain(int r) {
        retain = r;
    }

    public int getRetain() {
        return retain;
    }
}

