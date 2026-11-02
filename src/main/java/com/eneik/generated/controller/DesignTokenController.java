package com.eneik.generated.controller;

import com.eneik.generated.dto.CompileTokensRequest;
import com.eneik.generated.model.DesignTokens;
import com.eneik.generated.service.DesignTokenCompiler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/design-tokens")
public class DesignTokenController {

    private final DesignTokenCompiler compiler;

    public DesignTokenController(DesignTokenCompiler compiler) {
        this.compiler = compiler;
    }

    @PostMapping("/compile")
    public ResponseEntity<DesignTokens> compileTokens(@RequestBody(required = false) CompileTokensRequest request) {
        if (request == null || request.getArchetype() == null || request.getArchetype().trim().isEmpty()) {
            throw new IllegalArgumentException("Archetype is required and cannot be blank");
        }

        DesignTokens tokens = compiler.compileTokens(request.getArchetype().trim());
        return ResponseEntity.ok(tokens);
    }
}
