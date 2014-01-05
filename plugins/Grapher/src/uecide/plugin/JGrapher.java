package uecide.plugin;

import uecide.app.*;
import uecide.app.debug.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.datatransfer.*;
import java.awt.geom.*;
import java.text.*;

public class JGrapher extends JComponent
{
    Dimension screenSize;
    Font font;
    Color backgroundColor = new Color(0,0,0);
    Color axisColor = new Color(255,255,255);

    float axisY1Min = 0;
    float axisY1Max = 1000;
    float axisY1Step = 100;

    int axisXStep = 100;

    int topMargin = 10;
    int leftMargin = 10;
    int rightMargin = 10;
    int bottomMargin = 60;

    int numPoints = 0;
    
    ArrayList<DataSeries> series = new ArrayList<DataSeries>();
    ArrayList<Date> dates = new ArrayList<Date>();
    int tickCycle = 0;

    public class DataSeries {
        String name;
        Color color;
        int numPoints;
        ArrayList<Float> values;

        public DataSeries(String name, Color color, int numPoints) {
            this.name = name;
            this.color = color;
            this.numPoints = numPoints;
            this.values = new ArrayList<Float>();
            for (int i = 0; i < numPoints; i++) {
                values.add(0.0f);
            }
        }

        public void add(float value) {
            values.add(value);
            while (values.size() > numPoints) {
                values.remove(0);
            }
        }

        public float[] getValues() {
            float o[] = new float[numPoints];
            for (int i = 0; i < numPoints; i++) {
                o[i] = values.get(i);
            }
            return o;
        }

        public void resize(int numPoints) {
            this.numPoints = numPoints;
            while (values.size() > numPoints) {
                values.remove(0);
            }
            while (values.size() < numPoints) {
                values.add(0, 0.0f);
            }
        }

        public Color getColor() {
            return color;
        }

        public String getName() {
            return name;
        }
    }

    public JGrapher()
    {
        setScreenSize(new Dimension(640, 480));
    }

    public void paintComponent(Graphics screen) 
    {
        BufferedImage pic = renderGraph();
        screen.drawImage(pic, 0, 0, null);
    }

    public BufferedImage renderGraph()
    {
        BufferedImage offscreen = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = offscreen.createGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setStroke(new BasicStroke(1));
        g.setColor(backgroundColor);
        g.fillRect(0, 0, screenSize.width, screenSize.height);
        g.setColor(axisColor);

        g.drawLine(leftMargin, topMargin, leftMargin, screenSize.height - bottomMargin);
        g.drawLine(leftMargin, screenSize.height - bottomMargin, screenSize.width - rightMargin, screenSize.height - bottomMargin);


        float diff = axisY1Max - axisY1Min;
        float adiff = screenSize.height - topMargin - bottomMargin;
        float scale = adiff / diff;

        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int fontShift = fm.getAscent()/2;

        for (float i = 0; i <= diff; i += axisY1Step) {
            int pos = (int) ((float) i * scale);
            for (int j = leftMargin; j < screenSize.width - rightMargin; j+=2) {
                g.drawLine(j, screenSize.height - bottomMargin - pos, j, screenSize.height - bottomMargin - pos); 
            }
            g.drawLine(leftMargin - 5, screenSize.height - bottomMargin - pos, leftMargin, screenSize.height - bottomMargin - pos);
            g.drawString(Float.toString(axisY1Min + i), 0, screenSize.height-bottomMargin-pos+fontShift);
        }

        for (int x=0; x<numPoints; x++) {
            if (x % 20 == 19 - tickCycle) {
                g.drawLine(leftMargin + x, screenSize.height - bottomMargin, leftMargin + x, screenSize.height - bottomMargin + 5);
                AffineTransform orig = g.getTransform();
                g.rotate(Math.toRadians(-90),  leftMargin + x + fontShift, screenSize.height);
                SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
                String out = fmt.format(dates.get(x));
                g.drawString(out, leftMargin + x + fontShift, screenSize.height);
                g.setTransform(orig);
            }
        }
        g.setStroke(new BasicStroke(2));

        int ypos = 0;
        for (DataSeries s : series) {
            float[] vals = s.getValues();

            g.setColor(s.getColor());
            float oval = (vals[0] - axisY1Min) * scale;

            for (int x=1; x<numPoints; x++) {
                float val = (vals[x] - axisY1Min) * scale;
                g.drawLine(leftMargin + x, screenSize.height - bottomMargin - (int)oval, leftMargin + x + 1, screenSize.height - bottomMargin - (int)val);
                oval = val;
            }

            String out = s.getName() + ": " + vals[numPoints-1];
            g.drawString(out, leftMargin + 10, topMargin + 10 + (ypos * 20));
            ypos++;
        }

        return offscreen;
    }

    public Dimension getPreferredSize() { return screenSize; }

    public Dimension getMinimumSize() { return screenSize; }
    public Dimension getMaximumSize() { return screenSize; }

    public void setFont(Font f) {
        font = f;
        repaint();
    }

    public void setBackgroundColor(Color c) {
        backgroundColor = c;
        repaint();
    }

    public void setAxisColor(Color c) {
        axisColor = c;
        repaint();
    }

    public void setLeftMargin(int d) {
        leftMargin = d;
        numPoints = screenSize.width - leftMargin - rightMargin;
        recreateDataStore();
        repaint();
        Container parent = this.getParent();
        if (parent != null) {
            parent.repaint();
        }
    } 

    public void setRightMargin(int d) {
        rightMargin = d;
        numPoints = screenSize.width - leftMargin - rightMargin;
        recreateDataStore();
        repaint();
        Container parent = this.getParent();
        if (parent != null) {
            parent.repaint();
        }
    } 

    public void setTopMargin(int d) {
        topMargin = d;
        numPoints = screenSize.width - leftMargin - rightMargin;
        recreateDataStore();
        repaint();
        Container parent = this.getParent();
        if (parent != null) {
            parent.repaint();
        }
    } 

    public void setBottomMargin(int d) {
        bottomMargin = d;
        numPoints = screenSize.width - leftMargin - rightMargin;
        recreateDataStore();
        repaint();
        Container parent = this.getParent();
        if (parent != null) {
            parent.repaint();
        }
    } 

    public void setScreenSize(Dimension d) {
        screenSize = d;
        numPoints = screenSize.width - leftMargin - rightMargin;
        recreateDataStore();
        repaint();
        Container parent = this.getParent();
        if (parent != null) {
            parent.repaint();
        }
    } 

    public void recreateDataStore() {
        while (dates.size() > numPoints) {
            dates.remove(0);
        }
        while (dates.size() < numPoints) {
            dates.add(0, new Date());
        }
        for (DataSeries s : series) {
            s.resize(numPoints);
        }
    }

    public void addSeries(String name, Color color) {
        series.add(new DataSeries(name, color, numPoints));
    }

    public void addDataPoint(float[] v) {

        for (int i = 0; i < v.length; i++) {
            if (i < series.size()) {
                series.get(i).add(v[i]);
            }
        }
        dates.add(new Date());
        while (dates.size() > numPoints) {
            dates.remove(0);
        }
        tickCycle++;
        if (tickCycle == 20) {
            tickCycle = 0;
        }
        repaint();
    }

    public void setYMinimum(float value) {
        axisY1Min = value;
    }

    public void setYMaximum(float value) {
        axisY1Max = value;
    }

    public void setYStep(float value) {
        axisY1Step = value;
    }
    
    public void reset() {
        series = new ArrayList<DataSeries>();
        recreateDataStore();
    }
}
