package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.*;

public class Compiler extends UObject {
    public Compiler(File folder) {
        super(folder);
    }

    public String getErrorRegex() {
        String r = get("compiler.error");
        if (r == null) {
            r = "^([^:]+):(\\d+): error: (.*)";
        }
        return r;
    }

    public String getWarningRegex() {
        String r = get("compiler.warning");
        if (r == null) {
            r = "^([^:]+):(\\d+): warning: (.*)";
        }
        return r;
    }
}

