package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.SynthesizedDomDocument;
import com.eneik.generated.designer.domain.ViewportCapture;
import com.eneik.generated.designer.domain.VisionAuditReport;

import java.util.List;

/**
 * Service performing visual quality auditing against deontic gates (Leica/GPS noise, 375px mobile scroll)
 * and generating scores and correction directives.
 */
public interface VisionAuditLoop {

    /**
     * Audits a synthesized DOM document and its viewport captures.
     */
    VisionAuditReport auditDocument(SynthesizedDomDocument document, List<ViewportCapture> captures);
}
