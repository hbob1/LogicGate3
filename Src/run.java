package Src;

import java.util.*;

import javax.swing.SwingUtilities;

import Gui.HDLDesigner;

public class run {
    /*
     * public static void main(String[] args) throws Exception {
     * HDLParser parser = new HDLParser();
     * 
     * parser.parseFile("Gates\\XOR");
     * 
     * Simulator sim = new Simulator(parser.library);
     * 
     * Map<String, Boolean> inputs = new HashMap<>();
     * inputs.put("x", true);
     * inputs.put("y", true);
     * 
     * Map<String, Boolean> outs = sim.simulate("XOR", inputs);
     * System.out.println("Outputs: " + outs);
     * }
     */

        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> {
                new HDLDesigner().setVisible(true);
        });
    }
}