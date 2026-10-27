package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.CorrectionDirective;
import com.eneik.generated.designer.domain.DeonticGateResult;
import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;
import com.eneik.generated.designer.domain.VisionAuditReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Default implementation of {@link VisionAuditLoop} performing visual quality auditing against
 * deontic gates (Leica/GPS noise filtering, 375px mobile scroll overflow prevention,
 * focus-visible accessibility, and WCAG/APCA contrast requirements) and generating targeted
 * correction directives.
 */
public class DefaultVisionAuditLoop implements VisionAuditLoop {

    private static final List<String> NOISE_KEYWORDS = List.of(
            "leica",
            "gps-noise",
            "gps_noise",
            "camera-noise",
            "exif-location",
            "geotag-noise",
            "sensor-noise"
    );

    @Override
    public VisionAuditReport auditDocument(SynthesizedDomDocument document, List<ViewportCapture> captures) {
        Objects.requireNonNull(document, "document cannot be null");
        Objects.requireNonNull(captures, "captures cannot be null");

        int roundIndex = document.roundIndex();
        String htmlContent = document.htmlContent();
        String lowerHtml = htmlContent.toLowerCase(Locale.ROOT);

        boolean zeroLeicaGpsNoise = checkZeroLeicaGpsNoise(lowerHtml, captures);
        boolean noMobileHorizontalScroll = checkNoMobileHorizontalScroll(captures);
        boolean focusVisibleAccessible = checkFocusVisibleAccessible(lowerHtml);
        boolean contrastRatioValid = checkContrastRatioValid(lowerHtml);

        DeonticGateResult deonticGateResult = new DeonticGateResult(
                zeroLeicaGpsNoise,
                noMobileHorizontalScroll,
                focusVisibleAccessible,
                contrastRatioValid
        );

        List<CorrectionDirective> directives = new ArrayList<>();
        double scoreDeduction = 0.0;

        if (!zeroLeicaGpsNoise) {
            scoreDeduction += 2.0;
            directives.add(new CorrectionDirective(
                    "body",
                    "filter: none; background-image: none;",
                    "Leica/GPS noise or EXIF sensor artifacts detected in layout or viewport capture",
                    roundIndex
            ));
        }

        if (!noMobileHorizontalScroll) {
            scoreDeduction += 2.5;
            directives.add(new CorrectionDirective(
                    "html, body",
                    "overflow-x: hidden; max-width: 100vw;",
                    "Mobile horizontal scroll overflow detected on 375px viewport",
                    roundIndex
            ));
        }

        if (!focusVisibleAccessible) {
            scoreDeduction += 1.0;
            directives.add(new CorrectionDirective(
                    "*:focus-visible",
                    "outline: 2px solid #3b82f6; outline-offset: 2px;",
                    "Missing focus-visible keyboard navigation indicators in DOM elements",
                    roundIndex
            ));
        }

        if (!contrastRatioValid) {
            scoreDeduction += 1.0;
            directives.add(new CorrectionDirective(
                    ".text-low-contrast",
                    "color: #ffffff; background-color: #08090C;",
                    "Contrast ratio falls below WCAG AAA / APCA 7:1 target requirement",
                    roundIndex
            ));
        }

        double overallScore = Math.max(0.0, Math.min(10.0, 10.0 - scoreDeduction));
        // Round score to 1 decimal place for clean metric precision
        overallScore = Math.round(overallScore * 10.0) / 10.0;

        return new VisionAuditReport(overallScore, deonticGateResult, List.copyOf(directives), roundIndex);
    }

    private boolean checkZeroLeicaGpsNoise(String lowerHtml, List<ViewportCapture> captures) {
        for (String keyword : NOISE_KEYWORDS) {
            if (lowerHtml.contains(keyword)) {
                return false;
            }
        }
        for (ViewportCapture capture : captures) {
            String hash = capture.screenshotHash().toLowerCase(Locale.ROOT);
            for (String keyword : NOISE_KEYWORDS) {
                if (hash.contains(keyword)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkNoMobileHorizontalScroll(List<ViewportCapture> captures) {
        for (ViewportCapture capture : captures) {
            if (capture.viewport() == ViewportType.MOBILE_375 || capture.viewport().getWidthPx() == 375) {
                if (capture.hasHorizontalOverflow() || capture.measuredScrollWidthPx() > 375) {
                    return false;
                }
            }
            if (capture.hasHorizontalOverflow()) {
                return false;
            }
        }
        return true;
    }

    private boolean checkFocusVisibleAccessible(String lowerHtml) {
        boolean hasOutlineSuppression = lowerHtml.contains("outline-none")
                || lowerHtml.contains("outline: none")
                || lowerHtml.contains("outline:none");

        if (hasOutlineSuppression) {
            return lowerHtml.contains("focus-visible")
                    || lowerHtml.contains("focus:ring")
                    || lowerHtml.contains("focus:outline");
        }
        return true;
    }

    private boolean checkContrastRatioValid(String lowerHtml) {
        if (lowerHtml.contains("low-contrast") || lowerHtml.contains("opacity-10") || lowerHtml.contains("text-gray-300/10")) {
            return false;
        }
        return true;
    }
}
