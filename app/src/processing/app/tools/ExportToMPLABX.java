
package processing.app.tools;

import processing.app.*;
import processing.core.PApplet;

import java.io.*;
import java.util.*;

public class ExportToMPLABX implements Tool {
    Editor editor;
    Map pluginInfo;
    public void setInfo(Map info) { pluginInfo = info; }


    public void init(Editor editor) {
        this.editor = editor;
    }


    public String getMenuTitle() {
        return "Export to MPLAB-X";
    }

    public void run() {
        System.err.println(
            editor.getSketch().getName()
        );
    }
  public String getVersion() { return (String) pluginInfo.get("version"); }
  public String getCompiled() { return (String) pluginInfo.get("compiled"); }

}
