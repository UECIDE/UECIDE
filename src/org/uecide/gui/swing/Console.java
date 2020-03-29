package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.Preferences;
import org.uecide.Message;

import javax.swing.JScrollPane;
import javax.swing.JTextField;

import java.awt.Font;
import java.awt.BorderLayout;

import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.wittams.gritty.swing.TermPanel;
import com.wittams.gritty.swing.GrittyTerminal;

public class Console extends TabPanel implements MouseWheelListener, ContextEventListener, KeyListener {
    Context ctx;

    GrittyTerminal terminal;
    ConsoleTty tty;
    TermPanel panel;
    JTextField input;

    boolean promptShown = false;

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

        input = new JTextField();

        input.setVisible(false);

        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        add(input, BorderLayout.SOUTH);

        input.addKeyListener(this);

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

    ArrayList<String> history = new ArrayList<String>();
    int historyPointer = 0;

    @Override
    public void keyPressed(KeyEvent evt) {
        int keyCode = evt.getExtendedKeyCode();

        if (keyCode == KeyEvent.VK_UP) {
            historyPointer++;
            if (historyPointer > history.size()) {
                historyPointer = history.size();
            }

            input.setText(history.get(history.size() - historyPointer));
        }

        if (keyCode == KeyEvent.VK_DOWN) {
            historyPointer--;
            if (historyPointer < 0) {
                historyPointer = 0;
            }

            if (historyPointer == 0) {
                input.setText("");
            } else {
                input.setText(history.get(history.size() - historyPointer));
            }
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            String command = input.getText().trim();
            input.setText("");

            if (command.equals("")) return;

            history.add(command);
            historyPointer = 0;

            final String regex = "\"([^\"]*)\"|(\\S+)";

            ArrayList<String> bits = new ArrayList<String>();

            Matcher m = Pattern.compile(regex).matcher(command);
            while (m.find()) {
                if (m.group(1) != null) {
                    bits.add(m.group(1));
                } else {
                    bits.add(m.group(2));
                }
            }

            String out = "";
            for (String s : bits) {
                if (out == "") {
                    out = s;
                } else {
                    out = out + "::" + s;
                }
            }
            out = ctx.parseString(out);
            String[] parts = out.split("::");
            if (parts.length == 1) {
                ctx.actionThread(parts[0]);
            } else {
                String[] args = new String[parts.length - 1];
                for (int i = 0; i < parts.length - 1; i++) {
                    args[i] = parts[i+1];
                }
                ctx.actionThread(parts[0], (Object[]) args);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent evt) {
    }

    @Override
    public void keyTyped(KeyEvent evt) {
    }

    public void showHideCommandPrompt() {
        promptShown = !promptShown;
        input.setVisible(promptShown);
        if (promptShown) {
            input.requestFocus();
        }
        revalidate();
    }
}
