package Gui;

import javax.swing.*;

import Src.run;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GateComponent extends JComponent {
    // Set input and output port names after construction (for loading from file)
    public void setPorts(java.util.List<String> inputs, java.util.List<String> outputs) {
        this.inputPorts.clear();
        this.inputPorts.addAll(inputs);
        this.outputPorts.clear();
        this.outputPorts.addAll(outputs);
    }
    private final String gateName;
    private String instanceName;
    private int mouseX, mouseY;
    private final HDLDesigner designer;
    private boolean inputActive = false;

    public Map<String, Boolean> inputValues;
    
    private final List<String> inputPorts = new ArrayList<>();
    private final List<String> outputPorts = new ArrayList<>();

    private static final int PORT_RADIUS = 16;

    public GateComponent(String gateName, HDLDesigner designer) {
        this(gateName, gateName.toLowerCase(), designer);
    }

    public GateComponent(String gateName, String instanceName, HDLDesigner designer) {
        this.gateName = gateName;
        this.instanceName = instanceName;
        this.designer = designer;


    if (gateName.equalsIgnoreCase("INPUT")) {
            outputPorts.add("out");
        } else if (gateName.equalsIgnoreCase("OUTPUT")) {
            inputPorts.add("in");
        } else {
            List<String> definedInputs = designer.getDefinedInputsForGate(gateName);
            List<String> definedOutputs = designer.getDefinedOutputsForGate(gateName);

            if (definedInputs != null && !definedInputs.isEmpty()){
                inputPorts.addAll(definedInputs);
            } else {
                inputPorts.add("in1");
                inputPorts.add("in2");
            }

            if (definedOutputs != null && !definedOutputs.isEmpty()){
                outputPorts.addAll(definedOutputs);
            } else {
                outputPorts.add("out");
            }
        }

        // Scale width and height based on number of ports
    int minWidth = 55;
    int minHeight = 32;
    int portCount = Math.max(inputPorts.size(), outputPorts.size());
    int scaledHeight = Math.max(minHeight, 20 + 22 * portCount);
    int scaledWidth = minWidth + 10 * Math.max(0, portCount - 2);
    setBounds(50, 50, scaledWidth, scaledHeight);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - mouseX;
                int dy = e.getY() - mouseY;
                setLocation(getX() + dx, getY() + dy);
                // repaint the parent workspace so wires move too
                getParent().repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // translate click position inside this component
                Point p = e.getPoint();

                // 1) check if click is on an output port (start a wire)
                String clickedOut = getOutputPortAtLocal(p);
                if (clickedOut != null) {
                    designer.startWire(GateComponent.this, clickedOut);
                    return;
                }

                // 2) check if click is on an input port (finish a wire)
                String clickedIn = getInputPortAtLocal(p);
                if (clickedIn != null) {
                    designer.finishWire(GateComponent.this, clickedIn);
                    return;
                }

                // 3) if delete mode is on and clicked on the body, remove this gate
                if (designer.isDeleteMode()) {
                    designer.removeGate(GateComponent.this);
                }

                if (designer.isRunMode() && gateName.equalsIgnoreCase("INPUT")) {
                    String inputName = getInstanceName();

                    inputActive = !inputActive;
                    designer.onInputToggled(inputName, inputActive);
                    repaint();
                    return;
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public String getGateName() {
        return gateName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String name) {
        this.instanceName = name;
    }

    // Return absolute positions (workspace coordinates) for ports
    public Point getInputPortLocation(String portName) {
        int index = inputPorts.indexOf(portName);
        if (index == -1) return null;
        int y = (getHeight() / (inputPorts.size() + 1)) * (index + 1);
        int x = 0; // left edge
        return new Point(getX() + x, getY() + y);
    }

    public Point getOutputPortLocation(String portName) {
        int index = outputPorts.indexOf(portName);
        if (index == -1) return null;
        int y = (getHeight() / (outputPorts.size() + 1)) * (index + 1);
        int x = getWidth(); // right edge
        return new Point(getX() + x, getY() + y);
    }

    // Given a local point inside this component, return the input port name if any
    private String getInputPortAtLocal(Point p) {
        for (int i = 0; i < inputPorts.size(); i++) {
            int y = (getHeight() / (inputPorts.size() + 1)) * (i + 1);
            int x = 0;
            double dist = p.distance(x, y);
            if (dist <= PORT_RADIUS) return inputPorts.get(i);
        }
        return null;
    }

    // Given a local point inside this component, return the output port name if any
    private String getOutputPortAtLocal(Point p) {
        for (int i = 0; i < outputPorts.size(); i++) {
            int y = (getHeight() / (outputPorts.size() + 1)) * (i + 1);
            int x = getWidth();
            double dist = p.distance(x, y);
            if (dist <= PORT_RADIUS) return outputPorts.get(i);
        }
        return null;
    }

    public java.util.List<String> getInputPorts() { return inputPorts; }
    public java.util.List<String> getOutputPorts() { return outputPorts; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // body fill for INPUT/OUTPUT coloring
        boolean filled = false;
        if (gateName.equalsIgnoreCase("INPUT") && inputActive) {
            g.setColor(Color.RED);
            g.fillRect(0, 0, getWidth(), getHeight());
            filled = true;
        } else if (gateName.equalsIgnoreCase("OUTPUT") && designer.isRunMode()) {
            Boolean val = designer.outputValues.get(instanceName);
            if (Boolean.TRUE.equals(val)) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.LIGHT_GRAY);
            }
            g.fillRect(0, 0, getWidth(), getHeight());
            filled = true;
        }
        if (!filled) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        // name
        g.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(gateName);
        g.setColor(Color.BLACK);
        g.drawString(gateName, (getWidth() - w) / 2, getHeight() / 2 + 5);

        // input ports (blue)
        g.setColor(Color.BLUE);
        for (int i = 0; i < inputPorts.size(); i++) {
            int y = (getHeight() / (inputPorts.size() + 1)) * (i + 1);
            g.fillOval(0 - PORT_RADIUS/2, y - PORT_RADIUS/2, PORT_RADIUS, PORT_RADIUS);
        }

        // output ports (red)
        g.setColor(Color.RED);
        for (int i = 0; i < outputPorts.size(); i++) {
            int y = (getHeight() / (outputPorts.size() + 1)) * (i + 1);
            g.fillOval(getWidth() - PORT_RADIUS/2, y - PORT_RADIUS/2, PORT_RADIUS, PORT_RADIUS);
        }

    }
}
