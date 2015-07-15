package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

public class vc_java implements VariableCommand {
    public String main(Context sketch, String args) {
        return System.getProperty(args);
    }
}
