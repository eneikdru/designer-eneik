package com.eneik.generated.model;

import java.util.List;
import java.util.Map;

public record DesignTokens(
        ProductArchetype archetype,
        Map<String, String> colors,
        List<TypographyConstraint> typography,
        ContrastRule contrastRule
) {
}
