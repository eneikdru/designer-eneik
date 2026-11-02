package com.eneik.generated.designer.service;

import java.util.Map;

/**
 * Service providing real media assets and static resources for inclusion during DOM synthesis.
 */
public interface DesignAssetService {

    /**
     * Resolves project media asset URLs by category or context key.
     */
    Map<String, String> resolveProjectAssets(String projectContextKey);
}
