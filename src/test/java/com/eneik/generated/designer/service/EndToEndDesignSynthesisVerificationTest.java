package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.CorrectionDirective;
import com.eneik.generated.designer.domain.DeonticGateResult;
import com.eneik.generated.designer.domain.DesignTokenSet;
import com.eneik.generated.designer.domain.LayoutGridPattern;
import com.eneik.generated.designer.domain.ProductArchetype;
import com.eneik.generated.designer.domain.SynthesisSession;
import com.eneik.generated.designer.domain.SynthesisStatus;
import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;
import com.eneik.generated.designer.domain.VisionAuditReport;
import com.eneik.generated.designer.telemetry.DesignTelemetryCollector;
import com.eneik.generated.designer.telemetry.DesignTelemetrySummary;
import com.eneik.generated.designer.telemetry.InMemoryDesignTelemetryCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end design synthesis verification test suite for BARCAN-TAG-06 (QA Verification).
 * Verifies the complete synthesis pipeline from archetype input and initial seed data
 * through design token compilation, grid pattern resolution, asset service resolution,
 * semantic HTML5 DOM synthesis, Playwright vision harness capture on 1440px desktop
 * and 375px mobile viewports, and visual audit gate evaluation.
 */
class EndToEndDesignSynthesisVerificationTest {

    private DesignTokenCompiler tokenCompiler;
    private LayoutPatternRegistry patternRegistry;
    private DesignAssetService assetService;
    private SemanticLayoutSynthesizer layoutSynthesizer;
    private PlaywrightVisionHarness visionHarness;
    private VisionAuditLoop auditLoop;
    private DesignTelemetryCollector telemetryCollector;
    private PrimaryAssetManagementSystem primaryAssetManagementSystem;
    private SovereignDesignSynthesisService synthesisService;

    @BeforeEach
    void setUp() {
        tokenCompiler = archetype -> new DesignTokenSet(
                archetype,
                Map.of(
                        "h1", "clamp(2rem, 5vw, 4rem)",
                        "h2", "clamp(1.5rem, 3.5vw, 2.75rem)",
                        "body", "clamp(1rem, 1.2vw, 1.125rem)"
                ),
                "#08090C",
                "#11141C",
                "border-white/[0.07]",
                7.8
        );

        patternRegistry = new LayoutPatternRegistry() {
            @Override
            public LayoutGridPattern resolveDefaultPattern(ProductArchetype archetype) {
                return new LayoutGridPattern("BENTO_GRID_2_0", "grid-cols-12", "border-white/[0.07]", true);
            }

            @Override
            public List<LayoutGridPattern> listAvailablePatterns() {
                return List.of(
                        new LayoutGridPattern("SWISS_EDITORIAL_7_5", "grid-cols-12", "border-white/[0.07]", true),
                        new LayoutGridPattern("BENTO_GRID_2_0", "grid-cols-12", "border-white/[0.07]", true)
                );
            }
        };

        assetService = projectContextKey -> Map.of(
                "hero", "https://assets.eneik.com/hero-dashboard.png",
                "logo", "https://assets.eneik.com/brand-logo.svg"
        );

        layoutSynthesizer = new DefaultSemanticLayoutSynthesizer();
        visionHarness = new DefaultPlaywrightVisionHarness(Duration.ofSeconds(5));

        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), ZoneId.of("UTC"));
        telemetryCollector = new InMemoryDesignTelemetryCollector(fixedClock);
        primaryAssetManagementSystem = (contextKey, document, captures) -> {};
    }

    @Test
    @DisplayName("Should execute complete end-to-end design synthesis pipeline to approval with initial seed data")
    void testEndToEndDesignSynthesisPipelineSuccess() {
        auditLoop = (document, captures) -> {
            assertNotNull(document);
            assertNotNull(document.htmlContent());
            assertTrue(document.htmlContent().contains(ProductArchetype.DEV_TOOLS.getDisplayName() + " Platform"));
            assertTrue(document.htmlContent().contains("focus-visible"));

            assertEquals(2, captures.size());
            ViewportCapture desktop = captures.stream().filter(c -> c.viewport() == ViewportType.DESKTOP_1440).findFirst().orElseThrow();
            ViewportCapture mobile = captures.stream().filter(c -> c.viewport() == ViewportType.MOBILE_375).findFirst().orElseThrow();

            assertEquals(1440, desktop.measuredScrollWidthPx());
            assertEquals(375, mobile.measuredScrollWidthPx());
            assertFalse(mobile.hasHorizontalOverflow());

            return new VisionAuditReport(
                    9.5,
                    new DeonticGateResult(true, true, true, true),
                    List.of(),
                    0
            );
        };

        synthesisService = new DefaultSovereignDesignSynthesisService(
                tokenCompiler, patternRegistry, assetService, layoutSynthesizer,
                visionHarness, auditLoop, telemetryCollector, primaryAssetManagementSystem
        );

        SynthesisSession session = synthesisService.executeDesignSynthesis(ProductArchetype.DEV_TOOLS, "seed-ctx-001");

        assertNotNull(session);
        assertEquals(SynthesisStatus.COMPLETED_APPROVED, session.getStatus());
        assertEquals(0, session.getCurrentRound());
        assertNotNull(session.getLatestDomDocument());
        assertTrue(session.getLatestDomDocument().htmlContent().contains("<!DOCTYPE html>"));

        DesignTelemetrySummary summary = telemetryCollector.getSummary();
        assertEquals(1, summary.totalRequests());
        assertEquals(1, summary.successCount());
        assertEquals(0, summary.failureCount());
        assertEquals(1.0, summary.overallSuccessRate(), 0.0001);
    }

    @Test
    @DisplayName("Should execute iterative refinement loop in end-to-end pipeline until target audit score is achieved")
    void testEndToEndDesignSynthesisIterativeRefinementToApproval() {
        auditLoop = (document, captures) -> {
            if (document.roundIndex() == 0) {
                return new VisionAuditReport(
                        8.5,
                        new DeonticGateResult(true, true, true, true),
                        List.of(new CorrectionDirective("header nav", "padding: 1rem;", "Improve header spacing", 0)),
                        0
                );
            } else {
                return new VisionAuditReport(
                        9.4,
                        new DeonticGateResult(true, true, true, true),
                        List.of(),
                        1
                );
            }
        };

        synthesisService = new DefaultSovereignDesignSynthesisService(
                tokenCompiler, patternRegistry, assetService, layoutSynthesizer,
                visionHarness, auditLoop, telemetryCollector, primaryAssetManagementSystem
        );

        SynthesisSession session = synthesisService.executeDesignSynthesis(ProductArchetype.FIN_TECH, "seed-ctx-fin");

        assertNotNull(session);
        assertEquals(SynthesisStatus.COMPLETED_APPROVED, session.getStatus());
        assertEquals(1, session.getCurrentRound());
        assertNotNull(session.getLatestDomDocument());
        assertTrue(session.getLatestDomDocument().htmlContent().contains("Audit Refinement Round 1"));

        DesignTelemetrySummary summary = telemetryCollector.getSummary();
        assertEquals(1, summary.totalRequests());
        assertEquals(1, summary.successCount());
        assertEquals(2, summary.auditCount());
    }
}
