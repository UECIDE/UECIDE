package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class gpio implements BuiltinCommand {
    public boolean main(Sketch sketch, String[] arg) {
        try {
            int pin = Integer.parseInt(arg[0]);
            String command = arg[1];

            if (command.equals("high")) {
                exportPin(pin);
                setMode(pin, "out");
                setValue(pin, 1);
            } else if (command.equals("low")) {
                exportPin(pin);
                setMode(pin, "out");
                setValue(pin, 0);
            } else if (command.equals("hiz")) {
                exportPin(pin);
                setMode(pin, "in");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    void exportPin(int pin) {
        try {
            File export = new File("/sys/class/gpio/export");
            if (!export.exists()) {
                return;
            }
            PrintWriter pw = new PrintWriter(export);
            pw.println(pin);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setMode(int pin, String dir) {
        try {
            String path = "/sys/class/gpio/gpio" + pin;
            File direction = new File(path, "direction");
            if (!direction.exists()) {
                return;
            }

            PrintWriter pw = new PrintWriter(direction);
            pw.println(dir);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setValue(int pin, int val) {
        try {
            String path = "/sys/class/gpio/gpio" + pin;
            File value = new File(path, "value");
            if (!value.exists()) {
                return;
            }

            PrintWriter pw = new PrintWriter(value);
            pw.println(value);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
