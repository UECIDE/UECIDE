package uecide.plugin;

import uecide.app.*;
import uecide.plugin.*;
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
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;

public abstract class QueueWorker extends SwingWorker<Void, Long> {
    Object userObject = null;
    String taskCommand = null;

    public void setUserObject(Object ob) { userObject = ob; }
    public Object getUserObject() { return userObject; }
    public void setTaskCommand(String command) { taskCommand = command; }
    public String getTaskCommand() { return taskCommand; }

    public abstract String getTaskName();
    public abstract String getQueuedDescription();
    public abstract String getActiveDescription();
}
