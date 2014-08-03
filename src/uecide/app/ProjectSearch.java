package uecide.app;

import uecide.app.debug.*;
import uecide.app.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;
import java.beans.*;
import javax.imageio.*;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;


public class ProjectSearch
{
    JFrame frame;
    JPanel mainContainer;
    Editor editor;
    JScrollPane scroll;

    JTextField searchTerm;

    public ProjectSearch(Editor e) {
        editor = e;
        mainContainer = new JPanel();
        mainContainer.setLayout(new BorderLayout());
        JToolBar tb = new JToolBar();
        searchTerm = new JTextField();
        tb.add(searchTerm);
        JButton searchButton = new JButton("Search");

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSearch();
            }
        });

        searchTerm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSearch();
            }
        });
        
        tb.add(searchButton);
        tb.setFloatable(false);

        mainContainer.add(tb, BorderLayout.NORTH);

        scroll = new JScrollPane();
        mainContainer.add(scroll, BorderLayout.CENTER);

        //mainContainer.pack();
        editor.attachPanelAsTab("Search Project", mainContainer);
        searchTerm.requestFocus();
    }

    public void doSearch() {
        JPanel results = new JPanel();
        results.setLayout(new BoxLayout(results, BoxLayout.PAGE_AXIS));

        final HashMap<String, ArrayList<Integer>> matches = new HashMap<String, ArrayList<Integer>>();

        for (File f : editor.loadedSketch.sketchFiles) {
            JPanel res = new JPanel();
            res.setLayout(new BorderLayout());
            JButton fname = new JButton(f.getName());
            fname.setActionCommand(f.getAbsolutePath());
            fname.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ArrayList<Integer>finds = matches.get(e.getActionCommand());
                    if (finds != null) {
                        String theme = "theme." + Base.preferences.get("theme.selected", "default") + ".";
                        File f = new File(e.getActionCommand());
                        int tab = editor.openOrSelectFile(f);
                        if (tab == -1) {
                            return;
                        }
                        EditorBase eb = editor.getTab(tab);
                        for (int i : finds) {
                            eb.highlightLine(i, Base.theme.getColor(theme + "editor.searchall.bgcolor"));
                        }
                    }
                }
            });
            res.add(fname, BorderLayout.NORTH);

            JPanel resContent = new JPanel();
            resContent.setLayout(new BoxLayout(resContent, BoxLayout.PAGE_AXIS));

            String[] content = loadFileLines(f);

            boolean foundText = false;

            int lineno = 0;
            ArrayList<Integer> finds = new ArrayList<Integer>();
            for (String line : content) {
                lineno++;
                if (line.toLowerCase().contains(searchTerm.getText().toLowerCase())) {
                    foundText = true;
                    RSyntaxTextArea text = new RSyntaxTextArea();
                    RTextScrollPane sp = new RTextScrollPane(text);

                    finds.add(lineno-1);
                    
                    int startLine = lineno;
                    int focusLine = 0;
                    JLabel ln = new JLabel(Integer.toString(lineno));
                    StringBuilder to = new StringBuilder();
                    if (lineno > 1) {
                        to.append(content[lineno-2]);
                        to.append("\n");
                        startLine = lineno - 1;
                        focusLine = 1;
                    }
                    to.append(line);
                    if (lineno < content.length) {
                        to.append("\n");
                        to.append(content[lineno]);
                    }

                    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

                    Gutter g = sp.getGutter();
                    g.setLineNumberingStartIndex(startLine);

                    text.setText(to.toString());
                    text.setEditable(false);
                    text.setMaximumSize(new Dimension(Integer.MAX_VALUE, text.getPreferredSize().height));

                    sp.addMouseWheelListener(new MouseWheelListener() {
                        @Override
                        public void mouseWheelMoved(MouseWheelEvent e) {
                            scroll.dispatchEvent(e);
                        }
                    });
                    try {
                        text.addLineHighlight(focusLine, Base.theme.getColor("editor.searchall.bgcolor"));
                        text.setHighlightCurrentLine(false);
                    } catch (Exception e) {
                        Base.error(e);
                    }
                        
                    resContent.add(sp);
                }
            }

            if (foundText) {
                resContent.add(Box.createVerticalGlue());
                res.add(resContent, BorderLayout.CENTER);
                matches.put(f.getAbsolutePath(), finds);

                results.add(res);
            }
        }


        scroll.setViewportView(results);
    }

    public String[] loadFileLines(File f) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString().split("\n");
        } catch (Exception e) {
            Base.error(e);
        }
        return null;
    }
}
