package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.DesignTokenSet;
import com.eneik.generated.designer.domain.ProductArchetype;

/**
 * Service interface for compiling design tokens based on product archetype,
 * calculating adaptive typography clamp(...) expressions, and enforcing APCA/WCAG AAA contrast.
 */
public interface DesignTokenCompiler {

    /**
     * Compiles a design token set for the given product archetype.
     *
     * @param archetype target product archetype
     * @return compiled design token set
     */
    DesignTokenSet compileTokens(ProductArchetype archetype);
}
