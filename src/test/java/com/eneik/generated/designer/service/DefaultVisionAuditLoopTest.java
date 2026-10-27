package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.CorrectionDirective;
import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;
import com.eneik.generated.designer.domain.VisionAuditReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultVisionAuditLoopTest {

    private DefaultVisionAuditLoop auditLoop;

    @BeforeEach
    void setUp() {
        auditLoop = new DefaultVisionAuditLoop();
    }

    @Test
    @DisplayName("Should approve document when all deontic gates pass and score >= 9.2")
    void testAuditDocumentApproved() {
        SynthesizedDomDocument doc = SynthesizedDomDocument.create(
                "<html lang=\"en\"><body><main><button class=\"focus-visible:ring-2\">Click me</button></main></body></html>",
                0
        );
        List<ViewportCapture> captures = List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "sha256-desktop-clean", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "sha256-mobile-clean", 375)
        );

        VisionAuditReport report = auditLoop.auditDocument(doc, captures);

        assertNotNull(report);
        assertEquals(10.0, report.overallScore(), 0.001);
        assertTrue(report.isApproved());
        assertTrue(report.deonticGateResult().allGatesPassed());
        assertTrue(report.deonticGateResult().zeroLeicaGpsNoise());
        assertTrue(report.deonticGateResult().noMobileHorizontalScroll());
        assertTrue(report.deonticGateResult().focusVisibleAccessible());
        assertTrue(report.deonticGateResult().contrastRatioValid());
        assertTrue(report.correctionDirectives().isEmpty());
    }

    @Test
    @DisplayName("Should fail mobile horizontal scroll gate when 375px capture scroll width exceeds viewport width")
    void testAuditDocumentMobileHorizontalScrollFailure() {
        SynthesizedDomDocument doc = SynthesizedDomDocument.create(
                "<html lang=\"en\"><body><div class=\"w-[500px]\">Wide content</div></body></html>",
                1
        );
        List<ViewportCapture> captures = List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "sha256-desktop", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "sha256-mobile-overflow", 410)
        );

        VisionAuditReport report = auditLoop.auditDocument(doc, captures);

        assertNotNull(report);
        assertEquals(7.5, report.overallScore(), 0.001);
        assertFalse(report.isApproved());
        assertFalse(report.deonticGateResult().noMobileHorizontalScroll());
        assertFalse(report.deonticGateResult().allGatesPassed());

        assertEquals(1, report.correctionDirectives().size());
        CorrectionDirective directive = report.correctionDirectives().get(0);
        assertEquals("html, body", directive.targetCssSelector());
        assertTrue(directive.reason().contains("Mobile horizontal scroll overflow"));
        assertEquals(1, directive.roundIndex());
    }

    @Test
    @DisplayName("Should fail Leica/GPS noise gate when noise artifact keywords present in DOM or capture")
    void testAuditDocumentLeicaGpsNoiseFailure() {
        SynthesizedDomDocument doc = SynthesizedDomDocument.create(
                "<html><body><div id=\"camera-noise-overlay\">Leica sensor data</div></body></html>",
                2
        );
        List<ViewportCapture> captures = List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "sha256-gps-noise-capture", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "sha256-mobile", 375)
        );

        VisionAuditReport report = auditLoop.auditDocument(doc, captures);

        assertNotNull(report);
        assertEquals(8.0, report.overallScore(), 0.001);
        assertFalse(report.isApproved());
        assertFalse(report.deonticGateResult().zeroLeicaGpsNoise());

        assertEquals(1, report.correctionDirectives().size());
        CorrectionDirective directive = report.correctionDirectives().get(0);
        assertEquals("body", directive.targetCssSelector());
        assertTrue(directive.reason().contains("Leica/GPS noise"));
    }

    @Test
    @DisplayName("Should evaluate multiple gate failures cumulatively and issue corresponding directives")
    void testAuditDocumentMultipleGateFailures() {
        SynthesizedDomDocument doc = SynthesizedDomDocument.create(
                "<html><body><div class=\"low-contrast leica-noise-filter outline-none\">Bad markup</div></body></html>",
                0
        );
        List<ViewportCapture> captures = List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "clean-desktop", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "overflow-mobile", 400)
        );

        VisionAuditReport report = auditLoop.auditDocument(doc, captures);

        assertNotNull(report);
        // Score deductions: noise (-2.0), mobile scroll (-2.5), focus (-1.0), contrast (-1.0) = 10 - 6.5 = 3.5
        assertEquals(3.5, report.overallScore(), 0.001);
        assertFalse(report.isApproved());
        assertFalse(report.deonticGateResult().allGatesPassed());
        assertEquals(4, report.correctionDirectives().size());
    }
}
