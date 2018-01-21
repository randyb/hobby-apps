package org.randyb.spiral;

import spark.Spark;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.*;
import static spark.Spark.*;

public class Spirals {
    private static class Line extends Line2D.Double {
        public Line(double x1, double y1, double x2, double y2) {
            super(x1, y1, x2, y2);
        }
        public Line(Point p1, Point p2) {
            super(p1, p2);
        }
    }

    private static class Point extends Point2D.Double {
        public Point(double x, double y) {
            super(x, y);
        }
    }

    public static void main(String[] args) {
        port(80);
        get("/spiral", (req, res) -> {
            String w = req.queryParams("w");
            String h = req.queryParams("h");

            BufferedImage img = new BufferedImage(Integer.parseInt(w), Integer.parseInt(h), BufferedImage.TYPE_INT_RGB);
            draw((Graphics2D)img.getGraphics(), img.getWidth(), img.getHeight());

            res.type("image/png");
            ImageIO.write(img, "png", res.raw().getOutputStream());
            return "";
        });

        SwingUtilities.invokeLater(()->{
            JFrame frame = new JFrame("Spirals");
            frame.setMinimumSize(new Dimension(300, 300));
            frame.getContentPane().add(new JPanel() {
                @Override
                public void paint(Graphics g) {
                    draw((Graphics2D) g, getWidth(), getHeight());
                }
            });
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            new Timer(1, e -> frame.repaint()).start();
        });
    }

    private static int counter = 0;
    public static void draw(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.drawString(new Date().toString(), 20, 20);

        List<Line> lines = getLines(w, h);
        Color[] COLORS = {
                Color.RED, Color.GREEN, Color.BLUE};
        for (int i = 0; i < counter; i++) {
            g.setColor(COLORS[i%COLORS.length]);
            g.draw(lines.get(i));
        }

        counter += 1;
        if (counter > lines.size()) {
            counter = 0;
        }
    }

    private static List<Line> getLines(int w, int h) {
        List<Line> lines = new ArrayList<>();
        double inset = 0.01;
        double angle = toRadians(-(180 - 60));
        double scale = 0.01;
        lines.add(new Line(inset*w, h-(inset*h), w-(inset*w), h-(inset*h)));
        int numSides = 3;
        for (int i = 1; i < numSides; ++i) {
            Line last = lines.get(lines.size()-1);
            Point v = new Point(last.x2 - last.x1, last.y2 - last.y1);
            double x = cos(angle) * v.x - sin(angle) * v.y;
            double y = sin(angle) * v.x + cos(angle) * v.y;
            lines.add(new Line(last.x2, last.y2, last.x2 + x, last.y2 + y));
        }

        while (true) {
            Point2D a = lines.get(lines.size() - 1).getP2();
            Line b = lines.get(lines.size() - (numSides - 1));
            double x = (b.x2 - b.x1) * scale + b.x1;
            double y = (b.y2 - b.y1) * scale + b.y1;
            Line newLine = new Line(a.getX(), a.getY(), x, y);
            lines.add(newLine);
            if (abs(newLine.x1 - newLine.x2) < 1 && abs(newLine.y1 - newLine.y2) < 9) {
                break;
            }
        }

        return lines;
    }









}






























