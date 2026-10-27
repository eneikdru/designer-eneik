package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;
import com.eneik.generated.designer.domain.VisionAuditReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedVisionAuditLoopTest {

    private RuleBasedVisionAuditLoop auditLoop;

    @BeforeEach
    void setUp() {
        auditLoop = new RuleBasedVisionAuditLoop();
    }

    @Test
    @DisplayName("Should pass all deontic gates and give perfect score 10.0 for compliant DOM")
    void testAuditDocumentCompliant() {
        String cleanHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head><title>Clean UI</title></head>
                <body class="bg-[#08090C] text-white">
                    <button class="focus-visible:ring-2 focus-visible:ring-indigo-500">Click</button>
                </body>
                </html>
                """;
        SynthesizedDomDocument document = SynthesizedDomDocument.create(cleanHtml, 0);

        List<ViewportCapture> captures = List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "hash1440", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "hash375", 375)
        );

        VisionAuditReport report = auditLoop.auditDocument(document, captures);

        assertNotNull(report);
        assertEquals(10.0, report.overallScore(), 0.0001);
        assertTrue(report.isApproved());
        assertTrue(report.deonticGateResult().allGatesPassed());
        assertTrue(report.deonticGateResult().zeroLeicaGpsNoise());
        assertTrue(report.deonticGateResult().noMobileHorizontalScroll());
        assertTrue(report.deonticGateResult().focusVisibleAccessible());
        assertTrue(report.deonticGateResult().contrastRatioValid());
        assertTrue(report.correctionDirectives().isEmpty());
    }

    @Test
    @DisplayName("Should detect Leica/GPS noise defect and lower score")
    void testAuditDocumentWithNoiseDefect() {
        String htmlWithNoise = """
                <div class="leica-noise-filter">
                    <button class="focus-visible:ring-2">Submit</button>
                </div>
                """;
        SynthesizedDomDocument document = SynthesizedDomDocument.create(htmlWithNoise, 0);

        List<ViewportCapture> captures = List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "h1", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "h2", 375)
        );

        VisionAuditReport report = auditLoop.auditDocument(document, captures);

        assertFalse(report.deonticGateResult().zeroLeicaGpsNoise());
        assertFalse(report.isApproved());
        assertEquals(7.0, report.overallScore(), 0.0001);
        assertEquals(1, report.correctionDirectives().size());
        assertEquals("body", report.correctionDirectives().get(0).targetCssSelector());
    }

    @Test
    @DisplayName("Should detect 375px mobile horizontal scroll overflow defect")
    void testAuditDocumentWithMobileOverflow() {
        String cleanHtml = """
                <button class="focus-visible:ring-2">Submit</button>
                """;
        SynthesizedDomDocument document = SynthesizedDomDocument.create(cleanHtml, 1);

        List<ViewportCapture> captures = List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "h1", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "h2", 420) // Overflow > 375px
        );

        VisionAuditReport report = auditLoop.auditDocument(document, captures);

        assertFalse(report.deonticGateResult().noMobileHorizontalScroll());
        assertFalse(report.isApproved());
        assertEquals(7.5, report.overallScore(), 0.0001);
        assertEquals(1, report.correctionDirectives().size());
        assertTrue(report.correctionDirectives().get(0).suggestedRule().contains("overflow-x: hidden"));
    }

    @Test
    @DisplayName("Should detect missing focus-visible and low contrast ratio defects")
    void testAuditDocumentWithAccessibilityDefects() {
        String htmlDefective = """
                <div>
                    <button class="bg-gray">No focus indicator</button>
                    <p class="text-gray-300">Low contrast text</p>
                </div>
                """;
        SynthesizedDomDocument document = SynthesizedDomDocument.create(htmlDefective, 2);

        List<ViewportCapture> captures = List.of(
                new ViewportCapture(ViewportType.DESKTOP_1440, "h1", 1440),
                new ViewportCapture(ViewportType.MOBILE_375, "h2", 375)
        );

        VisionAuditReport report = auditLoop.auditDocument(document, captures);

        assertFalse(report.deonticGateResult().focusVisibleAccessible());
        assertFalse(report.deonticGateResult().contrastRatioValid());
        assertFalse(report.isApproved());
        assertEquals(7.0, report.overallScore(), 0.0001);
        assertEquals(2, report.correctionDirectives().size());
    }
}
