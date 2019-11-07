package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Preferences;

import javax.swing.JScrollPane;

import java.awt.BorderLayout;

import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;

import com.wittams.gritty.swing.TermPanel;
import com.wittams.gritty.swing.GrittyTerminal;

public class Console extends TabPanel implements MouseWheelListener {
    Context ctx;

    GrittyTerminal terminal;
    ConsoleTty tty;
    TermPanel panel;

    public Console(Context c) {
        super("Output");
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
    }

    public void error(String e) {
        tty.feed("[31m" + e + "[0m\r\n");
    }

    public void warning(String e) {
        tty.feed("[33m" + e + "[0m\r\n");
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
}
