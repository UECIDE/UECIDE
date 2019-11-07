package org.uecide;

import java.io.*;
import java.util.*;

public class PortListUpdaterService extends Service {

    public PortListUpdaterService() {
        setInterval(1000);
        setName("Port List Updater");
    }

    public void setup() {
        Serial.updatePortList();
    }

    public void cleanup() {
    }

    public void loop() {
        Serial.updatePortList();
    }

    public boolean isAutoStart() { return true; }
}
