package org.uecide.builtin;

import org.uecide.*;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

public class getauth extends BuiltinCommand {
    public getauth(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {

        String prompt = arg[0];
        String dest = arg[1];

        String value = "";
        String exist = Preferences.get(dest);

        if (UECIDE.isHeadless()) {
            LineReader reader = LineReaderBuilder.builder().build();
            System.out.print(prompt + ": ");
            String in = reader.readLine();
            if (in == "") {
                value = exist;
            } else {
                value = null;
            }
            ctx.set(dest, value);
            Preferences.set(dest, value);
            return true;
        }


        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        JLabel label = new JLabel(prompt);
        JPasswordField pass = new JPasswordField(exist);
        panel.add(label);
        panel.add(pass);
        String[] options = new String[]{"OK", "Cancel"};


        pass.addAncestorListener( new AncestorListener() {
          @Override
          public void ancestorRemoved( final AncestorEvent event ) {
          }
          @Override
          public void ancestorMoved( final AncestorEvent event ) {
          }
          @Override
          public void ancestorAdded( final AncestorEvent event ) {
            // Ask for focus (we'll lose it again)
            pass.requestFocusInWindow();
          }
        } );

        pass.addFocusListener( new FocusListener() {
          @Override
          public void focusGained( final FocusEvent e ) {
          }
          @Override
          public void focusLost( final FocusEvent e ) {
            if( isFirstTime ) {
              // When we lose focus, ask for it back but only once
              pass.requestFocusInWindow();
              isFirstTime = false;
            }
          }
          private boolean isFirstTime = true;
        } );
   
        int option = JOptionPane.showOptionDialog(null, panel, "Authentication",
                                 JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                                 null, options, options[0]);
        if(option == 0) // pressing OK button
        {
            char[] password = pass.getPassword();
            String p = new String(password);
            ctx.set(dest, p);
            Preferences.set(dest, p);
            return true;
        }

        ctx.error("Authentication Cancelled");
        return false;
    }

    public void kill() {
    }
}
