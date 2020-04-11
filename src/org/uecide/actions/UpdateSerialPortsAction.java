package org.uecide.actions;

import org.uecide.*;
import java.util.TreeMap;

public class UpdateSerialPortsAction extends Action {

    public UpdateSerialPortsAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "UpdateSerialPorts"
        };
    }

    public String getCommand() { return "updateserialports"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        Serial.updatePortList();
        return true;
    }
}
