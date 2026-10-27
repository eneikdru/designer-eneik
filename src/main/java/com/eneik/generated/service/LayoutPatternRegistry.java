package com.eneik.generated.service;

import com.eneik.generated.model.LayoutPattern;
import com.eneik.generated.model.MediaAsset;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LayoutPatternRegistry {

    private final List<LayoutPattern> patterns = new CopyOnWriteArrayList<>();
    private final List<MediaAsset> seedMediaAssets = new CopyOnWriteArrayList<>();

    public LayoutPatternRegistry() {
        initializeDefaults();
    }

    public void initializeDefaults() {
        patterns.clear();
        seedMediaAssets.clear();

        // Swiss Editorial Asymmetry 7/5
        patterns.add(new LayoutPattern(
                "swiss-asymmetry-7-5",
                "Swiss Editorial Asymmetry 7/5",
                "grid-cols-12",
                "col-span-7 md:col-span-7 col-span-5 md:col-span-5 gap-6",
                "1px solid rgba(255, 255, 255, 0.07)",
                "#08090C"
        ));

        // Swiss Editorial Asymmetry 8/4
        patterns.add(new LayoutPattern(
                "swiss-asymmetry-8-4",
                "Swiss Editorial Asymmetry 8/4",
                "grid-cols-12",
                "col-span-8 md:col-span-8 col-span-4 md:col-span-4 gap-8",
                "1px solid rgba(255, 255, 255, 0.07)",
                "#08090C"
        ));

        // Bento Grid 2.0
        patterns.add(new LayoutPattern(
                "bento-grid-2",
                "Bento Grid 2.0",
                "bento-grid",
                "grid grid-cols-1 md:grid-cols-3 gap-4 auto-rows-[250px]",
                "1px solid rgba(255, 255, 255, 0.07)",
                "#11141C"
        ));

        // Multi-level Obsidian Dark Theme (#08090C -> #11141C)
        patterns.add(new LayoutPattern(
                "multilevel-obsidian",
                "Multi-level Obsidian",
                "layered-obsidian",
                "bg-[#08090C] text-white divide-y divide-white/[0.07] border border-white/[0.07]",
                "1px solid rgba(255, 255, 255, 0.07)",
                "#08090C"
        ));

        // Seed media assets
        seedMediaAssets.add(new MediaAsset(
                "seed-hero-banner",
                "Hero Abstract Dark Grid",
                "assets/images/hero-dark-grid.webp",
                "HERO",
                "image/webp"
        ));

        seedMediaAssets.add(new MediaAsset(
                "seed-avatar-placeholder",
                "Executive Avatar Seed",
                "assets/images/avatar-executive.png",
                "AVATAR",
                "image/png"
        ));

        seedMediaAssets.add(new MediaAsset(
                "seed-preview-mockup",
                "System UI Interface Preview",
                "assets/images/ui-mockup-preview.svg",
                "MOCKUP",
                "image/svg+xml"
        ));
    }

    public List<LayoutPattern> getPatterns() {
        return Collections.unmodifiableList(new ArrayList<>(patterns));
    }

    public Optional<LayoutPattern> findPatternById(String id) {
        return patterns.stream()
                .filter(p -> p.id().equalsIgnoreCase(id))
                .findFirst();
    }

    public List<MediaAsset> getSeedMediaAssets() {
        return Collections.unmodifiableList(new ArrayList<>(seedMediaAssets));
    }

    public Optional<MediaAsset> findMediaAssetById(String id) {
        return seedMediaAssets.stream()
                .filter(m -> m.id().equalsIgnoreCase(id))
                .findFirst();
    }

    public void registerPattern(LayoutPattern pattern) {
        patterns.add(pattern);
    }

    public void registerMediaAsset(MediaAsset asset) {
        seedMediaAssets.add(asset);
    }
}
