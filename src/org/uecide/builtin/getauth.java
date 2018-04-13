/*
 * Copyright (c) 2018, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide.builtin;

import org.uecide.*;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

public class getauth implements BuiltinCommand {
    public boolean main(Context ctx, String[] arg) {

        String prompt = arg[0];
        String dest = arg[1];

        String value = "";
        String exist = Preferences.get(dest);

        if (Base.isHeadless()) {
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
