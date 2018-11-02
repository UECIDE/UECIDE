package uk.co.majenko.bmpedit;

import java.awt.*;
import java.awt.image.*;

public class Convolution {
    double[] matrix;
    int size;

    public static  final double[] BLUR = {
            0.05,  0.125, 0.05,
            0.125, 0.3,   0.125,
            0.05,  0.125, 0.05
    };

    public static final double[] EDGE = {
            -1, -1, -1,
            -1, 8, -1,
            -1, -1, -1
    };

    public static final double[] SHARPEN = {
             0, -1,  0,
            -1,  5, -1,
             0, -1,  0
    };

    public static final double[] LIGHTEN = {
             0,  0,  0,
             0,  1.5, 0,
             0,  0,  0
    };

    public static final double[] DARKEN = {
             0,  0,  0,
             0,  0.75,  0,
             0,  0,  0
    };


    public Convolution(double[] m) {
        matrix = m;
        size = (int)Math.sqrt(matrix.length);
    }

    BufferedImage apply(BufferedImage in) {
        int width = in.getWidth();
        int height = in.getHeight();

        int dim = size / 2;

        BufferedImage out = new BufferedImage(width, height, in.getType());


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                double tr = 0;
                double tg = 0;
                double tb = 0;
                double ta = 0;

                for (int dy = -dim; dy <= dim; dy++) {
                    for (int dx = -dim; dx <= dim; dx++) {
                        int px = x + dx;
                        int py = y + dy;

                        if (px < 0) px = 0;
                        if (py < 0) py = 0;
                        if (px >= width) px = width - 1;
                        if (py >= height) py = height - 1;

                        int argb = in.getRGB(px, py);
                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb  & 0xFF;

                        double weight = matrix[(dx + dim) + ((dy + dim) * size)];
                        double da = (double)a * weight;
                        double dr = (double)r * weight;
                        double dg = (double)g * weight;
                        double db = (double)b * weight;
                        ta += da;
                        tr += dr;
                        tg += dg;
                        tb += db;
                    }
                }

                int ia = (int)ta;
                int ir = (int)tr;
                int ig = (int)tg;
                int ib = (int)tb;

                if (ia > 255) ia = 255;
                if (ir > 255) ir = 255;
                if (ig > 255) ig = 255;
                if (ib > 255) ib = 255;

                if (ia < 0) ia = 0;
                if (ir < 0) ir = 0;
                if (ig < 0) ig = 0;
                if (ib < 0) ib = 0;

                int ic = (ia << 24) | (ir << 16) | (ig << 8) | ib;
                out.setRGB(x, y, ic);
            }
        }

        return out;
    }
}

