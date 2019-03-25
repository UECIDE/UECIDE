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
                    try {
                        if(expanded) {
                            text.setIcon(IconManager.getIcon(16, "tree.folder-open"));
                        } else {
                            text.setIcon(IconManager.getIcon(16, "tree.folder-closed"));
                        }
                    } catch (Exception ex) {
//                        ex.printStackTrace();
                    }
                } else {
                    try {
                        OverlayIcon oicon = new OverlayIcon(IconManager.getIcon(16, "mime." + FileType.getIcon(file)));

                        for(Plugin plugin : editor.plugins) {
                                ImageIcon fi = plugin.getFileIconOverlay(file);

                            if(fi != null) {
                                oicon.add(fi);
                            }
                        }
                        text.setIcon((ImageIcon)oicon);
                    } catch(AbstractMethodError e) {
                    } catch(Exception e) {
                        e.printStackTrace();
                    }

                }
                return text;
            }

            if (userObject instanceof FlaggedList) {
                FlaggedList ent = (FlaggedList)userObject;

                text.setText(ent.toString());

                try {
                    if (ent.getColor() == FlaggedList.Red) {
                        text.setIcon(IconManager.getIcon(16, "tree.fixme"));
                    } else if (ent.getColor() == FlaggedList.Green) {
                        text.setIcon(IconManager.getIcon(16, "tree.note"));
                    } else if (ent.getColor() == FlaggedList.Yellow) {
                        text.setIcon(IconManager.getIcon(16, "tree.todo"));
                    } else if (ent.getColor() == FlaggedList.Blue) {
                        text.setIcon(IconManager.getIcon(16, "tree.info"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                return text;
            }

            if (userObject instanceof TodoEntry) {
                TodoEntry ent = (TodoEntry)userObject;

                text.setText(ent.toString());
                try {
                    text.setIcon(IconManager.getIcon(16, "tree.todo"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return text;
            }

            if (userObject instanceof FunctionBookmark) {
                FunctionBookmark bm = (FunctionBookmark)userObject;
                text.setText(bm.briefFormatted());
                try {
                    text.setIcon(IconManager.getIcon(16, "tree.function"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return text;
            }



            if (userObject instanceof Library) {
                Library lib = (Library)userObject;
                int pct = lib.getCompiledPercent();

                if(editor.loadedSketch.libraryIsCompiled(lib) && (pct >= 100 || pct <= 0)) {
                    text.setText(lib.getName());
                    try {
                        text.setIcon(IconManager.getIcon(16, "tree.lib-good"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return text;
                } else {
                    if(pct > 0 && pct < 100) {
                        try {
                            if (pct >= 50) {
                                text.setIcon(IconManager.getIcon(16, "tree.lib-semi"));
                            } else {
                                text.setIcon(IconManager.getIcon(16, "tree.lib-bad"));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
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
                        try {
                            text.setIcon(IconManager.getIcon(16, "tree.lib-bad"));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
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
                    try {
                        icon = IconManager.getIcon(16, "apps.uecide");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                text.setIcon(icon);
                return text;
            }

            text.setText(userObject.toString());
            try {
                if(expanded) {
                    text.setIcon(IconManager.getIcon(16, "tree.folder-open"));
                } else {
                    text.setIcon(IconManager.getIcon(16, "tree.folder-closed"));
                }
            } catch (Exception ex) {
//                ex.printStackTrace();
            }
        }
        return text;
    }
}
