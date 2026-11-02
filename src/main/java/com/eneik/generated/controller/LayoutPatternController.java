package com.eneik.generated.controller;

import com.eneik.generated.model.LayoutPattern;
import com.eneik.generated.model.MediaAsset;
import com.eneik.generated.service.LayoutPatternRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LayoutPatternController {

    private final LayoutPatternRegistry registry;

    public LayoutPatternController(LayoutPatternRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/layout-patterns")
    public ResponseEntity<List<LayoutPattern>> getAllPatterns() {
        return ResponseEntity.ok(registry.getPatterns());
    }

    @GetMapping("/layout-patterns/{id}")
    public ResponseEntity<LayoutPattern> getPatternById(@PathVariable String id) {
        return registry.findPatternById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Layout pattern not found: " + id));
    }

    @GetMapping("/media-assets")
    public ResponseEntity<List<MediaAsset>> getAllMediaAssets() {
        return ResponseEntity.ok(registry.getSeedMediaAssets());
    }
}
