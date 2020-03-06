package org.uecide.gui.none;

import org.uecide.gui.*;
import org.uecide.gui.cli.*;
import org.uecide.*;
import org.uecide.actions.*;

import java.util.*;
import java.io.*;

import org.uecide.Compiler;
import org.uecide.Package;

public class NoneGui extends CliGui {

    public NoneGui(Context c) {
        super(c);
    }

    public void open() {
    }

    public static void init() {
    }

    public static void endinit() {
    }

    @Override
    public void openSketchFileEditor(SketchFile f) {
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }
}
