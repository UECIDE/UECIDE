/*
 * Copyright (c) 2017, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;

import java.security.MessageDigest;

import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;


import org.apache.commons.compress.archivers.ar.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.*;
import org.apache.commons.compress.compressors.xz.*;
import org.apache.commons.compress.compressors.bzip2.*;

public class DebFile {

    static final int DIRECTORY = 0;
    static final int FILE = 1;
    static final int LINK = 2;

    File debFile;
    File tempLoc;

    static class FileInfo {
        int type;
        File source;
    }

    DebFile(File f) {
        debFile = f;
    }

    // Extract a file into root adding the control files to db.
    public void extract(File db, File root) throws IOException {
        // Step one, extract the files from the deb.
        HashMap<String, File> files = extractOuterFiles();
        HashMap<File, FileInfo> pendingFiles = null;

        // Step two, extract the data to the filesystem with temporary names
        if (files.get("data.tar.gz") != null) pendingFiles = extractTarGzFile(files.get("data.tar.gz"), root);
        if (files.get("data.tar.xz") != null) pendingFiles = extractTarXzFile(files.get("data.tar.xz"), root);
        if (files.get("data.tar.bz2") != null) pendingFiles = extractTarBz2File(files.get("data.tar.bz2"), root);

        // Step three, rename the files to the proper names and
        // copy any linked files (windows doesn't do links)
        installFiles(pendingFiles);

        // Step four, extract the control information
        if (files.get("control.tar.gz") != null) pendingFiles = extractTarGzFile(files.get("control.tar.gz"), db);
        if (files.get("control.tar.xz") != null) pendingFiles = extractTarXzFile(files.get("control.tar.xz"), db);
        if (files.get("control.tar.bz2") != null) pendingFiles = extractTarBz2File(files.get("control.tar.bz2"), db);


        installFiles(pendingFiles);
        
        // Step five, remove the temporary files
        cleanup();
    }

    public String getPackageName() throws IOException, FileNotFoundException {
        HashMap<String, File> files = extractOuterFiles();
        HashMap<File, FileInfo> pendingFiles = null;
        File db = Base.createTempFolder("udeb_");
        if (files.get("control.tar.gz") != null) pendingFiles = extractTarGzFile(files.get("control.tar.gz"), db);
        if (files.get("control.tar.xz") != null) pendingFiles = extractTarXzFile(files.get("control.tar.xz"), db);
        if (files.get("control.tar.bz2") != null) pendingFiles = extractTarBz2File(files.get("control.tar.bz2"), db);

        File c = new File(db, "control.udeb-new");
        String packageName = null;
        if (c.exists()) {
            String data = Utils.getFileAsString(c);
            String[] lines = data.split("\n");
            for (String line : lines) {
                String[] bits = line.split(" ");
                if (bits[0].equals("Package:")) {
                    packageName = bits[1];
                }
            }
            Base.removeDir(db);
        }

        return packageName;
    }

    void installFiles(HashMap<File, FileInfo> files) throws IOException {
        // First do the file renaming
        for (Map.Entry<File, FileInfo> dest : files.entrySet()) {
            FileInfo fi = dest.getValue();
            File d = dest.getKey();
            if (fi.type == FILE) {
                // Remove the old file if it's there
                if (d.exists()) {
                    Base.tryDelete(d);
                }
                fi.source.renameTo(d);
            }
        }

        // Now copy linked files
        for (Map.Entry<File, FileInfo> dest : files.entrySet()) {
            FileInfo fi = dest.getValue();
            File d = dest.getKey();
            if (fi.type == LINK) {
                if (d.exists()) {
                    Base.tryDelete(d);
                }
                Files.copy(fi.source.toPath(), d.toPath(), REPLACE_EXISTING);
                d.setExecutable(fi.source.canExecute());
                d.setWritable(fi.source.canRead());
                d.setReadable(fi.source.canWrite());
            }
        }
            
    }

    HashMap<String, File> extractOuterFiles() throws IOException {
        HashMap<String, File> filelist = new HashMap<String, File>();
        tempLoc = Base.createTempFolder("udeb_");
        FileInputStream fis = new FileInputStream(debFile);
        ArArchiveInputStream ar = new ArArchiveInputStream(fis);
        ArArchiveEntry file = ar.getNextArEntry();
        while (file != null) {
            long size = file.getSize();
            String name = file.getName();
            File dest = new File(tempLoc, name);

            if (file.isDirectory()) {
                dest.mkdirs();
                dest.setLastModified(file.getLastModified());
            } else {
                InputStream from = new BufferedInputStream(ar);
                OutputStream to = new BufferedOutputStream(new FileOutputStream(dest));
                byte[] buffer = new byte[16 * 1024];
                int bytesRead;

                while((bytesRead = from.read(buffer)) != -1) {
                    to.write(buffer, 0, bytesRead);
                }

                to.flush();
                to.close();
                //from.close();
                dest.setLastModified(file.getLastModified());
                filelist.put(name, dest);
            }
            file = ar.getNextArEntry();
        }
        ar.close();
        return filelist;
    }

    void cleanup() {
        Base.removeDir(tempLoc);
        tempLoc = null;
    }

   HashMap<File, FileInfo> extractTarGzFile(File src, File root) throws IOException {
        GzipCompressorInputStream gzip = new GzipCompressorInputStream(new FileInputStream(src));
        TarArchiveInputStream tar = new TarArchiveInputStream(gzip);
        return extractTarFile(tar, root);
    }

    HashMap<File, FileInfo> extractTarXzFile(File src, File root) throws IOException {
        XZCompressorInputStream xzip = new XZCompressorInputStream(new FileInputStream(src));
        TarArchiveInputStream tar = new TarArchiveInputStream(xzip);
        return extractTarFile(tar, root);
    }

    HashMap<File, FileInfo> extractTarBz2File(File src, File root) throws IOException {
        BZip2CompressorInputStream bzip = new BZip2CompressorInputStream(new FileInputStream(src));
        TarArchiveInputStream tar = new TarArchiveInputStream(bzip);
        return extractTarFile(tar, root);
    }

    HashMap<File, FileInfo> extractTarFile(TarArchiveInputStream tar, File root) throws IOException {
        HashMap<File, FileInfo> files = new HashMap<File, FileInfo>();

        TarArchiveEntry te = tar.getNextTarEntry();
        while (te != null) {

            File dest = new File(root, te.getName());

            if (te.isDirectory()) {
                dest.mkdirs();
            } else if (te.isLink() || te.isSymbolicLink()) {
                FileInfo fi = new FileInfo();
                fi.type = LINK;
                if (te.getLinkName().startsWith("./") || te.getLinkName().startsWith("/")) {
                    fi.source = new File(root, te.getLinkName());
                } else {
                    fi.source = new File(dest.getParentFile(), te.getLinkName());
                }
                files.put(dest, fi);
            } else {
                File tempDest = new File(root, te.getName() + ".udeb-new");
                byte[] buffer = new byte[1024];
                int nread;
                int toRead = (int)te.getSize();
                FileOutputStream fos = new FileOutputStream(tempDest);
                while ((nread = tar.read(buffer, 0, toRead > 1024 ? 1024 : toRead)) > 0) {
                    toRead -= nread;
                    fos.write(buffer, 0, nread);
                }
                fos.close();
                tempDest.setExecutable((te.getMode() & 0100) == 0100);
                tempDest.setWritable((te.getMode() & 0200) == 0200);
                tempDest.setReadable((te.getMode() & 0400) == 0400);
                FileInfo fi = new FileInfo();
                fi.type = FILE;
                fi.source = tempDest;
                files.put(dest, fi);
            }

            te = tar.getNextTarEntry();
        }

        return files;
    }

/*
                } else {
                    try {
                        }
                        installedFiles.put(dest.getAbsolutePath(), tsize);
                    } catch (Exception fex) {
                        Base.error("Error extracting " + dest + ": " + fex + " (ignoring)");
                    }
                }
                te = tar.getNextTarEntry();
            }
            for (String link : symbolicLinks.keySet()) {
                String tgt = symbolicLinks.get(link);
                File linkFile = new File(root, link);
                File linkParent = linkFile.getParentFile();
                File troot = root;
                if (!tgt.startsWith("./")) {
                    troot = linkParent;
                }
                File tgtFile = new File(troot, tgt); //linkParent, tgt);
                FileInputStream copyFrom = new FileInputStream(tgtFile);
                FileOutputStream copyTo = new FileOutputStream(linkFile);
                byte[] copyBuffer = new byte[1024];
                int bytesCopied = 0;

                while ((bytesCopied = copyFrom.read(copyBuffer, 0, 1024)) > 0) {
                    copyTo.write(copyBuffer, 0, bytesCopied);
                }
                copyFrom.close();
                copyTo.close();
                linkFile.setExecutable(tgtFile.canExecute());
                linkFile.setReadable(tgtFile.canRead());
                linkFile.setWritable(tgtFile.canWrite());
            }
        } catch (Exception e) {
            Base.error(e);
        }
        return installedFiles;
    }
*/

}
