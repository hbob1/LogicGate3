package Src;
import java.util.*;

class HDLUtils {
    private static final Set<String> PRIMITIVES = Set.of("AND", "OR", "NOT");

    static boolean isPrimitive(String type) {
        return PRIMITIVES.contains(type.toUpperCase());
    }

    static int primitiveInputCount(String type) {
        type = type.toUpperCase();
        if (type.equals("NOT")) return 1;
        if (type.equals("AND") || type.equals("OR")) return 2;
        throw new IllegalArgumentException("Unknown primitive: " + type);
    }

    static int primitiveOutputCount(String type) {
        return 1;
    }
}