package com.eneik.generated.service;

import com.eneik.generated.model.ContrastRule;
import com.eneik.generated.model.DesignTokens;
import com.eneik.generated.model.ProductArchetype;
import com.eneik.generated.model.TypographyConstraint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DesignTokenCompiler {

    public ProductArchetype classifyArchetype(String inputArchetype) {
        return ProductArchetype.fromString(inputArchetype);
    }

    public DesignTokens compileTokens(String archetypeInput) {
        ProductArchetype archetype = classifyArchetype(archetypeInput);
        return compileTokens(archetype);
    }

    public DesignTokens compileTokens(ProductArchetype archetype) {
        ProductArchetype resolvedArchetype = (archetype != null) ? archetype : ProductArchetype.DEFAULT;

        Map<String, String> colors = buildColorPalette(resolvedArchetype);
        List<TypographyConstraint> typography = buildTypographyConstraints(resolvedArchetype);

        String fg = colors.getOrDefault("text", "#FFFFFF");
        String bg = colors.getOrDefault("background", "#08090C");

        ContrastRule contrastRule = ContrastRule.calculate(fg, bg);

        if (!contrastRule.meetsWcagAaa()) {
            fg = "#FFFFFF";
            bg = "#08090C";
            contrastRule = ContrastRule.calculate(fg, bg);
        }

        return new DesignTokens(resolvedArchetype, colors, typography, contrastRule);
    }

    private Map<String, String> buildColorPalette(ProductArchetype archetype) {
        return switch (archetype) {
            case DEV_TOOLS -> Map.of(
                    "background", "#08090C",
                    "surface", "#11141C",
                    "text", "#F3F4F6",
                    "primary", "#6366F1",
                    "accent", "#22C55E"
            );
            case FIN_TECH -> Map.of(
                    "background", "#0B0F19",
                    "surface", "#111827",
                    "text", "#F9FAFB",
                    "primary", "#10B981",
                    "accent", "#3B82F6"
            );
            case EDITORIAL -> Map.of(
                    "background", "#0F172A",
                    "surface", "#1E293B",
                    "text", "#F8FAFC",
                    "primary", "#E11D48",
                    "accent", "#F59E0B"
            );
            case CONSUMER -> Map.of(
                    "background", "#18181B",
                    "surface", "#27272A",
                    "text", "#FAFAFA",
                    "primary", "#EC4899",
                    "accent", "#8B5CF6"
            );
            case DEFAULT -> Map.of(
                    "background", "#08090C",
                    "surface", "#11141C",
                    "text", "#FFFFFF",
                    "primary", "#6366F1",
                    "accent", "#38BDF8"
            );
        };
    }

    private List<TypographyConstraint> buildTypographyConstraints(ProductArchetype archetype) {
        return List.of(
                TypographyConstraint.create("h1", "2rem", "5vw", "4rem"),
                TypographyConstraint.create("h2", "1.5rem", "3.5vw", "2.75rem"),
                TypographyConstraint.create("h3", "1.25rem", "2.5vw", "2rem"),
                TypographyConstraint.create("body", "1rem", "1.2vw", "1.125rem"),
                TypographyConstraint.create("small", "0.75rem", "0.9vw", "0.875rem")
        );
    }
}
