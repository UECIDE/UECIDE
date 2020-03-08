package org.uecide.gui.swing;

import org.uecide.Board;
import org.uecide.Core;
import org.uecide.Compiler;
import org.uecide.Programmer;
import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.FlowLayout;

public class StatusBar extends JPanel implements ContextEventListener {
    Context ctx;

    UObjectLabel boardName = null;
    UObjectLabel coreName = null;
    UObjectLabel compilerName = null;
    UObjectLabel programmerName = null;

    public StatusBar(Context c) {
        super();
        ctx = c;
        setLayout(new FlowLayout(FlowLayout.LEFT));

        ctx.listenForEvent("setBoard", this);
        ctx.listenForEvent("setCore", this);
        ctx.listenForEvent("setCompiler", this);
        ctx.listenForEvent("setProgrammer", this);
        boardName = new UObjectLabel(ctx.getBoard());
        coreName = new UObjectLabel(ctx.getCore());
        compilerName = new UObjectLabel(ctx.getCompiler());
        programmerName = new UObjectLabel(ctx.getProgrammer());
        add(boardName);
        add(coreName);
        add(compilerName);
        add(programmerName);
    }

    public void contextEventTriggered(ContextEvent evt) {
        switch (evt.getEvent()) {
            case "setBoard": boardName.setObject((Board)evt.getObject()); break;
            case "setCore": coreName.setObject((Core)evt.getObject()); break;
            case "setCompiler": compilerName.setObject((Compiler)evt.getObject()); break;
            case "setProgrammer": programmerName.setObject((Programmer)evt.getObject()); break;
        }
    }
}    
