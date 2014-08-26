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
    public Version clone() {
        Version out = new Version(null);
        out.chunks = new ArrayList<Integer>(chunks);
        return out;
    }

    public String toString() {
        return versionString;
    }
};
