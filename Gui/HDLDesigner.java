package Gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HDLDesigner extends JFrame {
    private final JPanel workspace;
    private final List<Wire> wires = new ArrayList<>();
    private GateComponent pendingFromGate = null;
    private String pendingFromPort = null;
    private boolean deleteMode = false;
    private Src.Component selectedComponent = null;
    private boolean runMode = false; 
    private Src.HDLParser parser;
    private String currentLoadedFile = null; // Track the currently loaded file
    private Map<String, List<String>> gateInputPorts = new HashMap<>();
    private Map<String, List<String>> gateOutputPorts = new HashMap<>();

    public Map<String, Boolean> outputValues = new LinkedHashMap<>();


    public HDLDesigner() {
        setTitle("HDL Designer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
    // Initialize parser
    this.parser = new Src.HDLParser();

    // Top toolbar
    JToolBar topToolbar = new JToolBar();
    JButton btnNew = new JButton("New");
    JButton btnOpen = new JButton("Open");
    JButton btnSave = new JButton("Save");
    JButton btnDelete = new JButton("Delete Mode");
    JButton btnRun = new JButton("Run");

    topToolbar.add(btnNew);
    topToolbar.add(btnOpen);
    topToolbar.add(btnSave);
    topToolbar.add(btnDelete);
    topToolbar.add(btnRun);

    // Left toolbar
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new GridLayout(0, 1, 5, 5));
    JButton btnInput = new JButton("Input");
    JButton btnOutput = new JButton("Output");
    JButton btnAdd = new JButton("Add Gate");

    leftPanel.add(btnInput);
    leftPanel.add(btnOutput);
    leftPanel.add(btnAdd);

    workspace = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (Wire w : wires) {
                    w.draw(g);
                }
                if (pendingFromGate != null) {
                    Point p = pendingFromGate.getOutputPortLocation(pendingFromPort);
                    if (p != null) {
                        g.setColor(Color.MAGENTA);
                        g.fillOval(p.x - 4, p.y - 4, 8, 8);
                    }
                }
            }
        };
        workspace.setBackground(Color.WHITE);

        add(topToolbar, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(new JScrollPane(workspace), BorderLayout.CENTER);

        btnNew.addActionListener(e -> createNewFile());
        btnOpen.addActionListener(e -> openFile());
        btnSave.addActionListener(e -> saveFile());
        btnAdd.addActionListener(e -> addGateFromList());
        btnInput.addActionListener(e -> addGateToWorkspace("INPUT"));
        btnOutput.addActionListener(e -> addGateToWorkspace("OUTPUT"));
        btnDelete.addActionListener(e -> {
            deleteMode = !deleteMode;
            btnDelete.setText(deleteMode ? "Delete: ON" : "Delete Mode");
        });
        btnRun.addActionListener(e -> {
            runMode = !runMode;
            btnRun.setText(runMode ? "Run: ON" : "Run");
            if (runMode && currentLoadedFile != null) {
                simulateCurrentFile();
            } else {
                outputValues.clear();
                workspace.repaint();
            }
        });
    }

    // Deprecated: replaced by simulateCurrentFile()
    public void sim(Src.Simulator simulator, Src.HDLParser parser) {}

    // Simulate the currently loaded file and update outputs
    private void simulateCurrentFile() {
        if (currentLoadedFile == null) return;
        try {
            parser.parseFile(currentLoadedFile);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Error parsing file: " + ex.getMessage());
            return;
        }
        Map<String, Boolean> inputValues = new LinkedHashMap<>();
        for (Component c : workspace.getComponents()) {
            if (c instanceof GateComponent) {
                GateComponent gc = (GateComponent) c;
                if (gc.getGateName().equalsIgnoreCase("INPUT")) {
                    Boolean val = false;
                    if (gc.inputValues != null && gc.inputValues.containsKey(gc.getInstanceName())) {
                        val = gc.inputValues.get(gc.getInstanceName());
                    }
                    inputValues.put(gc.getInstanceName(), val);
                }
            }
        }
        String compName = new java.io.File(currentLoadedFile).getName().replaceFirst("\\.txt$", "");
        Src.Simulator simulator = new Src.Simulator(parser.library);
        outputValues = simulator.simulate(compName, inputValues);
        workspace.repaint();
    }

    public void onInputToggled(String inputName, boolean value) {
        for (Component c : workspace.getComponents()) {
            if (c instanceof GateComponent) {
                GateComponent gc = (GateComponent) c;
                if (gc.getGateName().equalsIgnoreCase("INPUT") && gc.getInstanceName().equals(inputName)) {
                    if (gc.inputValues == null) {
                        gc.inputValues = new java.util.HashMap<>();
                    }
                    gc.inputValues.put(inputName, value);
                }
            }
        }
        if (runMode) {
            simulateCurrentFile();
        }
        workspace.repaint();
    }

    public List<String> getDefinedInputsForGate(String gateName) {
    return gateInputPorts.getOrDefault(gateName, new ArrayList<>());
    }

    public List<String> getDefinedOutputsForGate(String gateName) {
        return gateOutputPorts.getOrDefault(gateName, new ArrayList<>());
    }

    // Example: fill the maps when loading HDL
    private void registerGatePorts(String gateName, List<String> inputs, List<String> outputs) {
        gateInputPorts.put(gateName, new ArrayList<>(inputs));
        gateOutputPorts.put(gateName, new ArrayList<>(outputs));
    }

    public void startWire(GateComponent fromGate, String fromPort) {
        pendingFromGate = fromGate;
        pendingFromPort = fromPort;
        workspace.repaint();
    }

    public void finishWire(GateComponent toGate, String toPort) {
        if (pendingFromGate == null) {
            return;
        }

        if (pendingFromGate == toGate) {
            pendingFromGate = null;
            pendingFromPort = null;
            workspace.repaint();
            return;
        }
        wires.add(new Wire(pendingFromGate, pendingFromPort, toGate, toPort));
        pendingFromGate = null;
        pendingFromPort = null;
        workspace.repaint();
    }

    public boolean isDeleteMode() {
        return deleteMode;
    }

    public boolean isRunMode(){
        return runMode;
    }

    public void removeGate(GateComponent gate) {
        wires.removeIf(w -> w.fromGate == gate || w.toGate == gate);
        workspace.remove(gate);
        workspace.revalidate();
        workspace.repaint();
    }

    private void createNewFile() {
        String name = JOptionPane.showInputDialog(this, "Enter new gate name:");
        if (name != null && !name.trim().isEmpty()) {
            File file = new File("Gates/" + name + ".txt");
            JOptionPane.showMessageDialog(this, "Created: " + file.getAbsolutePath());
        }
    }

    private void openFile() {
        File dir = new File("Gates");
        String[] files = dir.list((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            JOptionPane.showMessageDialog(this, "No gates found.");
            return;
        }
        String choice = (String) JOptionPane.showInputDialog(
                this,
                "Select a gate:",
                "Open Gate",
                JOptionPane.PLAIN_MESSAGE,
                null,
                files,
                files[0]
        );
        if (choice != null) {
            workspace.removeAll();
            wires.clear();
            currentLoadedFile = new File(dir, choice).getPath();
            try (java.util.Scanner sc = new java.util.Scanner(new File(dir, choice))) {
                Map<String, Point> gatePositions = new HashMap<>();
                List<String> lines = new ArrayList<>();
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    lines.add(line);
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 0 && parts[0].equals("GRAPHICS") && parts.length >= 4) {
                        String instName = parts[1];
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        gatePositions.put(instName, new Point(x, y));
                    }
                }

                String compName = null;
                // Map instance name to GateComponent
                Map<String, GateComponent> instanceMap = new HashMap<>();
                // Store gate connection lines for later wire reconstruction
                List<String[]> gateConnectionLines = new ArrayList<>();

                for (String line : lines) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 0) continue;

                    switch (parts[0]) {
                        case "COMPONENT":
                            compName = parts[1];
                            break;
                        case "INPUT":
                            for (int i = 1; i < parts.length; i++) {
                                GateComponent g = new GateComponent("INPUT", parts[i], this);
                                java.util.List<String> in = new java.util.ArrayList<>();
                                java.util.List<String> out = new java.util.ArrayList<>();
                                out.add(parts[i]);
                                g.setPorts(in, out);
                                Point pos = gatePositions.getOrDefault(parts[i], new Point(20 + i * 40, 20));
                                g.setLocation(pos);
                                workspace.add(g);
                                instanceMap.put(parts[i], g);
                            }
                            break;
                        case "OUTPUT":
                            for (int i = 1; i < parts.length; i++) {
                                GateComponent g = new GateComponent("OUTPUT", parts[i], this);
                                java.util.List<String> in = new java.util.ArrayList<>();
                                java.util.List<String> out = new java.util.ArrayList<>();
                                in.add(parts[i]);
                                g.setPorts(in, out);
                                Point pos = gatePositions.getOrDefault(parts[i], new Point(20 + i * 40, 120));
                                g.setLocation(pos);
                                workspace.add(g);
                                instanceMap.put(parts[i], g);
                            }
                            break;
                        case "END":
                        case "GRAPHICS":
                            break;
                        default:
                            if (parts.length > 1) {
                                String instName = parts[1];
                                GateComponent g = new GateComponent(parts[0], instName, this);
                                java.util.List<String> in = new java.util.ArrayList<>();
                                java.util.List<String> out = new java.util.ArrayList<>();
                                for (int i = 2; i < parts.length - 1; i++) in.add(parts[i]);
                                if (parts.length > 2) out.add(parts[parts.length - 1]);
                                g.setPorts(in, out);
                                Point pos = gatePositions.getOrDefault(instName, new Point(100, 100));
                                g.setLocation(pos);
                                workspace.add(g);
                                instanceMap.put(instName, g);
                                // Save for wire reconstruction
                                gateConnectionLines.add(parts);
                            }
                            break;
                    }
                }

                // Map signal name to producing GateComponent and port
                Map<String, GateComponent> signalSourceMap = new HashMap<>();
                // INPUTs: signal name is instance name, port is "out"
                for (String key : instanceMap.keySet()) {
                    GateComponent g = instanceMap.get(key);
                    if (g.getGateName().equalsIgnoreCase("INPUT")) {
                        signalSourceMap.put(key, g);
                    }
                }
                // For each gate, its output signal is the last part of the line
                for (String[] parts : gateConnectionLines) {
                    if (parts.length > 2) {
                        String instName = parts[1];
                        String outSignal = parts[parts.length - 1];
                        GateComponent g = instanceMap.get(instName);
                        if (g != null) {
                            signalSourceMap.put(outSignal, g);
                        }
                    }
                }

                // Now reconstruct wires for each gate's inputs
                for (String[] parts : gateConnectionLines) {
                    if (parts.length > 2) {
                        String instName = parts[1];
                        GateComponent toGate = instanceMap.get(instName);
                        if (toGate == null) continue;
                        java.util.List<String> inPorts = toGate.getInputPorts();
                        int numInputs = inPorts.size();
                        int numSignals = parts.length - 2 - 1; // exclude gate name, inst name, and output
                        for (int i = 0; i < Math.min(numInputs, numSignals); i++) {
                            String inputSignal = parts[2 + i];
                            GateComponent fromGate = signalSourceMap.get(inputSignal);
                            if (fromGate == null) continue;
                            String fromPort = fromGate.getOutputPorts().isEmpty() ? "out" : fromGate.getOutputPorts().get(0);
                            String toPort = inPorts.get(i);
                            wires.add(new Wire(fromGate, fromPort, toGate, toPort));
                        }
                    }
                }

                // Reconstruct wires for OUTPUT blocks
                for (String key : instanceMap.keySet()) {
                    GateComponent g = instanceMap.get(key);
                    if (g.getGateName().equalsIgnoreCase("OUTPUT")) {
                        // The input signal is the only input port
                        java.util.List<String> inPorts = g.getInputPorts();
                        if (!inPorts.isEmpty()) {
                            String inPort = inPorts.get(0);
                            // The signal name is the instance name
                            String inputSignal = inPort;
                            GateComponent fromGate = signalSourceMap.get(inputSignal);
                            if (fromGate != null) {
                                String fromPort = fromGate.getOutputPorts().isEmpty() ? "out" : fromGate.getOutputPorts().get(0);
                                wires.add(new Wire(fromGate, fromPort, g, inPort));
                            }
                        }
                    }
                }

                workspace.revalidate();
                workspace.repaint();
                JOptionPane.showMessageDialog(this, "Loaded gate: " + choice);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage());
            }
        }
    } 
 
    private void saveFile() {
        String name = JOptionPane.showInputDialog(this, "Enter gate name to save:");
        if (name == null || name.trim().isEmpty()) return;
        File dir = new File("Gates");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, name + ".txt");
        try (java.io.PrintWriter out = new java.io.PrintWriter(file)) {

            java.util.List<GateComponent> gates = new java.util.ArrayList<>();
            java.util.List<GateComponent> outputsList = new java.util.ArrayList<>();
            for (Component c : workspace.getComponents()) {
                if (c instanceof GateComponent) {
                    GateComponent gc = (GateComponent) c;
                    gates.add(gc);
                    if (gc.getGateName().equalsIgnoreCase("OUTPUT")) outputsList.add(gc);
                }
            }
            if (!gates.isEmpty()) {
                out.println("COMPONENT " + name);

                java.util.List<String> inputs = new java.util.ArrayList<>();
                java.util.List<String> outputs = new java.util.ArrayList<>();
                for (GateComponent g : gates) {
                    if (g.getGateName().equalsIgnoreCase("INPUT")) {
                        inputs.add(g.getInstanceName());
                    } else if (g.getGateName().equalsIgnoreCase("OUTPUT")) {
                        outputs.add(g.getInstanceName());
                    }
                }
                if (!inputs.isEmpty()) out.println("INPUT " + String.join(" ", inputs));
                if (!outputs.isEmpty()) out.println("OUTPUT " + String.join(" ", outputs));

                java.util.Map<GateComponent, String> gateOutputNames = new java.util.HashMap<>();
                int tempIdx = 1;
                for (GateComponent g : gates) {
                    if (g.getGateName().equalsIgnoreCase("INPUT")) {
                        gateOutputNames.put(g, g.getInstanceName());
                    } else if (g.getGateName().equalsIgnoreCase("OUTPUT")) {
                        // skip
                    } else {
                        gateOutputNames.put(g, "temp" + tempIdx);
                        tempIdx++;
                    }
                }
                for (Wire w : wires) {
                    if (w.toGate != null && outputsList.contains(w.toGate)) {
                        gateOutputNames.put(w.fromGate, w.toGate.getInstanceName());
                    }
                }

                java.util.Map<String, java.util.Map<String, String>> gateInputSignalMap = new java.util.HashMap<>();
                for (Wire w : wires) {
                    if (w.toGate != null && !w.toGate.getGateName().equalsIgnoreCase("OUTPUT")) {
                        String toInst = w.toGate.getInstanceName();
                        String toPort = w.toPort;
                        String fromSignal = gateOutputNames.get(w.fromGate);
                        if (fromSignal == null) fromSignal = w.fromGate.getInstanceName();
                        gateInputSignalMap.computeIfAbsent(toInst, k -> new java.util.HashMap<>()).put(toPort, fromSignal);
                    }
                }

                for (GateComponent g : gates) {
                    String gName = g.getGateName();
                    if (gName.equalsIgnoreCase("INPUT") || gName.equalsIgnoreCase("OUTPUT")) continue;
                    StringBuilder line = new StringBuilder(gName + " " + g.getInstanceName());
                    java.util.List<String> inPorts = g.getInputPorts();
                    for (String in : inPorts) {
                        String sig = null;
                        java.util.Map<String, String> portMap = gateInputSignalMap.get(g.getInstanceName());
                        if (portMap != null) sig = portMap.get(in);
                        if (sig == null) sig = in;
                        line.append(" ").append(sig);
                    }
                    String outName = gateOutputNames.getOrDefault(g, "out");
                    line.append(" ").append(outName);
                    out.println(line.toString());
                }
                out.println("END");

                for (GateComponent g : gates) {
                    Point p = g.getLocation();
                    out.println("GRAPHICS" + " " + g.getInstanceName() + " " + p.x + " " + p.y);
                }
            }
            JOptionPane.showMessageDialog(this, "Saved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage());
        }
    }


    private void addGateFromList() {
        File dir = new File("Gates");
        String[] files = dir.list((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            JOptionPane.showMessageDialog(this, "No gates found.");
            return;
        }
        String choice = (String) JOptionPane.showInputDialog(
                this,
                "Select a gate to add:",
                "Add Gate",
                JOptionPane.PLAIN_MESSAGE,
                null,
                files,
                files[0]
        );
        if (choice != null) {
            addGateToWorkspace(choice.replace(".txt", ""));
        }
    }

    private void addGateToWorkspace(String gateName) {
    String instanceName = JOptionPane.showInputDialog(this, "Instance name for " + gateName + ":");
    if (instanceName == null || instanceName.trim().isEmpty()) return;
    GateComponent gate = new GateComponent(gateName, instanceName, this);

    gate.setPorts(
        getDefinedInputsForGate(gateName),
        getDefinedOutputsForGate(gateName)
    );

    workspace.add(gate);
    gate.setLocation(20 + (int)(Math.random() * 400), 20 + (int)(Math.random() * 300));
    workspace.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HDLDesigner().setVisible(true));
    }
}
