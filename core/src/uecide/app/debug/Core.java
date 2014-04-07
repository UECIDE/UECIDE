package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.*;

public class Core extends UObject implements MessageConsumer {

    public Core(File folder) {
        super(folder);
    }

    public void message(String m) {
        message(m, 1);
    }

    public void message(String m, int chan) {
        if (m.trim() != "") {
            if (chan == 2) {
                System.err.print(m);
            } else {
                System.out.print(m);
            }
        }
    }

    static public String[] headerListFromIncludePath(String path) {
        FilenameFilter onlyHFiles = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".h");
            }
        };

        return (new File(path)).list(onlyHFiles);
    }

    public File getManual() {
        String m = get("manual");
        if (m == null) {
            return null;
        }
        File mf = new File(getFolder(), m);
        if (!mf.exists()) {
            return null;
        }
        return mf;
    }

    public Compiler getCompiler() {
        String c = get("compiler");
        if (c == null) {
            return null;
        }
        return Base.compilers.get(c);
    }

}
