
package processing.app.tools;

import processing.app.*;
import processing.core.PApplet;

import java.io.*;

public class ExportToMPLABX implements Tool {
    Editor editor;

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
}
