package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.Debug;
import org.uecide.Message;

import javax.swing.JScrollPane;

import java.util.concurrent.Semaphore;

public class OutputPanel extends TabPanel implements ContextEventListener {
    Output outputArea;
    Context ctx;
    Semaphore printerSem = new Semaphore(1);

    JScrollPane scroll;
    
    public OutputPanel(Context ctx, AutoTab t) {
        super("Output", t);
        this.ctx = ctx;
        outputArea = new Output(ctx);
        scroll = new JScrollPane(outputArea);
//        scroll.getViewport().setOpaque(false);
        add(scroll);
        ctx.listenForEvent("message", this);
        ctx.listenForEvent("buildStart", this);
    }
    
    @Override

    public void contextEventTriggered(ContextEvent evt) {
        if (evt.getEvent().equals("message")) {
            Message m = (Message)evt.getObject();

            try {
                printerSem.acquire();
                switch (m.getMessageType()) {
                    case Message.HEADING: outputArea.append(m.getText() + "\n", Output.HEADING); break;
                    case Message.BULLET1: outputArea.append(m.getText() + "\n", Output.BULLET); break;
                    case Message.BULLET2: outputArea.append(m.getText() + "\n", Output.BULLET2); break;
                    case Message.BULLET3: outputArea.append(m.getText() + "\n", Output.BULLET3); break;
// commands go to the console                    case Message.COMMAND: outputArea.append(m.getText() + "\n", Output.COMMAND); break;
                    case Message.NORMAL: outputArea.append(m.getText() + "\n", Output.BODY); break;
                    case Message.WARNING: outputArea.append(m.getText() + "\n", Output.WARNING); break;
                    case Message.ERROR: outputArea.append(m.getText() + "\n", Output.ERROR); break;
                    case Message.STREAM_MESSAGE: outputArea.append(m.getText(), Output.BODY); break;
                    case Message.STREAM_WARNING: outputArea.append(m.getText(), Output.WARNING); break;
                    case Message.STREAM_ERROR: outputArea.append(m.getText(), Output.ERROR); break;
                }
                printerSem.release();
            } catch (Exception ex) {
                Debug.exception(ex);
            }
        }

        if (evt.getEvent().equals("buildStart")) {
            outputArea.clear();
        }
    }
}
