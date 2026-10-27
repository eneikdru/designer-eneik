package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.CorrectionDirective;
import com.eneik.generated.designer.domain.DeonticGateResult;
import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;
import com.eneik.generated.designer.domain.VisionAuditReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Concrete implementation of {@link VisionAuditLoop} evaluating deontic quality gates
 * (zero Leica/GPS noise, zero 375px mobile horizontal scroll, focus-visible accessibility,
 * and valid contrast ratios) and generating visual audit scores and correction directives.
 */
public class RuleBasedVisionAuditLoop implements VisionAuditLoop {

    private static final Pattern NOISE_PATTERN = Pattern.compile("(?i)(leica-noise|gps-artifact|sensor-grain|raw-noise)");
    private static final Pattern FOCUS_VISIBLE_PATTERN = Pattern.compile("(?i)(focus-visible|focus:outline|focus:ring)");
    private static final Pattern LOW_CONTRAST_PATTERN = Pattern.compile("(?i)(text-gray-300|text-zinc-400|contrast-fail)");

    @Override
    public VisionAuditReport auditDocument(SynthesizedDomDocument document, List<ViewportCapture> captures) {
        Objects.requireNonNull(document, "document cannot be null");
        Objects.requireNonNull(captures, "captures cannot be null");

        String html = document.htmlContent();
        int roundIndex = document.roundIndex();

        // 1. Evaluate Deontic Gates
        boolean zeroLeicaGpsNoise = !NOISE_PATTERN.matcher(html).find();

        boolean noMobileHorizontalScroll = true;
        for (ViewportCapture capture : captures) {
            if (capture.viewport() == ViewportType.MOBILE_375) {
                if (capture.hasHorizontalOverflow()) {
                    noMobileHorizontalScroll = false;
                }
            }
        }

        boolean focusVisibleAccessible = FOCUS_VISIBLE_PATTERN.matcher(html).find() || !html.contains("button") && !html.contains("a ");
        boolean contrastRatioValid = !LOW_CONTRAST_PATTERN.matcher(html).find();

        DeonticGateResult gateResult = new DeonticGateResult(
                zeroLeicaGpsNoise,
                noMobileHorizontalScroll,
                focusVisibleAccessible,
                contrastRatioValid
        );

        // 2. Generate Correction Directives and calculate score deductions
        List<CorrectionDirective> directives = new ArrayList<>();
        double scoreDeduction = 0.0;

        if (!zeroLeicaGpsNoise) {
            scoreDeduction += 3.0;
            directives.add(new CorrectionDirective(
                    "body",
                    "filter: none; mix-blend-mode: normal;",
                    "Remove Leica/GPS sensor noise visual artifacts",
                    roundIndex
            ));
        }

        if (!noMobileHorizontalScroll) {
            scoreDeduction += 2.5;
            directives.add(new CorrectionDirective(
                    "html, body, .container",
                    "max-width: 100vw; overflow-x: hidden;",
                    "Eliminate 375px mobile horizontal scroll overflow",
                    roundIndex
            ));
        }

        if (!focusVisibleAccessible) {
            scoreDeduction += 1.5;
            directives.add(new CorrectionDirective(
                    "button, a, input, select, textarea",
                    "focus-visible:ring-2 focus-visible:ring-indigo-500",
                    "Add mandatory focus-visible keyboard navigation ring",
                    roundIndex
            ));
        }

        if (!contrastRatioValid) {
            scoreDeduction += 1.5;
            directives.add(new CorrectionDirective(
                    "text, p, span, h1, h2, h3",
                    "color: #FFFFFF; text-contrast: AAA;",
                    "Adjust text contrast ratio to satisfy minimum 7:1 APCA/WCAG AAA",
                    roundIndex
            ));
        }

        // Calculate overall score bounded between 0.0 and 10.0
        double rawScore = 10.0 - scoreDeduction;
        double overallScore = Math.max(0.0, Math.min(10.0, rawScore));

        return new VisionAuditReport(overallScore, gateResult, List.copyOf(directives), roundIndex);
    }
}
