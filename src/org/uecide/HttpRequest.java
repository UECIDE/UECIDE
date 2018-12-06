package org.uecide;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class HttpRequest {
    URL url;

    public HttpRequest(String u) throws MalformedURLException {
        url = new URL(u);
    }

    public HttpRequest(URL u) {
        url = u;
    }

    public String getText() throws IOException {
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-Agent", "UECIDE/" + Base.systemVersion.toString() + " (" + Base.getOSFullName() + "; Java " + System.getProperty("java.version") + ")");
        conn.connect();
        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder o = new StringBuilder();
        String in;
        while ((in = r.readLine()) != null) {
            o.append(in);
            o.append("\n");
        }
        r.close();
        return o.toString();
    }

    public String getCompressedText() throws IOException {
        StringBuilder inData = new StringBuilder();
        byte[] buffer = new byte[1024];
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-Agent", "UECIDE/" + Base.systemVersion.toString() + " (" + Base.getOSFullName() + "; Java " + System.getProperty("java.version") + ")");
        conn.connect();
        int contentLength = conn.getContentLength();

        InputStream rawIn = (InputStream)conn.getInputStream();
        GZIPInputStream in = new GZIPInputStream(rawIn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        in.close();
        inData.append(out.toString("UTF-8"));
        return inData.toString();
    }
}
