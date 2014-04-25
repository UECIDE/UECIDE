/*
 * Copyright (c) 2014, Majenko Technologies
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

package uecide.app;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;


class FindAndReplace extends JFrame {

    Editor editor;
    JLabel findLabel;
    JLabel replaceLabel;
    JTextField findText;
    JTextField replaceText;
    JButton findButton;
    JButton replaceButton;
    JButton replaceAllButton;
    JButton closeButton;

    JCheckBox matchCase;
    JCheckBox wholeWord;
    JCheckBox searchBackwards;

    SketchEditor sketchEditor;

    SearchContext searchContext;

    public FindAndReplace(Editor editor) {
        this.editor = editor;

        searchContext = new SearchContext();

        this.sketchEditor = editor.getTextArea();

        Base.setIcon(this);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        //setBorder(new EmptyBorder(5, 5, 5, 5));

        findLabel = new JLabel(Translate.t("Search For:"));
        replaceLabel = new JLabel(Translate.t("Replace With:"));
        findText = new JTextField(40);
        replaceText = new JTextField(40);
        findButton = new JButton(Translate.t("Find"));
        replaceButton = new JButton(Translate.t("Replace"));
        replaceAllButton = new JButton(Translate.t("Replace All"));
        closeButton = new JButton(Translate.t("Close"));
        matchCase = new JCheckBox(Translate.t("Match Case"));
        wholeWord = new JCheckBox(Translate.t("Match Whole Word"));
        searchBackwards = new JCheckBox(Translate.t("Search Backwards"));

        findText.setBackground(new Color(255, 255, 255));
        replaceText.setBackground(new Color(255, 255, 255));


        findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                findText();
            }
        });

        replaceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                replaceText();
            }
        });

        replaceAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                replaceAllText();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeWindow();
            }
        });

        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        add(findLabel, c);
        c.gridy++;
        add(replaceLabel, c);
        
        c.gridwidth = 5;
        c.gridheight = 1;
        c.gridx = 1;
        c.gridy = 0;
    
        add(findText, c);
        c.gridy++;
        add(replaceText, c);

        Box line = Box.createHorizontalBox();

        line.add(matchCase);
        line.add(wholeWord);
        line.add(searchBackwards);
        
        c.gridwidth = 5;
        c.gridx = 1;
        c.gridy = 2;

        add(line, c);

        c.gridwidth = 1;
        c.gridy = 3;
        c.gridx = 1;
        c.weightx = 1.0;

        add(Box.createHorizontalGlue(), c);
        c.weightx = 0.0;
        c.gridx++;
        add(findButton, c);
        c.gridx++;
        add(replaceButton, c);
        c.gridx++;
        add(replaceAllButton, c);
        c.gridx++;
        add(closeButton, c);

        pack();

        if (sketchEditor.isSelectionActive()) {
            findText.setText(sketchEditor.getSelectedText());
        }

        setLocationRelativeTo(editor);
        setVisible(true);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                pullToFront();
            }
        });
    }

    public void pullToFront() {
        toFront();
        repaint();
    }

    public void populateSearchContext() {
        searchContext.setSearchFor(findText.getText());
        searchContext.setReplaceWith(replaceText.getText());
        searchContext.setSearchForward(!searchBackwards.isSelected());
        searchContext.setMatchCase(matchCase.isSelected());
        searchContext.setWholeWord(wholeWord.isSelected());
    }

    public void findText() {
        populateSearchContext();
        SearchEngine.find(sketchEditor.textArea, searchContext);
    }

    public void replaceText() {
        populateSearchContext();
        SearchEngine.replace(sketchEditor.textArea, searchContext);
    }

    public void replaceAllText() {
        populateSearchContext();
        SearchEngine.replaceAll(sketchEditor.textArea, searchContext);
    }

    public void closeWindow() {
        this.dispose();
    }
}
