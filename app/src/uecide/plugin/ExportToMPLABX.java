
package uecide.plugin;

import uecide.app.*;
import processing.core.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;


public class ExportToMPLABX extends BasePlugin {

    public String getMenuTitle() {
        return "Export to MPLAB-X";
    }

    public void run() {
        try {
            System.err.println(this.getClass().getClassLoader());
            InputStream in = loader.getResourceAsStream("uecide/plugin/test.txt");
            if (in == null) {
                System.err.println("FAIL!!!");
            } else {
                byte[] buffer = new byte[1024];
                while (in.read(buffer)>0) {
                    String res = new String(buffer);
                    System.out.print(res);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
