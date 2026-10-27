package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.SynthesizedDomDocument;

import java.util.Map;

/**
 * Service providing real media assets and static resources for inclusion during DOM synthesis.
 */
public interface DesignAssetService {

    /**
     * Resolves project media asset URLs by category or context key.
     */
    Map<String, String> resolveProjectAssets(String projectContextKey);

    /**
     * Submits the synthesized HTML and resolved media assets to the primary asset management system.
     *
     * @param projectContextKey project asset context key
     * @param document the successfully synthesized and audited DOM document
     * @param assets the resolved project media assets
     */
    void submitSynthesizedAssets(String projectContextKey, SynthesizedDomDocument document, Map<String, String> assets);
}
