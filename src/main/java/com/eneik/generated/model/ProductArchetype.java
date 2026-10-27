package com.eneik.generated.model;

import java.util.Arrays;

public enum ProductArchetype {
    DEV_TOOLS,
    FIN_TECH,
    EDITORIAL,
    CONSUMER,
    DEFAULT;

    public static ProductArchetype fromString(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT;
        }
        String normalized = name.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return Arrays.stream(values())
                .filter(a -> a.name().equals(normalized))
                .findFirst()
                .orElse(DEFAULT);
    }
}
