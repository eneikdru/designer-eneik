package com.eneik.generated.model;

import java.util.Objects;

public record MediaAsset(
        String id,
        String title,
        String url,
        String category,
        String mimeType
) {
    public MediaAsset {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(url, "url must not be null");
    }
}
