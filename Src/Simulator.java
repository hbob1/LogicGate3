package Src;
import java.util.*;

public class Simulator {
    private final Map<String, Component> library;

    public Simulator(Map<String, Component> library) {
        this.library = library;
    }

    public Map<String, Boolean> simulate(String componentName, Map<String, Boolean> inputValues) {
        Component comp = library.get(componentName);
        if (comp == null) throw new RuntimeException("Component not found: " + componentName);

        Map<String, Boolean> wires = new HashMap<>();

        for (String inName : comp.inputs) {
            if (!inputValues.containsKey(inName)) {
                throw new RuntimeException("Missing input value for: " + inName + " (component " + componentName + ")");
            }
            wires.put(inName, inputValues.get(inName));
        }

        for (GateDef gate : comp.gates) {
            if (HDLUtils.isPrimitive(gate.type)) {
                boolean r = evalPrimitive(gate.type, wires, gate.inputs);
                wires.put(gate.outputs.get(0), r);
            } else {

                Component sub = library.get(gate.type);
                if (sub == null) throw new RuntimeException("Unknown subcomponent: " + gate.type);

                Map<String, Boolean> subInputs = new HashMap<>();
                for (int i = 0; i < sub.inputs.size(); i++) {
                    String parentWire = gate.inputs.get(i);
                    if (!wires.containsKey(parentWire)) {
                        throw new RuntimeException("Wire not set before use: " + parentWire + " in instance " + gate.name);
                    }
                    subInputs.put(sub.inputs.get(i), wires.get(parentWire));
                }

                Map<String, Boolean> subOuts = simulate(gate.type, subInputs);

                for (int i = 0; i < sub.outputs.size(); i++) {
                    String subOutName = sub.outputs.get(i);
                    Boolean val = subOuts.get(subOutName);
                    wires.put(gate.outputs.get(i), val);
                }
            }
        }

        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String outName : comp.outputs) {
            result.put(outName, wires.get(outName));
        }
        return result;
    }

    private boolean evalPrimitive(String type, Map<String, Boolean> wires, List<String> inputs) {
        type = type.toUpperCase();
        switch (type) {
            case "AND":
                return wires.get(inputs.get(0)) && wires.get(inputs.get(1));
            case "OR":
                return wires.get(inputs.get(0)) || wires.get(inputs.get(1));
            case "NOT":
                return !wires.get(inputs.get(0));
            default:
                throw new RuntimeException("Unknown primitive: " + type);
        }
    }
}