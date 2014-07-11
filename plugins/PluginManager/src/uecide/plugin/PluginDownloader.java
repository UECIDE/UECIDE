package uecide.plugin;

import uecide.app.*;
import uecide.app.debug.*;
import uecide.app.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;


public class PluginDownloader extends QueueWorker {

    URL downloadFrom = null;
    File downloadTo = null;
    long contentLength = 0;
    PluginEntry entry;

    public PluginDownloader(PluginEntry pe) {
        entry = pe;
    }

    @Override
    public String getTaskName() {
        return entry.toString();
    }

    @Override
    public String getQueuedDescription() {
        return "Download pending";
    }

    @Override
    public String getActiveDescription() {
        return "Downloading";
    }

    @Override
    public Void doInBackground() {
        try {
            downloadFrom = entry.getURL();
            downloadTo = entry.getDownloadFile();
            HttpURLConnection httpConn = (HttpURLConnection) downloadFrom.openConnection();
            contentLength = httpConn.getContentLength();
            InputStream in = httpConn.getInputStream();
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(downloadTo));

            byte[] buffer = new byte[1024];
            int n;
            long tot = 0;
            while ((n = in.read(buffer)) > 0) {
                tot += n;
                Long[] l = new Long[1];
                if (contentLength == -1) {
                    l[0] = new Long(0);
                    publish(l);
                } else {
                    l[0] = (tot * 100) / contentLength;
                    publish(l);
                }
                out.write(buffer, 0, n);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            Base.error(e);
        }
        return null;
    }

    @Override
    public void done() {
    }
}
