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

import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class IOKit {
    static IOKitNode rootNode;

    public static int parseRegistry() {
        Pattern classPattern = Pattern.compile("([^\\s]+)\\s+<(.+)>");
        Pattern attrPattern = Pattern.compile("\"(.+)\"\\s+=\\s+(.*)");

        Stack<IOKitNode> stack = new Stack<IOKitNode>();

        try {
            ProcessBuilder pb = new ProcessBuilder("ioreg", "-w0", "-l");

            Process proc = pb.start();
            InputStream in = proc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            int currentDepth = 0;
            rootNode = null;
            IOKitNode lastNode = null;
            IOKitNode parentNode = null;


            String line;
            while ((line = br.readLine()) != null) {
                int indent = line.indexOf("+-o");
                if (indent == -1) { // In attributes
                    Matcher m = attrPattern.matcher(line);
                    if (m.find()) {
                        String k = m.group(1).trim();
                        String v = m.group(2).trim();

                        if (k.startsWith("\"")) {
                            k = k.substring(1, k.length() - 1);
                        }
                        if (v.startsWith("\"")) {
                            v = v.substring(1, v.length() - 1);
                        }
                        lastNode.set(k, v);
                    }
                    
                } else {
                    Matcher m = classPattern.matcher(line);
                    if (m.find()) {
                        String name = m.group(1);
                        String data = m.group(2);

                        IOKitNode newNode = new IOKitNode(name, parentNode);
                        String[] dataBits = data.split(", ");

                        for (String db : dataBits) {
                            db = db.trim();
                            if (db.equals("registered")) { newNode.setFlag(IOKitNode.Flags.REGISTERED); }
                            if (db.equals("!registered")) { newNode.clearFlag(IOKitNode.Flags.REGISTERED); }
                            if (db.equals("matched")) { newNode.setFlag(IOKitNode.Flags.MATCHED); }
                            if (db.equals("!matched")) { newNode.clearFlag(IOKitNode.Flags.MATCHED); }
                            if (db.equals("active")) { newNode.setFlag(IOKitNode.Flags.ACTIVE); }
                            if (db.equals("!active")) { newNode.clearFlag(IOKitNode.Flags.ACTIVE); }
                            if (db.startsWith("class ")) { newNode.setNodeClass(db.substring(6)); }
                            if (db.startsWith("id 0x")) { newNode.setId(Integer.parseInt(db.substring(5), 16)); }
                            if (db.startsWith("retain ")) { newNode.setRetain(Integer.parseInt(db.substring(7), 10)); }
                            if (db.startsWith("busy ")) {
                                Pattern withTime = Pattern.compile("^busy (\\d+) \\((\\d+) ms\\)$");
                                Matcher tm = withTime.matcher(db);
                                if (tm.find()) {
                                    newNode.setBusy(Integer.parseInt(tm.group(1)));
                                    newNode.setBusyTime(Integer.parseInt(tm.group(2)));
                                } else {
                                    newNode.setBusy(Integer.parseInt(db.substring(5), 10));
                                    newNode.setBusyTime(0);
                                }
                            }
                        }

                        if (rootNode == null) {
                            rootNode = newNode;
                        }

                        if (indent > currentDepth) {
                            stack.push(lastNode);
                            parentNode = lastNode;
                            currentDepth = indent;
                        } else if (indent < currentDepth) {
                            while (indent < currentDepth) {
                                parentNode = stack.pop();
                                currentDepth -= 2;
                            }
                        }

                        if (parentNode != null) {
                            parentNode.add(newNode);
                        }
                        lastNode = newNode;
                    }
                }
            }
            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static IOKitNode getRoot() {
        return rootNode;
    }

    public static void main(String arg[]) {
        IOKit.parseRegistry();
        IOKitNode r = IOKit.getRoot();

        ArrayList<IOKitNode> list = IOKit.findByClass("IOSerialBSDClient");

        try {
            for (IOKitNode n : list) {
                System.out.print(n.get("IOTTYDevice") + " ");
                IOKitNode par = n.getParentNode();
                while (par != null) {
                    if (par.getNodeClass().equals("IOUSBDevice")) {
                        System.out.println(String.format("VID = 0x%04x PID = 0x%04x Vendor = %s Product = %s Serial = %s",
                            Integer.parseInt(par.get("idVendor")), Integer.parseInt(par.get("idProduct")), 
                            par.get("USB Vendor Name"), par.get("USB Product Name"), par.get("USB Serial Number")));
                        break;
                    }
                    par = par.getParentNode();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void printTree(IOKitNode n, int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print(" ");
        }
        System.out.print(n.toString());
        for (IOKitNode c : n.getChildren()) {
            printTree(c, depth + 2);
        }
    }

    public static ArrayList<IOKitNode> findByClass(String c) {
        ArrayList<IOKitNode> list = new ArrayList<IOKitNode>();

        huntForClass(rootNode, c, list);
        return list;
    }

    static void huntForClass(IOKitNode n, String c, ArrayList<IOKitNode> l) {
        if (n.getNodeClass().equals(c)) {
            l.add(n);
        }
        for (IOKitNode cn : n.getChildren()) {
            huntForClass(cn, c, l);
        }
    }
}
