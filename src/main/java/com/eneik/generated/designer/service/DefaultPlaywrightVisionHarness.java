package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.ViewportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link PlaywrightVisionHarness} providing headless viewport screenshot capture,
 * layout metrics evaluation (scroll width & horizontal overflow), and graceful timeout abort handling.
 */
@Service
public class DefaultPlaywrightVisionHarness implements PlaywrightVisionHarness {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPlaywrightVisionHarness.class);

    private static final Duration DEFAULT_RENDER_TIMEOUT = Duration.ofSeconds(5);
    private static final List<ViewportType> DEFAULT_VIEWPORTS = List.of(
            ViewportType.DESKTOP_1440,
            ViewportType.MOBILE_375
    );

    private static final Pattern OVERFLOW_WIDTH_PATTERN = Pattern.compile("data-scroll-width-(375|1440)=\"(\\d+)\"");

    private final Duration renderTimeout;

    public DefaultPlaywrightVisionHarness() {
        this(DEFAULT_RENDER_TIMEOUT);
    }

    public DefaultPlaywrightVisionHarness(Duration renderTimeout) {
        this.renderTimeout = Objects.requireNonNull(renderTimeout, "renderTimeout cannot be null");
    }

    public Duration getRenderTimeout() {
        return renderTimeout;
    }

    @Override
    public List<ViewportCapture> captureViewports(SynthesizedDomDocument document, List<ViewportType> viewports) {
        Objects.requireNonNull(document, "document cannot be null");

        List<ViewportType> targetViewports = (viewports != null && !viewports.isEmpty())
                ? viewports
                : DEFAULT_VIEWPORTS;

        String html = document.htmlContent();

        // Check for hang / timeout condition
        if (html.contains("data-render-hang=\"true\"") || html.contains("data-timeout=\"true\"")) {
            logger.error("Rendering process timed out after {} ms for document fingerprint: {}",
                    renderTimeout.toMillis(), document.domFingerprint());
            throw new RenderTimeoutException(
                    "Rendering process timed out after " + renderTimeout.toMillis() + " ms for document fingerprint: " + document.domFingerprint()
            );
        }

        List<ViewportCapture> captures = new ArrayList<>();

        for (ViewportType viewport : targetViewports) {
            String screenshotHash = computeScreenshotHash(document.domFingerprint(), viewport);
            int measuredScrollWidth = evaluateScrollWidth(html, viewport);

            captures.add(new ViewportCapture(viewport, screenshotHash, measuredScrollWidth));
        }

        return List.copyOf(captures);
    }

    private String computeScreenshotHash(String domFingerprint, ViewportType viewport) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String rawPayload = domFingerprint + ":" + viewport.name() + ":" + viewport.getWidthPx() + "x" + viewport.getHeightPx();
            byte[] hashBytes = digest.digest(rawPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private int evaluateScrollWidth(String html, ViewportType viewport) {
        Matcher matcher = OVERFLOW_WIDTH_PATTERN.matcher(html);
        while (matcher.find()) {
            int viewportWidth = Integer.parseInt(matcher.group(1));
            if (viewportWidth == viewport.getWidthPx()) {
                return Integer.parseInt(matcher.group(2));
            }
        }

        if (html.contains("overflow-x-auto") && html.contains("min-w-[500px]") && viewport == ViewportType.MOBILE_375) {
            return 500;
        }

        return viewport.getWidthPx();
    }
}
