package org.uecide;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Dimension;

public class AnimatedIcon extends ImageIcon {
    ArrayList<ImageIcon> icons = new ArrayList<ImageIcon>();

    int frame = 0;
    int frames = 0;
    long speed = 0;

    Timer tickTimer;

    Component attachedComponent;

    public AnimatedIcon(long s, ImageIcon... is) {
        super();
        for (int i = 0; i < is.length; i++) {
            icons.add(is[i]);
        }
        frames = is.length;
        speed = s;
        setImage(is[0].getImage());
    }

    public void start(Component whence) {
        tickTimer = new Timer();
        attachedComponent = whence;
        tickTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                AnimatedIcon.this.tick();
            }
        }, speed, speed);
    }

    public void stop() {
        tickTimer.cancel();
    }

    public void tick() {
        setImage(icons.get(frame).getImage());
        frame++;
        if (frame >= frames) {
            frame = 0;
        }

        attachedComponent.repaint();
    }
}

