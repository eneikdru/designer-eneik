package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.CorrectionDirective;
import com.eneik.generated.designer.domain.DesignTokenSet;
import com.eneik.generated.designer.domain.LayoutGridPattern;
import com.eneik.generated.designer.domain.ProductArchetype;
import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSemanticLayoutSynthesizerTest {

    private DefaultSemanticLayoutSynthesizer synthesizer;
    private DesignTokenSet tokenSet;
    private LayoutGridPattern gridPattern;

    @BeforeEach
    void setUp() {
        synthesizer = new DefaultSemanticLayoutSynthesizer();
        tokenSet = new DesignTokenSet(
                ProductArchetype.DEV_TOOLS,
                Map.of("font-base", "clamp(1rem, 2vw, 1.2rem)"),
                "#08090C",
                "#11141C",
                "border-white/[0.07]",
                8.5
        );
        gridPattern = new LayoutGridPattern(
                "SWISS_ASYMMETRY_7_5",
                "grid grid-cols-12 gap-4",
                "border-white/[0.07]",
                true
        );
    }

    @Test
    @DisplayName("synthesizeLayout constructs valid HTML5 document styled with Tailwind CSS classes and WCAG 2.1 AA features")
    void synthesizeLayout_generatesValidHtml5WithTailwindClassesAndAccessibility() {
        Map<String, String> assets = Map.of("logo", "https://example.com/logo.png");

        SynthesizedDomDocument doc = synthesizer.synthesizeLayout(tokenSet, gridPattern, assets, 0);

        assertNotNull(doc);
        assertNotNull(doc.htmlContent());

        String html = doc.htmlContent();

        // Semantic HTML5 structure
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<html lang=\"en\">"));
        assertTrue(html.contains("<main class=\""));

        // Tailwind CSS integration (variables)
        assertTrue(html.contains("bg-[#08090C]"));
        assertTrue(html.contains("bg-[#11141C]"));
        assertTrue(html.contains("grid grid-cols-12 gap-4"));
        assertTrue(html.contains("--font-base: clamp(1rem, 2vw, 1.2rem);"));

        // Accessibility constraints (keyboard operability, visible focus, labelled controls)
        assertTrue(html.contains("focus-visible:outline"));
        assertTrue(html.contains("aria-label=\"Search\""));
        assertTrue(html.contains("aria-label=\"Submit Search\""));
        assertTrue(html.contains("tabindex=\"0\""));

        // Assets integration
        assertTrue(html.contains("https://example.com/logo.png"));
        assertTrue(html.contains("alt=\"logo\""));
    }

    @Test
    @DisplayName("refineLayout injects correction directives as CSS rules")
    void refineLayout_appendsCorrectionDirectives() {
        SynthesizedDomDocument doc = synthesizer.synthesizeLayout(tokenSet, gridPattern, Map.of(), 0);

        List<CorrectionDirective> directives = List.of(
                new CorrectionDirective(".my-class", "color: red", "Contrast fix", 1),
                new CorrectionDirective(".another-class", "padding: 10px", "Spacing fix", 1)
        );

        SynthesizedDomDocument refined = synthesizer.refineLayout(doc, tokenSet, directives, 1);

        assertNotNull(refined);
        assertEquals(1, refined.roundIndex());

        String html = refined.htmlContent();
        assertTrue(html.contains(".my-class { color: red; }"));
        assertTrue(html.contains("/* Contrast fix */"));
        assertTrue(html.contains(".another-class { padding: 10px; }"));
        assertTrue(html.contains("/* Spacing fix */"));
    }
}
