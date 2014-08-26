package org.uecide.builtin;

import org.uecide.*;
import javax.script.*;
import java.io.*;

public class ecma implements BuiltinCommand {
    PipedReader stdo_pr;
    PipedWriter stdo_pw;
    PipedReader stde_pr;
    PipedWriter stde_pw;
    boolean running = true;
    Sketch sketch;
    public boolean main(Sketch sktch, String[] arg) {
        sketch = sktch;
        if (arg.length < 1) {
            sketch.error("Usage: __builtin_ecma::filename.js[::arg::arg...]");
            return false;
        }

        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            engine.put("sketch", sketch);
            File f = new File(arg[0]);
            if (!f.exists()) {
                sketch.error("File not found");
                return false;
            }
            StringBuilder sb = new StringBuilder();

            String[] args = new String[arg.length - 1];

            for (int i = 0; i < args.length; i++) {
                args[i] = arg[i+1];
            }

            FileInputStream fis = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

            String line = null;
            while((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }

            br.close();
            
            engine.eval(sb.toString());

            running = true;

            try {
                stdo_pr = new PipedReader();
                stdo_pw = new PipedWriter(stdo_pr);
                stde_pr = new PipedReader();
                stde_pw = new PipedWriter(stde_pr);
            } catch (Exception ex) {
            }

            engine.getContext().setWriter(stdo_pw);
            engine.getContext().setErrorWriter(stde_pw);

            Thread stdout = new Thread() {
                public void run() {
                    while (running) {
                        char[] tmp = new char[30];
                        try {
                            while (stdo_pr.ready()) {
                                int i = stdo_pr.read(tmp, 0, 20);
                                sketch.messageStream(new String(tmp, 0, i));
                            }
                        } catch (Exception ex) {
                        }
                    }
                }
            };
            stdout.start();

            Thread stderr = new Thread() {
                public void run() {
                    while (running) {
                        char[] tmp = new char[30];
                        try {
                            while (stde_pr.ready()) {
                                int i = stde_pr.read(tmp, 0, 20);
                                sketch.messageStream(new String(tmp, 0, i));
                                System.err.print(new String(tmp, 0, i));
                            }
                        } catch (Exception ex) {
                        }
                    }
                }
            };
            stderr.start();


            Invocable inv = (Invocable)engine;
            Boolean ret = (Boolean)inv.invokeFunction("run", (Object[])args);
            stdo_pw.flush();
            stde_pw.flush();
            running = false;
            return ret;
        } catch (Exception e) {
            sketch.error(e);
        }
        return false;
    }
}
