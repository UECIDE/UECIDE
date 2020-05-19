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

import java.io.*;
import java.util.*;
import javax.swing.*;

import org.uecide.Compiler;

public class Core extends UObject {

    public static TreeMap<String, Core> cores = new TreeMap<String, Core>();

    public Core(File folder) {
        super(folder);
    }

    static public String[] headerListFromIncludePath(String path) {
        FilenameFilter onlyHFiles = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".h");
            }
        };

        return (new File(path)).list(onlyHFiles);
    }

    public Compiler getCompiler() {
        String c = get("compiler");

        if(c == null) {
            return null;
        }

        return org.uecide.Compiler.getCompiler(c);
    }

    public static void load() {
        cores.clear();
        ArrayList<File> coreFiles = FileCache.getFilesByName("core.txt");
        for (File cfile : coreFiles) {
            if(cfile.exists()) {
                Debug.message("    Loading core " + cfile.getAbsolutePath());
                Core newCore = new Core(cfile.getParentFile());

                if(newCore.isValid()) {
                    cores.put(newCore.getName(), newCore);
                } else {
                    Debug.message("    ==> IS NOT VALID!!!");
                }
            }
        }
    }

    public static Core getCore(String name) { 
        return cores.get(name); 
    }
}
