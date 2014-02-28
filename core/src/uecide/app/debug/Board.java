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
//    public HashMap<String, String>optionsSelected = new HashMap<String, String>();
//    public HashMap<String, String>optionsFlags = new HashMap<String, String>();

    private File bootloader = null;

    public Board(File folder) {
        super(folder);
    }

    public File getBootloader() {
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

//    public void setOption(String root, String opt) {
//        optionsSelected.put(root, opt);
//    }
//
//    public boolean optionIsSet(String root, String opt) {
//        if (optionsSelected.get(root) == null) {
//            return false;
//        }
//        System.err.println(optionsSelected.get(root));
//        if (optionsSelected.get(root).equals(opt)) {
//            return true;
//        }
//        return false;
//    }
}
