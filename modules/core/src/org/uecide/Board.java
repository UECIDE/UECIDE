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

import java.util.regex.*;

import org.uecide.Compiler;
import org.uecide.Core;

public class Board extends UObject {

    public static TreeMap<String, Board> boards = new TreeMap<String, Board>();

    public Board(File folder) {
        super(folder);
        if (get("group") == null) {
            set("group", "Other");
        }
    }

    public File getBootloader() {
        String bl = get("bootloader");

        if(bl == null) {
            return null;
        }

        File bootloader = new File(getFolder(), bl);

        if(!bootloader.exists()) {
            return null;
        }

        return bootloader;
    }

    public String getGroup() {
        return get("group");
    }

    public File getManual() {
        String m = get("manual");

        if(m == null) {
            return null;
        }

        File mf = new File(getFolder(), m);

        if(!mf.exists()) {
            return null;
        }

        return mf;
    }

    public Core getCore() {
        String c = get("core");

        if(c == null) {
            Debug.message("Board has no core");
            return null;
        }
        Debug.message("Board's core is [" + c + "]");
        Core cc = org.uecide.Core.getCore(c);
        Debug.message("Found base core " + cc);
        return cc;
    }

    public static void load() {
        boards.clear();
        ArrayList<File> boardFiles = FileCache.getFilesByName("board.txt");
        for (File bfile : boardFiles) {
            if(bfile.exists()) {
                Debug.message("    Loading board " + bfile.getAbsolutePath());
                Board newBoard = new Board(bfile.getParentFile());

                if(newBoard.isValid()) {
                    boards.put(newBoard.getName(), newBoard);
                } else {
                    Debug.message("    ==> IS NOT VALID!!!");
                }
            }
        }
    }

    public static Board getBoard(String name) {
        return boards.get(name);
    }
}
