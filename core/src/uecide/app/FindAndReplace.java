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

        findLabel = new JLabel(Translate.c("find.searchfor"));
        replaceLabel = new JLabel(Translate.c("find.replacewith"));
        findText = new JTextField(40);
        replaceText = new JTextField(40);
        findButton = new JButton(Translate.t("find.find"));
        replaceButton = new JButton(Translate.t("find.replace"));
        replaceAllButton = new JButton(Translate.t("find.replace.all"));
        closeButton = new JButton(Translate.t("gen.close"));
        matchCase = new JCheckBox(Translate.t("find.matchcase"));
        wholeWord = new JCheckBox(Translate.t("find.matchword"));
        searchBackwards = new JCheckBox(Translate.t("find.backwards"));

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
