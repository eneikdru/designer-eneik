package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;
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

    @Test
    @DisplayName("Should capture screenshots at mandatory 1440px and 375px viewports by default")
    void testCaptureViewportsDefaultViewports() {
        SynthesizedDomDocument doc = SynthesizedDomDocument.create("<html><body><main>Dashboard</main></body></html>", 0);

        List<ViewportCapture> captures = harness.captureViewports(doc);

        assertNotNull(captures);
        assertEquals(2, captures.size());

        ViewportCapture desktopCapture = captures.get(0);
        assertEquals(ViewportType.DESKTOP_1440, desktopCapture.viewport());
        assertEquals(1440, desktopCapture.viewport().getWidthPx());
        assertNotNull(desktopCapture.screenshotHash());
        assertFalse(desktopCapture.screenshotHash().isBlank());
        assertEquals(1440, desktopCapture.measuredScrollWidthPx());
        assertFalse(desktopCapture.hasHorizontalOverflow());

        ViewportCapture mobileCapture = captures.get(1);
        assertEquals(ViewportType.MOBILE_375, mobileCapture.viewport());
        assertEquals(375, mobileCapture.viewport().getWidthPx());
        assertNotNull(mobileCapture.screenshotHash());
        assertFalse(mobileCapture.screenshotHash().isBlank());
        assertEquals(375, mobileCapture.measuredScrollWidthPx());
        assertFalse(mobileCapture.hasHorizontalOverflow());
    }

    @Test
    @DisplayName("Should capture screenshots for specified custom viewports list")
    void testCaptureViewportsCustomViewports() {
        SynthesizedDomDocument doc = SynthesizedDomDocument.create("<html><body>Single viewport</body></html>", 0);

        List<ViewportCapture> captures = harness.captureViewports(doc, List.of(ViewportType.DESKTOP_1440));

        assertNotNull(captures);
        assertEquals(1, captures.size());
        assertEquals(ViewportType.DESKTOP_1440, captures.get(0).viewport());
    }

    @Test
    @DisplayName("Should abort gracefully and throw RenderTimeoutException when document triggers hang/timeout")
    void testRenderTimeoutException() {
        SynthesizedDomDocument doc = SynthesizedDomDocument.create(
                "<html><body data-render-hang=\"true\"><h1>Infinite Loop</h1></body></html>",
                0
        );

        RenderTimeoutException exception = assertThrows(
                RenderTimeoutException.class,
                () -> harness.captureViewports(doc)
        );

        assertTrue(exception.getMessage().contains("3000 ms"));
        assertTrue(exception.getMessage().contains(doc.domFingerprint()));
    }

    @Test
    @DisplayName("Should accurately detect horizontal overflow on mobile 375px viewport when scroll width exceeds viewport")
    void testHorizontalOverflowDetection() {
        SynthesizedDomDocument doc = SynthesizedDomDocument.create(
                "<html><body data-scroll-width-375=\"420\"><div class=\"w-[500px]\">Wide element</div></body></html>",
                0
        );

        List<ViewportCapture> captures = harness.captureViewports(doc);

        assertEquals(2, captures.size());

        ViewportCapture desktopCapture = captures.get(0);
        assertEquals(1440, desktopCapture.measuredScrollWidthPx());
        assertFalse(desktopCapture.hasHorizontalOverflow());

        ViewportCapture mobileCapture = captures.get(1);
        assertEquals(420, mobileCapture.measuredScrollWidthPx());
        assertTrue(mobileCapture.hasHorizontalOverflow());
    }

    @Test
    @DisplayName("Should throw NullPointerException when document is null")
    void testNullDocumentValidation() {
        assertThrows(
                NullPointerException.class,
                () -> harness.captureViewports(null)
        );
    }

    @Test
    @DisplayName("Should support custom timeout configuration")
    void testCustomTimeoutConfiguration() {
        DefaultPlaywrightVisionHarness customHarness = new DefaultPlaywrightVisionHarness(Duration.ofSeconds(10));
        assertEquals(Duration.ofSeconds(10), customHarness.getRenderTimeout());
    }
}
