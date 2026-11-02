package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.CorrectionDirective;
import com.eneik.generated.designer.domain.DeonticGateResult;
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
    @DisplayName("Clean document passes deontic gate with high score")
    void testCleanDocumentPassesDeonticGate() {
        String cleanHtml = "<!DOCTYPE html><html class=\"dark\"><body class=\"bg-[#08090C] text-white\">" +
                "<nav><a href=\"#\" class=\"focus-visible:ring-2 focus-visible:ring-emerald-400\">Link</a></nav>" +
                "<h1>Title</h1><p>Content</p></body></html>";

        SynthesizedDomDocument doc = SynthesizedDomDocument.create(cleanHtml, 0);
        ViewportCapture desktop = new ViewportCapture(ViewportType.DESKTOP_1440, "desktop-hash", 1440);
        ViewportCapture mobile = new ViewportCapture(ViewportType.MOBILE_375, "mobile-hash", 375);

        VisionAuditReport report = auditLoop.auditDocument(doc, List.of(desktop, mobile));

        assertNotNull(report);
        assertTrue(report.deonticGateResult().allGatesPassed());
        assertTrue(report.overallScore() >= 9.0);
        assertTrue(report.correctionDirectives().isEmpty());
    }

    @Test
    @DisplayName("Document with pseudo-optical noise fails deontic gate")
    void testDocumentWithOpticalNoiseFails() {
        String noiseHtml = "<!DOCTYPE html><html><body>" +
                "<h1>Project</h1>" +
                "<p>Captured with Leica 50mm f/1.4 at 37°46'N 122°25'W</p>" +
                "</body></html>";

        SynthesizedDomDocument doc = SynthesizedDomDocument.create(noiseHtml, 0);
        ViewportCapture desktop = new ViewportCapture(ViewportType.DESKTOP_1440, "desktop-hash", 1440);

        VisionAuditReport report = auditLoop.auditDocument(doc, List.of(desktop));

        assertNotNull(report);
        assertFalse(report.deonticGateResult().allGatesPassed());
        assertTrue(report.overallScore() < 9.0);
        assertFalse(report.correctionDirectives().isEmpty());
        assertTrue(report.correctionDirectives().stream().anyMatch(d -> d.reason().contains("BARCAN-TAG-11 violation")));
    }

    @Test
    @DisplayName("Document with banned slop colors triggers correction directive")
    void testDocumentWithBannedSlopColorsFails() {
        String slopHtml = "<!DOCTYPE html><html><body class=\"bg-indigo-600 text-purple-400\">" +
                "<button class=\"bg-violet-500\">Click</button>" +
                "</body></html>";

        SynthesizedDomDocument doc = SynthesizedDomDocument.create(slopHtml, 0);
        ViewportCapture desktop = new ViewportCapture(ViewportType.DESKTOP_1440, "desktop-hash", 1440);

        VisionAuditReport report = auditLoop.auditDocument(doc, List.of(desktop));

        assertNotNull(report);
        assertFalse(report.correctionDirectives().isEmpty());
        assertTrue(report.correctionDirectives().stream().anyMatch(d -> d.reason().contains("anti-slop gate")));
    }

    @Test
    @DisplayName("Mobile viewport horizontal overflow triggers correction directive")
    void testMobileViewportOverflowTriggersCorrection() {
        String cleanHtml = "<!DOCTYPE html><html><body><h1>Header</h1></body></html>";

        SynthesizedDomDocument doc = SynthesizedDomDocument.create(cleanHtml, 0);
        ViewportCapture desktop = new ViewportCapture(ViewportType.DESKTOP_1440, "desktop-hash", 1440);
        ViewportCapture mobileWithOverflow = new ViewportCapture(ViewportType.MOBILE_375, "mobile-hash", 450);

        VisionAuditReport report = auditLoop.auditDocument(doc, List.of(desktop, mobileWithOverflow));

        assertNotNull(report);
        assertFalse(report.deonticGateResult().allGatesPassed());
        assertTrue(report.correctionDirectives().stream().anyMatch(d -> d.reason().contains("Mobile 375px viewport horizontal overflow")));
    }
}
