package org.uecide;

import org.uecide.debug.*;
import org.uecide.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
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


public class ProjectSearch {
    JFrame frame;
    JPanel mainContainer;
    Editor editor;
    JScrollPane scroll;
    JTextPane text;
    HTMLDocument doc;
    HTMLEditorKit kit;


    JTextField searchTerm;

    class SearchResult extends JButton {
        File file;
        public File getFile() { return file; }

        String term;
        public String getTerm() { return term; }

        String data;
        public String getData() { return data; }

        int line;
        public int getLine() { return line; }

        SearchResult(File f, String t, String d, int l) {
            file = f;
            term = t;
            data = d;
            line = l;

            setLayout(new BorderLayout());

            JLabel top = new JLabel(file.getName() + " line " + line);
            add(top, BorderLayout.NORTH);

            String o = data.replace("<", "&lt;");
            o = o.replace(">", "&gt;");
            o = o.replaceAll("(?i)" + term, "<u>$0</u>");

            JTextPane descLabel = new JTextPane() {
                @Override
                public synchronized void addMouseListener(MouseListener l) {
                }

                @Override
                public synchronized void addMouseMotionListener(
                        MouseMotionListener l) {
                }

                @Override
                public synchronized void addMouseWheelListener(
                        MouseWheelListener l) {
                }

                @Override
                public void addNotify() {
                    disableEvents(AWTEvent.MOUSE_EVENT_MASK | 
                            AWTEvent.MOUSE_MOTION_EVENT_MASK | 
                            AWTEvent.MOUSE_WHEEL_EVENT_MASK);
                    super.addNotify();
                }
            };
            descLabel.setFocusable(false);
            descLabel.setRequestFocusEnabled(false);
            descLabel.setContentType("text/html");
            descLabel.setText("<html><body><pre>" + o + "</pre></body></html>");
  //          descLabel.setLineWrap(true);
  //          descLabel.setWrapStyleWord(true);
            descLabel.setEditable(false);
            descLabel.setOpaque(false);
            add(descLabel, BorderLayout.CENTER);

            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));

            Font fn = top.getFont();
            top.setFont(new Font(fn.getName(), Font.BOLD, fn.getSize()));

        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth();
            int h = getHeight();
            Color color1 = Color.WHITE;
            Color color2 = new Color(215, 225, 255);
            GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);
        }

    }

    public ProjectSearch(Editor e) {
        editor = e;
        mainContainer = new JPanel();
        mainContainer.setLayout(new BorderLayout());
        JToolBar tb = new JToolBar();
        searchTerm = new JTextField("", 20);
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


        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.PAGE_AXIS));

//                            File f = new File(m.group(2));
//                            int line = 0;
//                            try {
//                                line = Integer.parseInt(m.group(1));
//                            } catch (Exception ee) {
//                            }
//                            if (line > 0) {
//                                int tab = editor.openOrSelectFile(f);
//                                if (tab >= 0) {
//                                    EditorBase eb = editor.getTab(tab);
//                                    eb.gotoLine(line-1);
//                                    eb.requestFocus();
//                                }
//                            }
//                        }

        scroll.setViewportView(list);

        //mainContainer.pack();
        editor.attachPanelAsTab("Search Project", mainContainer);
        searchTerm.requestFocus();
        
    }

    public void clearText() {
        try {
            doc.remove(0, doc.getLength());
        } catch(BadLocationException e) {
        }
    }

    public void appendToText(String s) {
        try {
            kit.insertHTML(doc, doc.getLength(), s, 0, 0, null);
            text.setCaretPosition(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void doSearch() {

        for(int i = 0; i < editor.getTabCount(); i++) {
            EditorBase eb = editor.getTab(i);

            if(eb != null) {
                eb.clearHighlights();
                eb.removeFlagGroup(0x1000); // Error flags
                eb.removeFlagGroup(0x1001); // Warning flags
            }
        }

        String theme = Base.preferences.get("theme.editor", "default");
        theme = "theme." + theme + ".";
        String term = searchTerm.getText().toLowerCase();
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.PAGE_AXIS));
        for(File f : editor.loadedSketch.sketchFiles) {
            int tab = editor.getTabByFile(f);
            EditorBase eb = null;
            if (tab > -1) {
                eb = editor.getTab(tab);
            }

            String[] content = loadFileLines(f);
            boolean foundText = false;

            int lineno = 0;
            ArrayList<Integer> finds = new ArrayList<Integer>();

            for(String line : content) {
                lineno++;

                if(line.toLowerCase().contains(term)) {
                    foundText = true;
                    if (eb != null) {
                        eb.highlightLine(lineno - 1, Base.theme.getColor(theme + "editor.searchall.bgcolor"));
                    }

                    SearchResult res = new SearchResult(f, term, line, lineno);
                    res.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            SearchResult r = (SearchResult)e.getSource();
                            if (r.getLine() > 0) {
                                int tab = editor.openOrSelectFile(r.getFile());
                                if (tab >= 0) {
                                    EditorBase eb = editor.getTab(tab);
                                    eb.gotoLine(r.getLine()-1);
                                    eb.requestFocus();
                                }
                            }
                        }
                    });
                    list.add(res);
                }
            }
            scroll.setViewportView(list);
        }
    }

    public String[] loadFileLines(File f) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            StringBuilder sb = new StringBuilder();

            while((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            reader.close();
            return sb.toString().split("\n");
        } catch(Exception e) {
            Base.error(e);
        }

        return null;
    }
}
