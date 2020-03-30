package org.uecide;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import de.waldheinz.fs.*;
import de.waldheinz.fs.fat.*;
import de.waldheinz.fs.util.*;

public class Utils {
    public static Image getScaledImage(Image srcImg, int w, int h){
        Image rescaled = srcImg.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
        return rescaled;
//        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g2 = resizedImg.createGraphics();
//
//        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//        g2.drawImage(srcImg, 0, 0, w, h, null);
//        g2.dispose();
//
 //       return resizedImg;
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
            return Long.parseLong(s);
        } catch (Exception e) {
            Base.exception(e);
        }
        return 0;
    }

    public static int s2i(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            Base.exception(e);
        }
        return 0;
    }

    public static float s2f(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            Base.exception(e);
        }
        return 0.0f;
    }

    public static void browse(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                Base.exception(ex);
            }
        }
    }

    public static void open(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(new File(url));
            } catch (Exception ex) {
                Base.exception(ex);
            }
        }
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
            Base.exception(e);
            Base.error(e);
            return false;
        }

        return true;
    }
}
