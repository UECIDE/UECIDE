package uecide.app.varcmd;

import uecide.app.*;
import java.io.File;
import java.util.Map;

public class vc_env implements VariableCommand {
    public String main(Sketch sketch, String args) {
        Map<String, String> env = System.getenv();
        return env.get(args);
    }
}
