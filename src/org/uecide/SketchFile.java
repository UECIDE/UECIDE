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
        data = d;
        bufferModified = System.currentTimeMillis();
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

        ArrayList<FunctionBookmark> protos = new ArrayList<FunctionBookmark>();

        Tool t = Base.getTool("ctags");
        if (t != null) {
            Pattern pat = Pattern.compile("\\^([^\\(]+)\\(");
            ctx.set("filename", file.getName());
            ctx.set("sketch.root", file.getParentFile().getAbsolutePath());
            ctx.set("build.root", sketch.buildFolder.getAbsolutePath());
            ctx.set("build.path", sketch.buildFolder.getAbsolutePath());
            ctx.startBuffer(true);
            t.execute(ctx, "ctags.parse.ino");
            ctx.endBuffer();

            File tags = new File(sketch.buildFolder, file.getName() + ".tags");
            if (tags.exists()) { // We got the tags
                String tagData = Utils.getFileAsString(tags);
                String[] tagLines = tagData.split("\n");

                for (String tagLine : tagLines) {

                    String[] chunks = tagLine.split("\t");

                    if (chunks[0].startsWith("!")) continue;

                    String itemName = chunks[0].trim();
                    String fileName = chunks[1].trim();
                    String objectType = chunks[3].trim();

                    HashMap<String, String> params = new HashMap<String, String>();

                    for (int i = 4; i < chunks.length; i++) {
                        String[] parts = chunks[i].split(":", 2);
                        if (parts.length == 2) {
                            params.put(parts[0], parts[1]);
                        }
                    }


                    if (objectType.equals("f")) { // Function
                        if (params.get("class") != null) { // Class member function
                            String returnType = getReturnTypeFromProtoAndSignature(chunks[2], params.get("signature"));
                            if ((returnType != null) && (!returnType.equals(""))) {
                                if (itemName.indexOf("::") > 0) {
                                    itemName = itemName.substring(itemName.indexOf("::") + 2);
                                }
                                FunctionBookmark bm = new FunctionBookmark(
                                    FunctionBookmark.MEMBER_FUNCTION,
                                    sketch.translateBuildFileToSketchFile(fileName),
                                    Utils.s2i(params.get("line")),
                                    itemName,
                                    returnType,
                                    params.get("signature"),
                                    params.get("class")
                                );
                                protos.add(bm);
                            }
                        } else { // Global function
                            String returnType = getReturnTypeFromProtoAndSignature(chunks[2], params.get("signature"));
                            FunctionBookmark bm = new FunctionBookmark(
                                FunctionBookmark.FUNCTION,
                                sketch.translateBuildFileToSketchFile(fileName),
                                Utils.s2i(params.get("line")),
                                itemName,
                                returnType,
                                params.get("signature"),
                                null
                            );
                            protos.add(bm);
                        }
                    } else if (objectType.equals("v")) { // Variable
                        String returnType = getReturnTypeFromProtoAndName(chunks[2], itemName);
                        FunctionBookmark bm = new FunctionBookmark(
                            FunctionBookmark.VARIABLE,
                            sketch.translateBuildFileToSketchFile(fileName),
                            Utils.s2i(params.get("line")),
                            itemName,
                            returnType,
                            null,
                            null
                        );
                        protos.add(bm);
                    } else if (objectType.equals("m")) { // Class member variable
                        String returnType = getReturnTypeFromProtoAndName(chunks[2], itemName);
                        FunctionBookmark bm = new FunctionBookmark(
                            FunctionBookmark.MEMBER_VARIABLE,
                            sketch.translateBuildFileToSketchFile(fileName),
                            Utils.s2i(params.get("line")),
                            itemName,
                            returnType,
                            params.get("class"),
                            null
                        );
                        protos.add(bm);
                    } else if (objectType.equals("d")) { // Preprocessor macro
                        FunctionBookmark bm = new FunctionBookmark(
                            FunctionBookmark.DEFINE,
                            sketch.translateBuildFileToSketchFile(fileName),
                            Utils.s2i(params.get("line")),
                            itemName,
                            null,
                            null,
                            null
                        );
                        protos.add(bm);
                    } else if (objectType.equals("c")) { // Class definition
                        FunctionBookmark bm = new FunctionBookmark(
                            FunctionBookmark.CLASS,
                            sketch.translateBuildFileToSketchFile(fileName),
                            Utils.s2i(params.get("line")),
                            itemName,
                            null,
                            null,
                            null
                        );
                        protos.add(bm);
                    } else if (objectType.equals("p")) { // Function prototype - may be a class instantiation
                        String returnType = getReturnTypeFromProtoAndSignature(chunks[2], params.get("signature"));
                        FunctionBookmark bm = new FunctionBookmark(
                            FunctionBookmark.VARIABLE,
                            sketch.translateBuildFileToSketchFile(fileName),
                            Utils.s2i(params.get("line")),
                            itemName,
                            returnType,
                            null,
                            null
                        );
                        protos.add(bm);
                    } else { // Something we don't know about
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

}
