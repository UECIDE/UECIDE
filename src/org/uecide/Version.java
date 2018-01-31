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

// A version string is a strange beast.  It's essentially a chain
// of mixed-base numbers separated by some other character.
// A mixed base number is a number where different characters within
// it reperesent values in different bases.  For example, the number
// "34b" has two base-10 numbers (3 and 4) and a base-26 number (b = 2).

public class Version implements Comparable, Cloneable {
    public ArrayList<Integer> chunks;
    public String versionString;

    public Version(String data) {
        if(data == null) {
            data = "0.0.0a";
        }

        try {
            versionString = data;
            chunks = new ArrayList<Integer>();

            if(data != null) {
                // First, let's standardize any separators
                data = data.replaceAll("-", ".");
                data = data.replaceAll("_", ".");
                data = data.replaceAll("pl", ".");
                data = data.replaceAll("rev", ".");

                // Now let's split it into bits
                String[] parts = data.split("\\.");

                // And iterate through it all cleaning the data and converting it to numbers
                for(String part : parts) {
                    int val = 0;
                    char[] letters = part.toCharArray();

                    for(char letter : letters) {
                        if(letter >= '0' && letter <= '9') {
                            val = val * 10;
                            val += (letter - '0');
                        } else if(letter >= 'a' && letter <= 'z') {
                            val = val * 26;
                            val += (letter - 'a');
                        } else if(letter >= 'A' && letter <= 'Z') {
                            val = val * 26;
                            val += (letter - 'A');
                        }
                    }

                    chunks.add(val);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public int compareTo(Object o) {
        Version v = (Version)o;
        int i = 0;

        for(i = 0; i < chunks.size(); i++) {

            int targetValue = 0;

            if(i < (v.chunks.size())) {
                targetValue = v.chunks.get(i);
            }

            if(chunks.get(i) < targetValue) {
                return -1;
            }

            if(chunks.get(i) > targetValue) {
                return 1;
            }
        }

        return 0;
    }

    @Override
    public Version clone() throws CloneNotSupportedException {
        super.clone();
        Version out = new Version(null);
        out.chunks = new ArrayList<Integer>(chunks);
        return out;
    }

    public String toString() {
        return versionString;
    }
};
