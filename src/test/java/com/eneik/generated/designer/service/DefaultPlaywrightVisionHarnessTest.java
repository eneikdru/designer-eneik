package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;
import com.eneik.generated.designer.exception.RenderTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPlaywrightVisionHarnessTest {

    private DefaultPlaywrightVisionHarness harness;

    @BeforeEach
    void setUp() {
        harness = new DefaultPlaywrightVisionHarness(Duration.ofSeconds(3));
    }

    @AfterEach
    void tearDown() {
        if (harness != null) {
            harness.shutdown();
        }
    }

    @Test
    @DisplayName("Should capture screenshots at mandatory 1440px desktop and 375px mobile viewports by default")
    void testCaptureViewportsMandatoryDefaults() {
        SynthesizedDomDocument document = SynthesizedDomDocument.create("<html><body><header>Nav</header></body></html>", 0);

        List<ViewportCapture> captures = harness.captureViewports(document, null);

        assertNotNull(captures);
        assertEquals(2, captures.size());

        ViewportCapture desktopCapture = captures.stream()
                .filter(c -> c.viewport() == ViewportType.DESKTOP_1440)
                .findFirst()
                .orElseThrow();

        assertEquals(1440, desktopCapture.viewport().getWidthPx());
        assertEquals(900, desktopCapture.viewport().getHeightPx());
        assertEquals(1440, desktopCapture.measuredScrollWidthPx());
        assertFalse(desktopCapture.hasHorizontalOverflow());
        assertNotNull(desktopCapture.screenshotHash());
        assertFalse(desktopCapture.screenshotHash().isBlank());

        ViewportCapture mobileCapture = captures.stream()
                .filter(c -> c.viewport() == ViewportType.MOBILE_375)
                .findFirst()
                .orElseThrow();

        assertEquals(375, mobileCapture.viewport().getWidthPx());
        assertEquals(812, mobileCapture.viewport().getHeightPx());
        assertEquals(375, mobileCapture.measuredScrollWidthPx());
        assertFalse(mobileCapture.hasHorizontalOverflow());
        assertNotNull(mobileCapture.screenshotHash());
        assertFalse(mobileCapture.screenshotHash().isBlank());
    }

    @Test
    @DisplayName("Should capture custom requested viewports")
    void testCaptureViewportsCustomViewports() {
        SynthesizedDomDocument document = SynthesizedDomDocument.create("<html><body><main>Content</main></body></html>", 0);

        List<ViewportCapture> captures = harness.captureViewports(document, List.of(ViewportType.MOBILE_375));

        assertNotNull(captures);
        assertEquals(1, captures.size());
        assertEquals(ViewportType.MOBILE_375, captures.get(0).viewport());
    }

    @Test
    @DisplayName("Should abort gracefully and throw RenderTimeoutException when document causes renderer to hang")
    void testRenderTimeoutExceptionOnHangingDocument() {
        DefaultPlaywrightVisionHarness shortTimeoutHarness = new DefaultPlaywrightVisionHarness(Duration.ofMillis(200));

        try {
            SynthesizedDomDocument hangingDocument = SynthesizedDomDocument.create(
                    "<html><body data-render-hang=\"true\">Hanging document</body></html>", 0);

            RenderTimeoutException exception = assertThrows(
                    RenderTimeoutException.class,
                    () -> shortTimeoutHarness.captureViewports(hangingDocument, List.of(ViewportType.DESKTOP_1440))
            );

            assertTrue(exception.getMessage().contains("timed out after 200ms"));
        } finally {
            shortTimeoutHarness.shutdown();
        }
    }

    @Test
    @DisplayName("Should produce deterministic screenshot hashes for identical document fingerprints and viewports")
    void testDeterministicScreenshotHash() {
        SynthesizedDomDocument doc1 = SynthesizedDomDocument.create("<html><body><p>Identical</p></body></html>", 0);
        SynthesizedDomDocument doc2 = SynthesizedDomDocument.create("<html><body><p>Identical</p></body></html>", 0);

        List<ViewportCapture> captures1 = harness.captureViewports(doc1, List.of(ViewportType.DESKTOP_1440, ViewportType.MOBILE_375));
        List<ViewportCapture> captures2 = harness.captureViewports(doc2, List.of(ViewportType.DESKTOP_1440, ViewportType.MOBILE_375));

        assertEquals(captures1.get(0).screenshotHash(), captures2.get(0).screenshotHash());
        assertEquals(captures1.get(1).screenshotHash(), captures2.get(1).screenshotHash());
    }

    @Test
    @DisplayName("Should accurately measure scroll width and identify horizontal overflow on mobile viewport")
    void testHorizontalOverflowDetection() {
        SynthesizedDomDocument overflowDoc = SynthesizedDomDocument.create(
                "<html><body><div class=\"w-[1440px]\">Too wide</div></body></html>", 0);

        List<ViewportCapture> captures = harness.captureViewports(overflowDoc, List.of(ViewportType.MOBILE_375));

        assertEquals(1, captures.size());
        ViewportCapture mobileCapture = captures.get(0);
        assertEquals(1440, mobileCapture.measuredScrollWidthPx());
        assertTrue(mobileCapture.hasHorizontalOverflow());
    }

    @Test
    @DisplayName("Should throw NullPointerException when document is null")
    void testNullDocumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> harness.captureViewports(null, null));
    }
}
