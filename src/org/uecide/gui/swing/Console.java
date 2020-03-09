package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.Preferences;
import org.uecide.Message;

import javax.swing.JScrollPane;

import java.awt.BorderLayout;

import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;

import com.wittams.gritty.swing.TermPanel;
import com.wittams.gritty.swing.GrittyTerminal;

public class Console extends TabPanel implements MouseWheelListener, ContextEventListener {
    Context ctx;

    GrittyTerminal terminal;
    ConsoleTty tty;
    TermPanel panel;

    public Console(Context c, AutoTab def) {
        super("Output", def);
        ctx = c;
        terminal = new GrittyTerminal();
        tty = new ConsoleTty(c);
        panel = terminal.getTermPanel();

        panel.setAntiAliasing(true);
        panel.setFont(Preferences.getFont("theme.console.fonts.command"));

        panel.addMouseWheelListener(this);

        terminal.setTty(tty);
        terminal.start();

        add(terminal, BorderLayout.CENTER);
        c.listenForEvent("message", this);
    }

    public void streamError(String e) {
        tty.feed("[31m" + e + "[0m");
    }

    public void error(String e) {
        tty.feed("[31m" + e + "[0m\r\n");
    }

    public void streamWarning(String e) {
        tty.feed("[33m" + e + "[0m");
    }
        
    public void warning(String e) {
        tty.feed("[33m" + e + "[0m\r\n");
    }
        
    public void streamMessage(String e) {
        tty.feed("[37m" + e + "[0m");
    }
        
    public void message(String e) {
        tty.feed("[37m" + e + "[0m\r\n");
    }
        
    public void command(String e) {
        tty.feed("[32m" + e + "[0m\r\n");
    }

    public void bullet(String e) {
        tty.feed("[37m * " + e + "[0m\r\n");
    }
        
    public void bullet2(String e) {
        tty.feed("[37m   > " + e + "[0m\r\n");
    }
        
    public void bullet3(String e) {
        tty.feed("[0m     o " + e + "[0m\r\n");
    }
        
    public void heading(String e) {
        tty.feed("[36m[1m[4m" + e + "[0m\r\n");
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        terminal.getScrollBar().setValue(
            terminal.getScrollBar().getValue() + (
                e.getScrollAmount() * e.getWheelRotation()
            )
        );
    }

    @Override
    public void contextEventTriggered(ContextEvent e) {
        if (e.getEvent().equals("message")) {
            Message m = (Message)e.getObject();

            switch (m.getMessageType()) {
                case Message.HEADING: heading(m.getText()); break;
                case Message.BULLET1: bullet(m.getText()); break;
                case Message.BULLET2: bullet2(m.getText()); break;
                case Message.BULLET3: bullet3(m.getText()); break;
                case Message.COMMAND: command(m.getText()); break;
                case Message.NORMAL: message(m.getText()); break;
                case Message.WARNING: warning(m.getText()); break;
                case Message.ERROR: error(m.getText()); break;
                case Message.STREAM_MESSAGE: tty.feed(m.getText()); break;
                case Message.STREAM_WARNING: tty.feed(m.getText()); break;
                case Message.STREAM_ERROR: tty.feed(m.getText()); break;
            }
        }
    }
}
