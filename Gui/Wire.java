package Gui;

import java.awt.*;

public class Wire {
    public GateComponent fromGate;
    public String fromPort;
    public GateComponent toGate;
    public String toPort;

    public Wire(GateComponent fromGate, String fromPort, GateComponent toGate, String toPort) {
        this.fromGate = fromGate;
        this.fromPort = fromPort;
        this.toGate = toGate;
        this.toPort = toPort;
    }

    public void draw(Graphics g) {
        Point p1 = fromGate.getOutputPortLocation(fromPort);
        Point p2 = toGate.getInputPortLocation(toPort);
        if (p1 != null && p2 != null){
            g.setColor(Color.BLACK);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    public boolean isNear(int x, int y) {
        Point p1 = fromGate.getOutputPortLocation(fromPort);
        Point p2 = toGate.getInputPortLocation(toPort);
        if (p1 == null || p2 == null) return false;
        double dist = ptLineDist(p1.x, p1.y, p2.y, p2.x, x, y);
        return dist < 5;
    }

    private double ptLineDist(int x1, int y1, int x2, int y2, int px, int py) {
        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param  = -1;
        if (len_sq != 0) param = dot / len_sq;

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = px - xx;
        double dy = py - yy;
        return Math.sqrt(dx * dx +  dy * dy);
    }
}
