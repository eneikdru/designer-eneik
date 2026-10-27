package com.eneik.generated.designer.telemetry;

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
import com.eneik.generated.designer.service.DefaultSovereignDesignSynthesisService;
import com.eneik.generated.designer.service.DesignAssetService;
import com.eneik.generated.designer.service.DesignTokenCompiler;
import com.eneik.generated.designer.service.LayoutPatternRegistry;
import com.eneik.generated.designer.service.PlaywrightVisionHarness;
import com.eneik.generated.designer.service.SemanticLayoutSynthesizer;
import com.eneik.generated.designer.service.VisionAuditLoop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Telemetry Pipeline (BARCAN-TAG-06).
 * Verifies that generation outcomes, failure rates, and visual audit iteration counts
 * are accurately captured and reported by the telemetry system.
 */
class TelemetryPipelineVerificationTest {

    private DesignTokenCompiler tokenCompiler;
    private LayoutPatternRegistry patternRegistry;
    private DesignAssetService assetService;
    private SemanticLayoutSynthesizer layoutSynthesizer;
    private PlaywrightVisionHarness visionHarness;
    private InMemoryDesignTelemetryCollector telemetryCollector;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-10-27T14:00:00Z"), ZoneId.of("UTC"));
        telemetryCollector = new InMemoryDesignTelemetryCollector(fixedClock);

        tokenCompiler = archetype -> new DesignTokenSet(
                archetype,
                Map.of("h1", "clamp(2rem, 5vw, 4rem)"),
                "#08090C",
                "#11141C",
                "border-white/[0.07]",
                8.0
        );

        patternRegistry = new LayoutPatternRegistry() {
            @Override
            public LayoutGridPattern resolveDefaultPattern(ProductArchetype archetype) {
                return new LayoutGridPattern("BENTO_2_0", "grid-cols-12", "border-white/[0.07]", true);
            }

            @Override
            public List<LayoutGridPattern> listAvailablePatterns() {
                return List.of(new LayoutGridPattern("BENTO_2_0", "grid-cols-12", "border-white/[0.07]", true));
            }
        };

        assetService = contextKey -> Map.of("hero", "https://assets.eneik.com/hero.png");

        layoutSynthesizer = new SemanticLayoutSynthesizer() {
            @Override
            public SynthesizedDomDocument synthesizeLayout(DesignTokenSet tokenSet, LayoutGridPattern gridPattern, Map<String, String> projectAssetUrls, int roundIndex) {
                return new SynthesizedDomDocument("<html><body>Round " + roundIndex + "</body></html>", roundIndex, "hash-" + roundIndex);
            }

            @Override
            public SynthesizedDomDocument refineLayout(SynthesizedDomDocument previousDocument, DesignTokenSet tokenSet, List<com.eneik.generated.designer.domain.CorrectionDirective> directives, int roundIndex) {
                return new SynthesizedDomDocument("<html><body>Refined Round " + roundIndex + "</body></html>", roundIndex, "hash-refined-" + roundIndex);
            }
        };

        visionHarness = (document, viewports) -> List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "desktop-snap", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "mobile-snap", 375)
        );
    }

    @Test
    @DisplayName("AC 1: Given a suite of simulated generation requests, telemetry accurately reflects success and failure rates")
    void testSimulatedGenerationRequestsSuccessAndFailureRates() {
        // Setup audit loop: DEV_TOOLS succeeds on round 0, FIN_TECH fails after 3 rounds, CONSUMER succeeds on round 1, EDITORIAL fails with exception
        VisionAuditLoop customAuditLoop = (document, captures) -> {
            if (document.htmlContent().contains("Round 0")) {
                if (document.htmlContent().contains("FIN_TECH")) {
                    return new VisionAuditReport(7.5, new DeonticGateResult(true, false, true, true), List.of(), 0);
                }
            }
            return new VisionAuditReport(9.5, new DeonticGateResult(true, true, true, true), List.of(), document.roundIndex());
        };

        // Custom synthesis service with archetype condition
        DefaultSovereignDesignSynthesisService service = new DefaultSovereignDesignSynthesisService(
                tokenCompiler,
                patternRegistry,
                assetService,
                layoutSynthesizer,
                visionHarness,
                (document, captures) -> {
                    // DEV_TOOLS: immediate success (score 9.5)
                    // FIN_TECH: always failing audit (score 7.0, low score) -> triggers 3 refinements then fallback (not failed status, but fallback success)
                    // EDITORIAL: throws exception -> fails (status FAILED)
                    // CONSUMER: immediate success (score 9.4)
                    return customAuditLoop.auditDocument(document, captures);
                },
                telemetryCollector,
                fixedClock
        );

        // 1. DEV_TOOLS (Success)
        SynthesisSession session1 = service.executeDesignSynthesis(ProductArchetype.DEV_TOOLS, "ctx-1");
        assertEquals(SynthesisStatus.COMPLETED_APPROVED, session1.getStatus());

        // 2. CONSUMER (Success)
        SynthesisSession session2 = service.executeDesignSynthesis(ProductArchetype.CONSUMER, "ctx-2");
        assertEquals(SynthesisStatus.COMPLETED_APPROVED, session2.getStatus());

        // 3. EDITORIAL (Exception -> Failure)
        DefaultSovereignDesignSynthesisService failingService = new DefaultSovereignDesignSynthesisService(
                tokenCompiler,
                patternRegistry,
                assetService,
                layoutSynthesizer,
                visionHarness,
                (document, captures) -> {
                    throw new IllegalStateException("Leica visual noise gate breach");
                },
                telemetryCollector,
                fixedClock
        );
        SynthesisSession session3 = failingService.executeDesignSynthesis(ProductArchetype.EDITORIAL, "ctx-3");
        assertEquals(SynthesisStatus.FAILED, session3.getStatus());

        // Verify aggregated telemetry summary
        DesignTelemetrySummary summary = telemetryCollector.getSummary();

        // 3 total generation requests: 2 success, 1 failure
        assertEquals(3, summary.totalRequests());
        assertEquals(2, summary.successCount());
        assertEquals(1, summary.failureCount());
        assertEquals(0.66666, summary.overallSuccessRate(), 0.001);

        // Verify archetype specific stats
        ArchetypeTelemetrySummary devToolsStats = summary.archetypeSummaries().get(ProductArchetype.DEV_TOOLS);
        assertNotNull(devToolsStats);
        assertEquals(1, devToolsStats.totalRequests());
        assertEquals(1, devToolsStats.successCount());
        assertEquals(0, devToolsStats.failureCount());
        assertEquals(1.0, devToolsStats.successRate(), 0.0001);

        ArchetypeTelemetrySummary editorialStats = summary.archetypeSummaries().get(ProductArchetype.EDITORIAL);
        assertNotNull(editorialStats);
        assertEquals(1, editorialStats.totalRequests());
        assertEquals(0, editorialStats.successCount());
        assertEquals(1, editorialStats.failureCount());
        assertEquals(0.0, editorialStats.successRate(), 0.0001);
    }

    @Test
    @DisplayName("AC 2: Given a mockup requiring multiple audit iterations, telemetry reports exact number of iterations performed")
    void testMultipleAuditIterationsExactReporting() {
        // Mockup requires 2 refinement rounds (total 3 iterations: round 0, round 1, round 2) before approval on round 2
        VisionAuditLoop iterativeAuditLoop = (document, captures) -> {
            if (document.roundIndex() == 0) {
                return new VisionAuditReport(7.8, new DeonticGateResult(true, false, true, true), List.of(), 0);
            } else if (document.roundIndex() == 1) {
                return new VisionAuditReport(8.5, new DeonticGateResult(true, true, true, true), List.of(), 1);
            } else {
                return new VisionAuditReport(9.4, new DeonticGateResult(true, true, true, true), List.of(), 2);
            }
        };

        DefaultSovereignDesignSynthesisService service = new DefaultSovereignDesignSynthesisService(
                tokenCompiler,
                patternRegistry,
                assetService,
                layoutSynthesizer,
                visionHarness,
                iterativeAuditLoop,
                telemetryCollector,
                fixedClock
        );

        SynthesisSession session = service.executeDesignSynthesis(ProductArchetype.FIN_TECH, "ctx-iterative");

        assertEquals(SynthesisStatus.COMPLETED_APPROVED, session.getStatus());
        assertEquals(2, session.getCurrentRound()); // 2 refinement rounds executed

        List<TelemetryEvent> events = telemetryCollector.getRecordedEvents();
        // Should have 3 audit events (round 0, round 1, round 2) + 1 generation success event
        assertEquals(4, events.size());

        // Verify audit event iterations
        assertEquals(0, events.get(0).iterationCount());
        assertEquals(7.8, events.get(0).auditScore(), 0.0001);

        assertEquals(1, events.get(1).iterationCount());
        assertEquals(8.5, events.get(1).auditScore(), 0.0001);

        assertEquals(2, events.get(2).iterationCount());
        assertEquals(9.4, events.get(2).auditScore(), 0.0001);

        // Verify generation success event iteration count matches final session round (2)
        TelemetryEvent successEvent = events.get(3);
        assertEquals(TelemetryEventType.GENERATION_SUCCESS, successEvent.eventType());
        assertEquals(2, successEvent.iterationCount());
        assertEquals(9.4, successEvent.auditScore(), 0.0001);

        // Verify summary average iteration count calculation
        DesignTelemetrySummary summary = telemetryCollector.getSummary();
        assertEquals(1, summary.totalRequests());
        assertEquals(2.0, summary.averageIterations(), 0.0001);
    }

    @Test
    @DisplayName("Edge Case: Telemetry correctly handles maximum audit round fallback termination (3 rounds)")
    void testMaxAuditRoundsFallbackTelemetry() {
        // Audit loop never passes score threshold (< 9.2)
        VisionAuditLoop failingAuditLoop = (document, captures) -> new VisionAuditReport(
                8.1,
                new DeonticGateResult(true, false, true, true),
                List.of(),
                document.roundIndex()
        );

        DefaultSovereignDesignSynthesisService service = new DefaultSovereignDesignSynthesisService(
                tokenCompiler,
                patternRegistry,
                assetService,
                layoutSynthesizer,
                visionHarness,
                failingAuditLoop,
                telemetryCollector,
                fixedClock
        );

        SynthesisSession session = service.executeDesignSynthesis(ProductArchetype.EDITORIAL, "ctx-max-rounds");

        assertEquals(SynthesisStatus.COMPLETED_FALLBACK, session.getStatus());
        assertEquals(3, session.getCurrentRound());

        // Events: 4 audit completed events (rounds 0, 1, 2, 3) + 1 success event (fallback completion)
        List<TelemetryEvent> events = telemetryCollector.getRecordedEvents();
        assertEquals(5, events.size());

        TelemetryEvent successEvent = events.get(4);
        assertEquals(TelemetryEventType.GENERATION_SUCCESS, successEvent.eventType());
        assertEquals(3, successEvent.iterationCount());
        assertEquals(8.1, successEvent.auditScore(), 0.0001);

        DesignTelemetrySummary summary = telemetryCollector.getSummary();
        assertEquals(3.0, summary.averageIterations(), 0.0001);
    }
}
