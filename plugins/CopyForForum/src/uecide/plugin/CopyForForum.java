package uecide.plugin;

import uecide.app.*;
import uecide.plugin.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.datatransfer.*;


public class CopyForForum extends BasePlugin {

    public String getMenuTitle() {
        return Translate.t("Copy for Forum");
    }

    public int flags() {
        return BasePlugin.MENU_EDIT_MID;
    }

    public void run() {
        StringBuilder out = new StringBuilder();
        out.append("[code]\n");
        String[] data;
        if (editor.isSelectionActive()) {
            data = editor.getSelectedText().split("\n");
        } else {
            data = editor.getText().split("\n");
        }
        for (String line : data) {
            line = line.replace("\t", "  ");
            out.append(line);
            out.append("\n");
        }
        out.append("[/code]\n");

        Clipboard clipboard = editor.getToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(out.toString()),null);
    }
}
