/*
 * Copyright (c) 2015, Majenko Technologies
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

package org.uecide;

import org.uecide.plugin.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.swing.tree.*;

public class FileCellRenderer extends DefaultTreeCellRenderer {

    Editor editor;

    public FileCellRenderer(Editor e) {
        super();
        editor = e;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        JLabel text = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if((value != null) && (value instanceof DefaultMutableTreeNode)) {
            Object userObject = ((DefaultMutableTreeNode)value).getUserObject();


            if (userObject instanceof File) {
                File file = (File)userObject;
                text.setText(file.getName());

                if(file.isDirectory()) {
                    if(expanded) {
                        text.setIcon(Base.getIcon("bookmarks", "folder-open", 16));
                    } else {
                        text.setIcon(Base.getIcon("bookmarks", "folder", 16));
                    }
                } else {
                    OverlayIcon oicon = new OverlayIcon(Base.getIcon("mime", FileType.getIcon(file), 16));

                    for(Plugin plugin : editor.plugins) {
                        try {
                            ImageIcon fi = plugin.getFileIconOverlay(file);

                            if(fi != null) {
                                oicon.add(fi);
                            }
                        } catch(AbstractMethodError e) {
                        } catch(Exception e) {
                        }
                    }

                    text.setIcon((ImageIcon)oicon);
                }
                return text;
            }

            if (userObject instanceof FlaggedList) {
                FlaggedList ent = (FlaggedList)userObject;

                text.setText(ent.toString());

                if (ent.getColor() == FlaggedList.Red) {
                    text.setIcon(Base.getIcon("flags", "fixme", 16));
                } else if (ent.getColor() == FlaggedList.Green) {
                    text.setIcon(Base.getIcon("flags", "note", 16));
                } else if (ent.getColor() == FlaggedList.Yellow) {
                    text.setIcon(Base.getIcon("flags", "todo", 16));
                } else if (ent.getColor() == FlaggedList.Blue) {
                    text.setIcon(Base.getIcon("flags", "info", 16));
                }

                return text;
            }

            if (userObject instanceof TodoEntry) {
                TodoEntry ent = (TodoEntry)userObject;

                text.setText(ent.toString());
                text.setIcon(Base.getIcon("bookmarks", "todo", 16));
                return text;
            }

            if (userObject instanceof FunctionBookmark) {
                FunctionBookmark bm = (FunctionBookmark)userObject;
                text.setText(bm.briefFormatted());
                text.setIcon(Base.getIcon("bookmarks", "function", 16));
                return text;
            }



            if (userObject instanceof Library) {
                Library lib = (Library)userObject;
                int pct = lib.getCompiledPercent();

                if(editor.loadedSketch.libraryIsCompiled(lib) && (pct >= 100 || pct <= 0)) {
                    text.setText(lib.getName());
                    text.setIcon(Base.getIcon("bookmarks", "library-good", 16));
                    return text;
                } else {
                    if(pct > 0 && pct < 100) {
                        if (pct >= 50) {
                            text.setIcon(Base.getIcon("bookmarks", "library-semi", 16));
                        } else {
                            text.setIcon(Base.getIcon("bookmarks", "library-bad", 16));
                        }

                        text.setText("");
                        JProgressBar bar = new JProgressBar();
                        bar.setString(lib.getName());
                        Dimension d = bar.getSize();
                        d.width = 120;
                        bar.setPreferredSize(d);
                        bar.setStringPainted(true);
                        bar.setValue(pct);

                        JPanel p = new JPanel();
                        p.setLayout(new BorderLayout());
                        p.add(text, BorderLayout.CENTER);
                        p.add(bar, BorderLayout.EAST);
                        return p;
                    } else {
                        text.setIcon(Base.getIcon("bookmarks", "library-bad", 16));
                        text.setText(lib.getName());
                        return text;
                    }
                }
            }

            if (userObject instanceof Sketch) {
                Sketch so = (Sketch)userObject;
                text.setText(so.getName());
                ImageIcon icon = so.getIcon(16);

                if(icon == null) {
                    icon = Base.loadIconFromResource("icon16.png");
                }
                text.setIcon(icon);
                return text;
            }

            text.setText(userObject.toString());
            if(expanded) {
                text.setIcon(Base.getIcon("bookmarks", "folder-open", 16));
            } else {
                text.setIcon(Base.getIcon("bookmarks", "folder", 16));
            }
        }
        return text;
    }
}
