package org.uecide.gui.swing;

import org.uecide.Board;
import org.uecide.Core;
import org.uecide.Compiler;
import org.uecide.Programmer;
import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import javax.swing.BoxLayout;

public class StatusBar extends JPanel implements ContextEventListener {
    Context ctx;

    UObjectLabel boardName = null;
    UObjectLabel coreName = null;
    UObjectLabel compilerName = null;
    UObjectLabel programmerName = null;
    JProgressBar percentageIndicator = null;

    public StatusBar(Context c) {
        super();
        ctx = c;
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        ctx.listenForEvent("setBoard", this);
        ctx.listenForEvent("setCore", this);
        ctx.listenForEvent("setCompiler", this);
        ctx.listenForEvent("setProgrammer", this);
        ctx.listenForEvent("percentComplete", this);
        boardName = new UObjectLabel(ctx.getBoard());
        coreName = new UObjectLabel(ctx.getCore());
        compilerName = new UObjectLabel(ctx.getCompiler());
        programmerName = new UObjectLabel(ctx.getProgrammer());
        percentageIndicator = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        add(boardName);
        add(coreName);
        add(compilerName);
        add(programmerName);
        add(Box.createHorizontalGlue());
        percentageIndicator.setAlignmentX(JProgressBar.RIGHT_ALIGNMENT);
        add(percentageIndicator);
    }

    public void contextEventTriggered(ContextEvent evt) {
        switch (evt.getEvent()) {
            case "setBoard": boardName.setObject((Board)evt.getObject()); break;
            case "setCore": coreName.setObject((Core)evt.getObject()); break;
            case "setCompiler": compilerName.setObject((Compiler)evt.getObject()); break;
            case "setProgrammer": programmerName.setObject((Programmer)evt.getObject()); break;
            case "percentComplete":
                Integer i = (Integer)evt.getObject();
                percentageIndicator.setValue(i);
                break;
        }
    }
}    
