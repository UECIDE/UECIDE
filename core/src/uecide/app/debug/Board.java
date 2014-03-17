package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.*;
import uecide.plugin.*;

import java.util.regex.*;

import uecide.app.Serial;
import uecide.app.SerialException;
import uecide.app.SerialNotFoundException;


public class Board extends UObject {
    public Board(File folder) {
        super(folder);
    }

    public File getBootloader() {
        String bl = get("bootloader");
        if (bl == null) {
            return null;
        }
        File bootloader = new File(getFolder(), bl);
        if (!bootloader.exists()) {
            return null;
        }
        return bootloader;
    }

    public String getGroup() {
        return get("group");
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

}
