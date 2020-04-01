package org.uecide;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsFile;
import de.waldheinz.fs.util.FileDisk;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.fat.FatType;
import de.waldheinz.fs.fat.SuperFloppyFormatter;
import java.awt.Desktop;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Arrays;

public class Utils {
    public static Image getScaledImage(Image srcImg, int w, int h){
        Image rescaled = srcImg.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
        return rescaled;
    }

    public static boolean s2b(String s) {
        if (s == null) return false;
        if (s.equals("true")) return true;
        if (s.equals("t")) return true;
        if (s.equals("yes")) return true;
        if (s.equals("y")) return true;
        return false;
    }

    public static long s2l(String s) {
        try {
            return Long.decode(s);
        } catch (Exception e) {
            Debug.exception(e);
        }
        return 0;
    }

    public static int s2i(String s) {
        try {
            return Integer.decode(s);
        } catch (Exception e) {
            Debug.exception(e);
        }
        return 0;
    }

    public static int s2i(String s, int d) {
        if (s == null) return d;
        try {
            return Integer.decode(s);
        } catch (Exception e) {
            Debug.exception(e);
        }
        return d;
    }

    public static float s2f(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            Debug.exception(e);
        }
        return 0.0f;
    }

    public static void browse(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                Debug.exception(ex);
                ex.printStackTrace();
            }
        }
    }

    public static void open(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(new File(url));
            } catch (Exception ex) {
                Debug.exception(ex);
            }
        }
    }

    public static void copyDir(File sourceDir, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                UECIDE.error("Unable to make target folder " + targetDir.getAbsolutePath());
                return;
            }
        }
        String files[] = sourceDir.list();

        for(int i = 0; i < files.length; i++) {
            // Ignore dot files (.DS_Store), dot folders (.svn) while copying
            if(files[i].charAt(0) == '.') continue;

            //if (files[i].equals(".") || files[i].equals("..")) continue;
            File source = new File(sourceDir, files[i]);
            File target = new File(targetDir, files[i]);

            if(source.isDirectory()) {
                //target.mkdirs();
                copyDir(source, target);
                target.setLastModified(source.lastModified());
            } else {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }


    public static byte[] loadBytesRaw(File file) throws FileNotFoundException, IOException {
        FileInputStream input = new FileInputStream(file);

        int size = (int) file.length();
        byte buffer[] = new byte[size];
        int offset = 0;
        int bytesRead;

        while((bytesRead = input.read(buffer, offset, size - offset)) != -1) {
            offset += bytesRead;
            if(bytesRead == 0) break;
        }

        input.close();

        return buffer;
    }

    public static String getFileAsString(File f) throws IOException, FileNotFoundException {
        if (f == null) {
            return "";
        }
        if (!f.exists()) {
            return "";
        }
        if (f.isDirectory()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(f));
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        reader.close();
        return sb.toString();

    }

    public static String getResourceAsString(String resource) throws IOException {
        String out = "";
        InputStream from = Utils.class.getResourceAsStream(resource);
        int bytesRead;

        StringBuilder sb = new StringBuilder();

        String line = null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(from));
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        out = sb.toString();

        reader.close();
        from.close();
        return out;
    }

    public static void copyResourceToFile(String res, File dest) throws IOException {
        InputStream from = Utils.class.getResourceAsStream(res);
        OutputStream to = new BufferedOutputStream(new FileOutputStream(dest));
        byte[] buffer = new byte[16 * 1024];
        int bytesRead;

        while((bytesRead = from.read(buffer)) != -1) {
            to.write(buffer, 0, bytesRead);
        }

        from.close();
        to.close();
    }

    public static long getFolderSize(File root) {
        long total = 0;
        File[] files = root.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                total += getFolderSize(file);
            } else {
                total += file.length();
            }
        }
        return total;
    }

    public static void addFilesToImage(FsDirectory dir, File srcdir) throws IOException, FileNotFoundException {
        if (srcdir == null) return;
        File[] files = srcdir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                FsDirectoryEntry newDir = dir.addDirectory(file.getName());
                FsDirectory nd = newDir.getDirectory();
                addFilesToImage(nd, file);
                nd.flush();
            } else {
                FsDirectoryEntry newFile = dir.addFile(file.getName());
                FsFile f = newFile.getFile();
                f.setLength(file.length());


                RandomAccessFile input = new RandomAccessFile(file, "r");
                FileChannel inChannel = input.getChannel();
                long fileSize = inChannel.size();
                ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
                inChannel.read(buffer);
                buffer.flip();
                f.write(0, buffer);
                f.flush();
            }
        }
    }
        

    public static boolean buildFATImage(File root, File dest, long size, FatType type, String volname) {
        try {
            if (size == 0) {
                long used = getFolderSize(root);
                used *= 1.1; // 10% extra space
                used += (512 * 32); // 32 blocks for system stuff
                used /= 512;
                used++;
                used *= 512;
                size = used;
            }

            BlockDevice dev = FileDisk.create(dest, size);
            FatFileSystem fs = SuperFloppyFormatter.get(dev).setFatType(type).format();

            addFilesToImage(fs.getRoot(), root);

            fs.close();

        } catch (Exception e) {
            Debug.exception(e);
            UECIDE.error(e);
            return false;
        }

        return true;
    }

    public static long millis() {
        Date d = new Date();
        return d.getTime();
    }

    public static void deltree(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File file : files) {
                deltree(file);
            }
        }
        System.err.println("Deleting " + f);
        f.delete();
    }

    final static int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

    static {
        Arrays.sort(illegalChars);
    }

    public static String sanitize(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        int len = badFileName.codePointCount(0, badFileName.length());
        for (int i=0; i<len; i++) {
            int c = badFileName.codePointAt(i);
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.appendCodePoint(c);
            }
        }

        String name = cleanName.toString();
        name = name.toLowerCase();
        name = name.replaceAll("\\s+", "_");
        name = name.replaceAll(",", "_");
        name = name.replaceAll("\\.", "_");
        return name;
    }

}
