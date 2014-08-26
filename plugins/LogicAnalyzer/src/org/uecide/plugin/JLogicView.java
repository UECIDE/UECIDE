package org.uecide.plugin;

import javax.swing.*;

import java.util.*;

import java.awt.*;
import java.awt.event.*;

public class JLogicView extends JPanel implements MouseMotionListener, MouseListener {
    
    double[] scalers = {
        0.001d, 0.002d, 0.005d,
        0.01d, 0.02d, 0.05d,
        0.1d, 0.2d, 0.5d,
        1d, 2d, 5d, 
        10d, 20d, 50d, 
        100d, 200d, 500d, 
        1000d, 2000d, 5000d,
        10000d, 20000d, 50000d, 
        100000d, 200000d, 500000d, 
        1000000d, 2000000d, 5000000d, 
        10000000d, 20000000d, 50000000d,
        100000000d, 200000000d, 500000000d, 
        1000000000d, 2000000000d, 5000000000d, 
        10000000000d, 20000000000d, 50000000000d, 
        100000000000d, 200000000000d, 500000000000d
    };

    JScrollBar horizontalScroll = null;

    Integer cPos = 0;
    Integer hPos = -1;

    TreeMap<Long, Integer>dataStore = new TreeMap<Long, Integer>();

    long timeOffset = 0;
    double samplesPerPixel = 100;
    long startTime = 0;

    boolean dragging = false;
    int startX = 0;
    long dragOffset = 0;

    double nsPerSample = 1d;

    public static final int LOW = 0;
    public static final int HIGH = 1;
    public static final int CHG = 2;

    long lastReceivedTimestamp = 0;

    public JLogicView() {
        super();

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void paintComponent(Graphics screen) {
        int th = getHeight() - 20;
        screen.setColor(new Color(50, 50, 70));
        screen.fillRect(0, 0, getWidth(), getHeight());
        int traceHeight = th / 8;
        int hoveredTrace = hPos / traceHeight;

        for (int i = 0; i < 8; i++) {
            if (i == hoveredTrace) {
                screen.setColor(new Color(60, 60, 80));
                screen.fillRect(0, i * traceHeight + 2, getWidth(), traceHeight - 5);
            }
            screen.setColor(new Color(10, 10, 20));
            screen.drawLine(0, i * traceHeight + 1, getWidth(), i * traceHeight + 1);
            screen.setColor(new Color(100, 100, 150));
            screen.drawLine(0, (i + 1) * traceHeight - 2, getWidth(), (i + 1) * traceHeight - 2);
            screen.setColor(new Color(150, 0, 0));
            screen.drawLine(cPos, i * traceHeight, cPos, i * traceHeight + 2);
            screen.drawLine(cPos - 1, i * traceHeight, cPos - 1, i * traceHeight + 1);
            screen.drawLine(cPos + 1, i * traceHeight, cPos + 1, i * traceHeight + 1);
            screen.drawLine(cPos, (i + 1) * traceHeight - 3, cPos, (i + 1) * traceHeight - 1);
            screen.drawLine(cPos - 1, (i + 1) * traceHeight - 2, cPos - 1, (i + 1) * traceHeight - 1);
            screen.drawLine(cPos + 1, (i + 1) * traceHeight - 2, cPos + 1, (i + 1) * traceHeight - 1);
        }

        for (int i = 0; i < th; i += 3) {
            screen.drawLine(cPos, i, cPos, i);
        }
            

        int[] states = { 0, 0, 0, 0, 0, 0, 0, 0 };
        int[] newStates = { 0, 0, 0, 0, 0, 0, 0, 0 };

        int pval = 0;

        Integer sv = 0;
        Map.Entry<Long, Integer> startValue = dataStore.lowerEntry(timeOffset);
        if (startValue != null) {
            sv = startValue.getValue();
        }

        states[0] = ((sv & 0x01) != 0) ? HIGH : LOW;
        states[1] = ((sv & 0x02) != 0) ? HIGH : LOW;
        states[2] = ((sv & 0x04) != 0) ? HIGH : LOW;
        states[3] = ((sv & 0x08) != 0) ? HIGH : LOW;
        states[4] = ((sv & 0x10) != 0) ? HIGH : LOW;
        states[5] = ((sv & 0x20) != 0) ? HIGH : LOW;
        states[6] = ((sv & 0x40) != 0) ? HIGH : LOW;
        states[7] = ((sv & 0x80) != 0) ? HIGH : LOW;
            
        int gutter = traceHeight / 10;

        long start = timeOffset;
        long end = (long)(timeOffset + getWidth() * samplesPerPixel);

        // This grabs a snapshot of all the points between the start and end times.
        TreeMap<Long, Integer> sub = null;
        synchronized (dataStore) {
            sub = new TreeMap<Long, Integer>(dataStore.subMap(start, end));
        }

        screen.setColor(new Color(200, 200, 200));

        // If there are no points in the sample set then draw a horizontal line for each trace
        // at the level of the most recent point before this sample set starts
        if (sub.size() == 0) {
            for (int i = 0; i < 8; i++) {
                if ((sv & (1<<i)) == 0) {
                    screen.drawLine(0, (i + 1) * traceHeight - gutter, getWidth(), (i + 1) * traceHeight - gutter);
                } else {
                    screen.drawLine(0, i * traceHeight + gutter, getWidth(), i * traceHeight + gutter);
                }
            }
        } else {
            // We have some data points, so let's draw them.

            int lastOff = 0;
            for (long time : sub.keySet()) {
                int val = sub.get(time);
                int off = (int)((time - timeOffset) / samplesPerPixel);

                screen.setColor(new Color(200, 200, 200));
            
                for (int i = 0; i < 8; i++) {
                    pval = ((val & (1<<i)) == 0) ? LOW : HIGH;
                    if (states[i] == LOW) {
                        screen.drawLine(lastOff, (i + 1) * traceHeight - gutter, off, (i + 1) * traceHeight - gutter);
                    } else {
                        screen.drawLine(lastOff, i * traceHeight + gutter, off, i * traceHeight + gutter);
                    }
                    if (pval != states[i]) {
                        states[i] = pval;
                        screen.drawLine(off, i * traceHeight + gutter, off, (i + 1) * traceHeight - gutter);
                    }
                }
                lastOff = off;
            }

            int seenOff = (int)((lastReceivedTimestamp - timeOffset) / samplesPerPixel);
            if (seenOff > lastOff) {
                for (int i = 0; i < 8; i++) {
                    if (states[i] == LOW) {
                        screen.drawLine(lastOff, (i + 1) * traceHeight - gutter, seenOff, (i + 1) * traceHeight - gutter);
                    } else {
                        screen.drawLine(lastOff, i * traceHeight + gutter, seenOff, i * traceHeight + gutter);
                    }
                }
            }
        }

        long measuredTime = 0;

        // Now for the fancy arrow thingie.
        if ((hoveredTrace >= 0) && (hoveredTrace <= 7)) {

            // Step one, get the value at the cursor position
            Map.Entry<Long, Integer> curVal = dataStore.lowerEntry((long)((cPos * samplesPerPixel) + timeOffset));
            if (curVal != null) {
                Map.Entry<Long, Integer>startVal = curVal;
                Map.Entry<Long, Integer>endVal = curVal;
                int curSet = ((curVal.getValue() & (1<<hoveredTrace)) == 0) ? LOW : HIGH;

                // Step two, expand out until we find two edges.
                Map.Entry<Long, Integer> testVal = null;

                testVal = dataStore.lowerEntry(startVal.getKey());

                while (testVal != null) {
                    int testSet = ((testVal.getValue() & (1<<hoveredTrace)) == 0) ? LOW : HIGH;
                    if (testSet != curSet) {
                        break;
                    }
                    startVal = testVal;
                    testVal = dataStore.lowerEntry(startVal.getKey());
                }
                if (testVal != null) {
                    testVal = dataStore.higherEntry(endVal.getKey());

                    while (testVal != null) {
                        int testSet = ((testVal.getValue() & (1<<hoveredTrace)) == 0) ? LOW : HIGH;
                        endVal = testVal;
                        if (testSet != curSet) {
                            break;
                        }
                        testVal = dataStore.higherEntry(endVal.getKey());
                    }
                    if (testVal != null) {
                        long astartTime = startVal.getKey();
                        long aendTime = endVal.getKey();
                        int startPos = (int)((astartTime - timeOffset) / samplesPerPixel);
                        int endPos = (int)((aendTime - timeOffset) / samplesPerPixel);
                        if (startPos < 0) { 
                            startPos = 0;
                        }
                        if (endPos > getWidth()) {
                            endPos = getWidth();
                        }

                        screen.setColor(new Color(200, 0, 0));
                        screen.drawLine(startPos, hoveredTrace * traceHeight + (traceHeight / 2), endPos, hoveredTrace * traceHeight + (traceHeight / 2));
                        screen.drawLine(startPos, hoveredTrace * traceHeight + (traceHeight / 2), startPos + 3, hoveredTrace * traceHeight + (traceHeight / 2) - 3);
                        screen.drawLine(startPos, hoveredTrace * traceHeight + (traceHeight / 2), startPos + 3, hoveredTrace * traceHeight + (traceHeight / 2) + 3);
                        screen.drawLine(endPos, hoveredTrace * traceHeight + (traceHeight / 2), endPos - 3, hoveredTrace * traceHeight + (traceHeight / 2) - 3);
                        screen.drawLine(endPos, hoveredTrace * traceHeight + (traceHeight / 2), endPos - 3, hoveredTrace * traceHeight + (traceHeight / 2) + 3);

                        measuredTime = aendTime - astartTime;
                    }
                }
            } 
        }

        screen.setColor(new Color(200, 200, 200));

        measuredTime *= nsPerSample;

        double freq = 0d;
        if (measuredTime != 0) {
            double mt = (measuredTime * 2) / 1000000000d;
            freq = 1d / mt;
        }
        
        screen.drawString(String.format("Resolution: %.3f us/pixel Offset: %.3f us Cursor: %.3f us : Measured: %.3f us / %.3f Hz",
            samplesPerPixel / 1000d * nsPerSample, timeOffset / 1000d * nsPerSample, (timeOffset + (cPos * samplesPerPixel)) / 1000d * nsPerSample, measuredTime / 1000d, freq
        ), 10, th + 10);
    }


    public void mouseMoved(MouseEvent e) {
        cPos = e.getX();
        hPos = e.getY();
        repaint();
    }
    
    public void mouseDragged(MouseEvent e) {
        cPos = e.getX();
        if (dragging) {
            int endX = e.getX();
            timeOffset = (long)(dragOffset + ((startX - endX) * samplesPerPixel));
        }
        if (timeOffset < 0) {
            timeOffset = 0;
        }

        if (horizontalScroll != null) {
            horizontalScroll.setValue((int)((timeOffset - ((getWidth() * samplesPerPixel))) / 1000L));
        }
            
        repaint();
    }

    public void mouseClicked(MouseEvent e) {
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {
        startX = e.getX();
        dragging = true;
        dragOffset = timeOffset;
    }
    public void mouseReleased(MouseEvent e) {
        dragging = false;
    }


    public void addDataPoint(long timestamp, int sample) {
        if (sample != -1) {
            if (dataStore.size() == 0) {
                startTime = System.nanoTime();
            }
            synchronized (dataStore) {
                dataStore.put(timestamp, sample);
            }
        }
        lastReceivedTimestamp = timestamp;
    }

    public void clearData() {
        synchronized (dataStore) {
            dataStore.clear();
        }
    }

    public long getSampleLength() {
        if (dataStore == null || dataStore.size() == 0) {
            return 0;
        }
        return dataStore.lastKey();
    }

    public int getZoomRange() { 
        return scalers.length;
    }

    public void setZoom(int z) {
        if (z >= scalers.length) {
            z = scalers.length-1;
        }
        if (z < 0) {
            z = 0;
        }
        samplesPerPixel = scalers[z];
        repaint();
    }

    public void setOffset(long o) {
        timeOffset = o;
        repaint();
    }

    public int getScrollPosition() {
        return (int)((lastReceivedTimestamp - ((getWidth() * samplesPerPixel))) / 1000L);
    }

    public void setNSPerSample(double n) {
        nsPerSample = n;
    }

    public void tieHorizontalScroll(JScrollBar hs) {
        horizontalScroll = hs;
    }

}
