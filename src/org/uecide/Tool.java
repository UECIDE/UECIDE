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

package org.uecide;

import java.io.*;
import java.util.*;

import org.uecide.plugin.*;

import java.util.regex.*;

public class Tool extends UObject {
    public static ArrayList<Tool> tools = new ArrayList<Tool>();


    public Tool(File folder) {
        super(folder);
    }

    public boolean execute(Context ctx, String key) {
        Context localCtx = new Context(ctx);
        localCtx.mergeSettings(_properties);
        localCtx.set("tool.root", getFolder().getAbsolutePath());
        boolean res = (Boolean)localCtx.executeKey(key, true);
        return res;
    }

    public ArrayList<ToolIcon> getIcons(int region) throws IOException {
        ArrayList<ToolIcon> out = new ArrayList<ToolIcon>();
        String[] iconlist = _properties.childKeysOf("tool.icon");

        for (String icon : iconlist) {
            PropertyFile icondata = _properties.getChildren("tool.icon." + icon);
            if ((region == ToolIcon.TOOLBAR) && (icondata.get("location").equals("toolbar"))) {
                ToolIcon ti = new ToolIcon(this, icon, icondata, Preferences.getInteger("theme.iconsize"));
                out.add(ti);
            } else if ((region == ToolIcon.EDITOR) && (icondata.get("location").equals("editor"))) {
                ToolIcon ti = new ToolIcon(this, icon, icondata, Preferences.getInteger("theme.miniiconsize"));
                out.add(ti);
            }
        }
        return out; 
    }

    public ArrayList<ToolMenu> getMenuItems() {
        ArrayList<ToolMenu> out = new ArrayList<ToolMenu>();

        String[] menuList = _properties.childKeysOf("tool.menu");
    
        for (String menu : menuList) {
            PropertyFile menudata = _properties.getChildren("tool.menu." + menu);
            ToolMenu m = new ToolMenu(this, menudata);
            out.add(m);
        }
        return out;
    }


    // ==== STATIC ====

    public static ArrayList<ToolMenu> getToolMenuItems() {
        ArrayList<ToolMenu> out = new ArrayList<ToolMenu>();
        for (Tool tool : tools) {
            ArrayList<ToolMenu> items = tool.getMenuItems();
            out.addAll(items);
        }
        return out;
    }

    public static ArrayList<ToolIcon> getRegionIcons(int region) throws IOException {
        ArrayList<ToolIcon> out = new ArrayList<ToolIcon>();
        for (Tool tool : tools) {
            ArrayList<ToolIcon> icons = tool.getIcons(region);
            out.addAll(icons);
        }
        return out;
    }

    public static void loadTools() {
        tools.clear();
        for (File dir : Base.getToolsFolders()) {
            loadToolsFromFolder(dir);
        }
    }

    public static boolean loadToolsFromFolder(File folder) {
        if (!folder.isDirectory()) {
            return false;
        }
        File[] list = folder.listFiles();
        for (File f : list) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) {
                File tfile = new File(f, "tool.txt");
                if (tfile.exists()) {
                    Tool t = new Tool(f);
                    tools.add(t);
                }
            }
        }
        return true;
    }
}
