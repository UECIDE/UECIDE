package org.uecide;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SketchFile implements Comparable {
    long bufferModified = 0;
    long fileModified = 0;
    String data;

    Context ctx;
    Sketch sketch;
    File file;

    HashMap<Integer, String> lineComments = new HashMap<Integer, String>();

    public SketchFile(Context c, Sketch s, File f) throws IOException {
        ctx = c;
        sketch = s;
        file = f;
        loadFileData();
    }

    public void loadFileData() throws IOException {
        if (!file.exists()) {
            // We're making a new file
            setFileData("");
            saveDataToDisk();
            return;
        }
        data = Utils.getFileAsString(file);
        fileModified = file.lastModified();
        bufferModified = fileModified;
    }

    public void setFileData(String d) {
        if (!(data.equals(d))) {
            data = d;
            bufferModified = System.currentTimeMillis();
            ctx.triggerEvent("SketchDataModified");
        }
    }

    public String getFileData() {
        return data;
    }

    public boolean isModified() {
        return bufferModified > fileModified;
    }

    public boolean isOutdated() {
        return bufferModified < fileModified;
    }

    public void saveDataToDisk() throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(file));
        pw.print(data);
        pw.close();
        fileModified = file.lastModified();
        bufferModified = fileModified;
    }

    public void saveDataToDisk(File out) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(out));
        pw.print(data);
        pw.close();
    }

    public File getFile() {
        return file;
    }

    public long getFileModified() {
        return fileModified;
    }

    public long getBufferModified() {
        return bufferModified;
    }

    public int getType() {
        return FileType.getType(file);
    }

    public int getGroup() {
        return FileType.getGroup(file);
    }

    public String getIcon() {
        return FileType.getIcon(file);
    }

    public String toString() {
        return file.getName();
    }

    public HashMap<Integer, String> getLineComments() {
        return lineComments;
    }

    public void setLineComment(int line, String comment) {
        lineComments.put(line, comment);
    }

    public void clearLineComments() {
        lineComments.clear();
    }

    public boolean isMainFile() {
        String fn = file.getName();
        String pn = file.getParentFile().getName();
        if (fn.equals(pn + ".ino")) return true;
        if (fn.equals(pn + ".pde")) return true;
        return false;
    }

    public String stripComments() {
        int cpos = 0;
        boolean inString = false;
        boolean inEscape = false;
        boolean inMultiComment = false;
        boolean inSingleComment = false;

        // We'll work through the string a character at a time pushing it on to the
        // string builder if we want it, or pushing a space if we don't.

        StringBuilder out = new StringBuilder();

        while (cpos < data.length()) {
            char thisChar = data.charAt(cpos);
            char nextChar = ' ';
            if (cpos < data.length() - 1) {
                nextChar = data.charAt(cpos + 1);
            }

            // Don't process any escaped characters - just add them verbatim.
            if (thisChar == '\\') {
                if (!inSingleComment && !inMultiComment)
                    out.append(thisChar);
                cpos++;
                if (cpos < data.length()) {
                    if (!inSingleComment && !inMultiComment)
                        out.append(data.charAt(cpos));
                    cpos++;
                }
                continue;
            }

            // If we're currently in a string then keep moving on until the end of the string.
            // If we hit the closing quote we still want to move on since it'll start a new
            // string otherwise.
            if (inString) {
                out.append(thisChar);
                if (thisChar == '"') {
                    inString = false;
                }
                cpos++;
                continue;
            }

            // If we're in a single line comment then keep skipping until we hit the end of the line.
            if (inSingleComment) {
                if (thisChar == '\n') {
                    out.append(thisChar);
                    inSingleComment = false;
                    cpos++;
                    continue;
                }
                cpos++;
                continue;
            }

            // If we're in a multi-line comment then keep skipping until we
            // hit the end of comment sequence.  Preserve newlines.
            if (inMultiComment) {
                if  (thisChar == '*' && nextChar == '/') {
                    inMultiComment = false;
                    cpos++;
                    cpos++;
                    continue;
                }
                if (thisChar == '\n') {
                    out.append(thisChar);
                    cpos++;
                    continue;
                }
                cpos++;
                continue;
            }

            // Is this the start of a quote?
            if (thisChar == '"') {
                out.append(thisChar);
                cpos++;
                inString = true;
                continue;
            }

            // How about the start of a single line comment?
            if (thisChar == '/' && nextChar == '/') {
                inSingleComment = true;
                out.append(" ");
                cpos++;
                continue;
            }

            // The start of a muti-line comment?
            if (thisChar == '/' && nextChar == '*') {
                inMultiComment = true;
                out.append(" ");
                cpos++;
                continue;
            }

            // None of those? Then let's just append.
            out.append(thisChar);
            cpos++;
        }

        return out.toString();
    }


    public ArrayList<FunctionBookmark> scanForFunctions() throws IOException {

        ctx.triggerEvent("fileDataRead");

        ArrayList<FunctionBookmark> protos = new ArrayList<FunctionBookmark>();

        Tool t = Base.getTool("ctags");
        if (t != null) {
            File tmp = new File(sketch.buildFolder, "tmp");
            if (!tmp.exists()) {
                tmp.mkdirs();
            }

            File tempSource = new File(tmp, file.getName());
            saveDataToDisk(tempSource);
            ctx.set("filename", file.getName());
            ctx.set("sketch.root", file.getParentFile().getAbsolutePath());
            ctx.set("build.root", sketch.buildFolder.getAbsolutePath());
            ctx.set("build.path", sketch.buildFolder.getAbsolutePath());
            ctx.set("tmp.root", tmp.getAbsolutePath());
            ctx.startBuffer(true);
            t.execute(ctx, "ctags.bookmark");
            ctx.endBuffer();

            File tags = new File(tmp, file.getName() + ".tags");
            if (tags.exists()) { // We got the tags
                String tagData = Utils.getFileAsString(tags);
                String[] tagLines = tagData.split("\n");

                Pattern p = Pattern.compile("^(.*)/\\^.*\\$/;\"(.*)$");

                for (String tagLine : tagLines) {
                    // Skip comments
                    if (tagLine.startsWith("!")) {
                        continue;
                    }

                    // Split the tag line into three parts with a regexp.  <anything>/^<ignored>$/;"<anything>
                    Matcher m = p.matcher(tagLine);
                    if (m.find()) {

                        String[] first = m.group(1).split("\t");
                        String itemName = first[0].trim();

                        String[] chunks = m.group(2).split("\t");
                        HashMap<String, String> params = new HashMap<String, String>();

                        for (String chunk : chunks) {
                            chunk = chunk.trim();
                            if (chunk.contains(":")) {
                                String[] bits = chunk.split(":", 2);
                                if (bits[0].equals("typeref")) {
                                    String[] b2 = bits[1].split(":");
                                    bits[1] = b2[1];
                                }

                                if (bits[0].equals("scope") && bits[1].startsWith("class:")) {
                                    String[] b2 = bits[1].split(":");
                                    bits[0] = "class";
                                    bits[1] = b2[1];
                                }

                                params.put(bits[0], bits[1]);
                            }
                        }

                        if (params.get("kind").equals("function")) { // Function
                            if (params.get("class") != null) { // Class member function
                                FunctionBookmark bm = new FunctionBookmark(
                                    FunctionBookmark.MEMBER_FUNCTION,
                                    this,
                                    Utils.s2i(params.get("line")),
                                    itemName,
                                    params.get("typeref"),
                                    params.get("signature"),
                                    params.get("class"),
                                    params.get("properties"),
                                    Utils.s2i(params.get("end"), Utils.s2i(params.get("line")))
                                );
                                protos.add(bm);
                            } else { // Global function
                                FunctionBookmark bm = new FunctionBookmark(
                                    FunctionBookmark.FUNCTION,
                                    this,
                                    Utils.s2i(params.get("line")),
                                    itemName,
                                    params.get("typeref"),
                                    params.get("signature"),
                                    null,
                                    params.get("properties"),
                                    Utils.s2i(params.get("end"), Utils.s2i(params.get("line")))
                                );
                                protos.add(bm);
                            }
                        } else if (params.get("kind").equals("variable")) { // Variable
                            FunctionBookmark bm = new FunctionBookmark(
                                FunctionBookmark.VARIABLE,
                                this,
                                Utils.s2i(params.get("line")),
                                itemName,
                                params.get("typeref"),
                                null,
                                null,
                                params.get("properties"),
                                Utils.s2i(params.get("end"), Utils.s2i(params.get("line")))
                            );
                            protos.add(bm);
                        } else if (params.get("kind").equals("define")) { // Preprocessor macro
                            FunctionBookmark bm = new FunctionBookmark(
                                FunctionBookmark.DEFINE,
                                this,
                                Utils.s2i(params.get("line")),
                                itemName,
                                null,
                                null,
                                null,
                                params.get("properties"),
                                Utils.s2i(params.get("end"), Utils.s2i(params.get("line")))
                            );
                            protos.add(bm);
                        } else if (params.get("kind").equals("class")) { // Class definition
                            FunctionBookmark bm = new FunctionBookmark(
                                FunctionBookmark.CLASS,
                                this,
                                Utils.s2i(params.get("line")),
                                itemName,
                                null,
                                null,
                                null,
                                params.get("properties"),
                                Utils.s2i(params.get("end"), Utils.s2i(params.get("line")))
                            );
                            protos.add(bm);
                        } else if (params.get("kind").equals("prototype")) { // Function prototype - may be a class instantiation
                            FunctionBookmark bm = new FunctionBookmark(
                                FunctionBookmark.VARIABLE,
                                this,
                                Utils.s2i(params.get("line")),
                                itemName,
                                params.get("typeref"),
                                null,
                                null,
                                params.get("properties"),
                                Utils.s2i(params.get("end"), Utils.s2i(params.get("line")))
                            );
                            protos.add(bm);
                        } else { // Something we don't know about
                        }
                    }
                }
            }
        }
        return protos;
    }


    String getReturnTypeFromProtoAndName(String proto, String name) {
        if (proto.startsWith("/^")) {
            String trimmed = proto.substring(2).trim();
            trimmed.replaceAll("\\s+", " ");
            int nameLoc = trimmed.indexOf(name);
            if (nameLoc == -1) return "";
            return trimmed.substring(0, nameLoc);
        }
        return "";
    }

    String getReturnTypeFromProtoAndSignature(String proto, String signature) {
        if (signature == null) return "";
        proto = proto.replaceAll("\\s+"," ");
        signature = signature.replaceAll("\\s+"," ");

        if (proto.startsWith("/^")) {
            proto = proto.substring(2).trim();
        }
        if (proto.endsWith("$/;\"")) {
            proto = proto.substring(0, proto.length() - 4);
        }
        proto = proto.trim();
        int sigpos = proto.indexOf(signature);
        if (sigpos > 0) {
            String front = proto.substring(0, sigpos);
            String[] parts = front.split("\\s+");
            String out = "";
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) out += " ";
                out += parts[i];
            }

            if (parts[parts.length-1].startsWith("*")) {
                out += "*";
            }
            return out;
        }
        return "";
    }

    public int compareTo(Object o) {
        if (!(o instanceof SketchFile)) return 0;
        return file.compareTo(((SketchFile)o).getFile());
    }

    public boolean renameFile(File newFile) {
        if (file.renameTo(newFile)) {
            file = newFile;
            return true;
        }
        return false;
    }

}
