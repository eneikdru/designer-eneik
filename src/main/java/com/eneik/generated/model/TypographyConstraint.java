package com.eneik.generated.model;

import java.util.Objects;

public record TypographyConstraint(
        String fontSizeCategory,
        String minSize,
        String preferredSize,
        String maxSize,
        String clampExpression
) {
    public TypographyConstraint {
        Objects.requireNonNull(fontSizeCategory, "fontSizeCategory must not be null");
        Objects.requireNonNull(minSize, "minSize must not be null");
        Objects.requireNonNull(preferredSize, "preferredSize must not be null");
        Objects.requireNonNull(maxSize, "maxSize must not be null");
        if (clampExpression == null || clampExpression.isBlank()) {
            clampExpression = "clamp(" + minSize + ", " + preferredSize + ", " + maxSize + ")";
        }
    }

    public static TypographyConstraint create(String category, String min, String pref, String max) {
        String clampExpr = "clamp(" + min + ", " + pref + ", " + max + ")";
        return new TypographyConstraint(category, min, pref, max, clampExpr);
    }
}
