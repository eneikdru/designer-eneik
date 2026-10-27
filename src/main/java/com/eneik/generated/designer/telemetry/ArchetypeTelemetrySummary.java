package com.eneik.generated.designer.telemetry;

import com.eneik.generated.designer.domain.ProductArchetype;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregated telemetry summary metrics across product archetypes for GQM framework evaluation.
 */
public record ArchetypeTelemetrySummary(
        ProductArchetype archetype,
        long totalRequests,
        long successCount,
        long failureCount,
        long auditCount,
        double successRate,
        double averageIterations,
        double averageAuditScore
) {
    public ArchetypeTelemetrySummary {
        Objects.requireNonNull(archetype, "archetype cannot be null");
    }
}
