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

public class Compiler extends UObject {

    public static TreeMap<String, Compiler> compilers = new TreeMap<String, Compiler>();

    public Compiler(File folder) {
        super(folder);
    }

    public String getErrorRegex() {
        String r = get("compiler.error");

        if(r == null) {
            r = "^([^:]+):(\\d+): error: (.*)";
        }

        return r;
    }

    public String getWarningRegex() {
        String r = get("compiler.warning");

        if(r == null) {
            r = "^([^:]+):(\\d+): warning: (.*)";
        }

        return r;
    }

    public static void load() {
        compilers.clear();
        ArrayList<File> compilerFiles = FileCache.getFilesByName("compiler.txt");
        for (File cfile : compilerFiles) {
            if(cfile.exists()) {
                Debug.message("    Loading compiler " + cfile.getAbsolutePath());
                Compiler newCompiler = new Compiler(cfile.getParentFile());

                if(newCompiler.isValid()) {
                    compilers.put(newCompiler.getName(), newCompiler);
                } else {
                    Debug.message("    ==> IS NOT VALID!!!");
                }
            }
        }
    }

    public static Compiler getCompiler(String name) {
        return compilers.get(name);
    }

}


