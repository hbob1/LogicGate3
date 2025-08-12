package Src;
import java.util.ArrayList;
import java.util.List;

public class Component {
    String name;
    List<String> inputs = new ArrayList<>();
    List<String> outputs = new ArrayList<>();
    List<GateDef> gates = new ArrayList<>();

    public String toString() {
        return String.format("Component %s\n  IN: %s\n  OUT: %s\n  GATES:\n    %s",
                name, inputs, outputs, gates);
    }
}
