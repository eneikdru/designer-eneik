package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;

import java.util.List;

/**
 * Service rendering hardware screenshots across required viewports (1440px desktop & 375px mobile).
 */
public interface PlaywrightVisionHarness {

    /**
     * Captures screenshot and layout metrics for the given DOM document across specified viewports.
     */
    List<ViewportCapture> captureViewports(SynthesizedDomDocument document, List<ViewportType> viewports);

    /**
     * Convenience method capturing screenshot and layout metrics for default mandatory viewports (1440px desktop & 375px mobile).
     */
    default List<ViewportCapture> captureViewports(SynthesizedDomDocument document) {
        return captureViewports(document, List.of(ViewportType.DESKTOP_1440, ViewportType.MOBILE_375));
    }
}
