package Src;
import java.util.List;


class GateDef {
    String type;     
    String name;      
    List<String> inputs;
    List<String> outputs;

    GateDef(String type, String name, List<String> inputs, List<String> outputs) {
        this.type = type;
        this.name = name;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public String toString() {
        return String.format("%s %s in=%s out=%s", type, name, inputs, outputs);
    }
}

