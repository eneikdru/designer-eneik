package com.eneik.generated.designer.domain;

/**
 * Product archetype classifier determining design system aesthetic rules and token defaults.
 */
public enum ProductArchetype {
    DEV_TOOLS("DevTools", "Dark minimalist obsidian theme with mono typography and sharp borders"),
    FIN_TECH("FinTech", "High-contrast precision editorial layout with obsidian accents"),
    EDITORIAL("Editorial", "Swiss magazine asymmetry (7/5 and 8/4 grid ratios) with bold serif accents"),
    CONSUMER("Consumer", "Bento Grid 2.0 with layered depth and subtle glassmorphic borders");

    private final String displayName;
    private final String description;

    ProductArchetype(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
