package org.uecide;

import java.io.*;

public interface FileConverter {
    public boolean convertFile(File buildFolder);
    public File getFile();
    public String[] getHeaderLines();
}
