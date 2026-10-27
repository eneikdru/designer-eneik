package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.CorrectionDirective;
import com.eneik.generated.designer.domain.DesignTokenSet;
import com.eneik.generated.designer.domain.LayoutGridPattern;
import com.eneik.generated.designer.domain.SynthesizedDomDocument;

import java.util.List;
import java.util.Map;

/**
 * Service synthesizing semantic HTML5 / Tailwind DOM documents.
 */
public interface SemanticLayoutSynthesizer {

    /**
     * Synthesizes initial DOM document for a given token set and grid pattern.
     */
    SynthesizedDomDocument synthesizeLayout(
            DesignTokenSet tokenSet,
            LayoutGridPattern gridPattern,
            Map<String, String> projectAssetUrls,
            int roundIndex
    );

    /**
     * Refines an existing DOM document applying targeted correction directives.
     */
    SynthesizedDomDocument refineLayout(
            SynthesizedDomDocument previousDocument,
            DesignTokenSet tokenSet,
            List<CorrectionDirective> directives,
            int roundIndex
    );
}
