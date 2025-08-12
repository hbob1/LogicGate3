package Src;
import java.io.*;
import java.util.*;

public class HDLParser {
    public Map<String, Component> library = new HashMap<>();


    public void parseFile(String filename) throws IOException {
        List<String> lines = readLines(filename);
        Component current = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("GRAPHICS")) continue;

            String[] parts = line.split("\\s+");
            String keyword = parts[0].toUpperCase();

            switch (keyword) {
                case "COMPONENT":
                    current = new Component();
                    current.name = parts[1];
                    break;

                case "INPUT":
                    ensureCurrent(current, line);
                    current.inputs.addAll(Arrays.asList(parts).subList(1, parts.length));
                    break;

                case "OUTPUT":
                    ensureCurrent(current, line);
                    current.outputs.addAll(Arrays.asList(parts).subList(1, parts.length));
                    break;

                case "END":
                    ensureCurrent(current, line);
                    library.put(current.name, current);
                    System.out.println("Loaded component: " + current.name);
                    current = null;
                    break;

                default:
                    ensureCurrent(current, line);
                    parseGate(parts, current);
                    break;
            }
        }
    }

    private void ensureCurrent(Component current, String line) {
        if (current == null) throw new RuntimeException("Syntax error (not inside COMPONENT) at: " + line);
    }

    private void parseGate(String[] parts, Component current) {
        String type = parts[0].toUpperCase();
        String instName = parts[1];

        int numInputs, numOutputs;
        if (HDLUtils.isPrimitive(type)) {
            numInputs = HDLUtils.primitiveInputCount(type);
            numOutputs = HDLUtils.primitiveOutputCount(type);
        } else {
            Component def = library.get(type);
            if (def == null) {
                // Try to load the component from Gates/type.txt
                String gateFile = "Gates/" + type + ".txt";
                File f = new File(gateFile);
                if (!f.exists()) {
                    // Try lowercase filename as fallback
                    f = new File("Gates/" + type.toLowerCase() + ".txt");
                }
                if (f.exists()) {
                    try {
                        parseFile(f.getPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load component file for type: " + type, e);
                    }
                    def = library.get(type);
                }
                if (def == null) {
                    throw new RuntimeException("Unknown component type (not primitive, not loaded): " + type);
                }
            }
            numInputs = def.inputs.size();
            numOutputs = def.outputs.size();
        }

        List<String> tokens = Arrays.asList(parts);
        int needed = 2 + numInputs + numOutputs;
        if (tokens.size() < needed) {
            throw new RuntimeException("Not enough tokens for gate: " + String.join(" ", parts));
        }

        List<String> inputs = tokens.subList(2, 2 + numInputs);
        List<String> outputs = tokens.subList(2 + numInputs, 2 + numInputs + numOutputs);

        current.gates.add(new GateDef(type, instName, inputs, outputs));
    }

    private List<String> readLines(String filename) throws IOException {
        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String l;
            while ((l = br.readLine()) != null) out.add(l);
        }
        return out;
    }
}