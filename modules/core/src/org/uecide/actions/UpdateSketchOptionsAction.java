package org.uecide.actions;

import org.uecide.Sketch;
import org.uecide.SketchFile;
import org.uecide.Context;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.TreeMap;

public class UpdateSketchOptionsAction extends Action {

    public UpdateSketchOptionsAction(Context c) { super(c); }

    public String[] getUsage() {
        return new String[] {
            "UpdateSketchOptions"
        };
    }

    public String getCommand() { return "updatesketchoptions"; }

    public boolean actionPerformed(Object[] args) throws ActionException {
        if (args.length != 0) {
            throw new SyntaxErrorActionException();
        }

        Sketch sketch = ctx.getSketch();

        SketchFile sf = sketch.getMainFile();

        String data = sf.getFileData();
        String[] lines = data.split("\n");

        TreeMap<String, String> opts = sketch.getOptionGroups();

        Pattern p = Pattern.compile("^\\s*#\\s*pragma\\s+option\\s+([^\\s]+)\\s*=\\s*([^\\s]+)\\s*$");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = p.matcher(line);
            if (m.find()) {
                if (opts.get(m.group(1)) != null) {
                    lines[i] = "#pragma option " + m.group(1) + "=" + sketch.getOption(m.group(1));
                    opts.remove(m.group(1));
                }
            }
        }

        StringBuilder out = new StringBuilder();

        for (String opt : opts.keySet()) {
            out.append("#pragma option " + opt + "=" + sketch.getOption(opt) + "\n");
        }

        for (int i = 0; i < lines.length; i++) {
            out.append(lines[i]);
            out.append("\n");
        }

        sf.setFileData(out.toString());

        return true;
    }
}
