package com.eneik.generated.controller;

import com.eneik.generated.dto.CompileTokensRequest;
import com.eneik.generated.model.ContrastRule;
import com.eneik.generated.model.DesignTokens;
import com.eneik.generated.model.ProductArchetype;
import com.eneik.generated.model.TypographyConstraint;
import com.eneik.generated.security.ApiKeyAuthInterceptor;
import com.eneik.generated.service.DesignTokenCompiler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DesignTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DesignTokenCompiler compiler;

    @Value("${designer.security.api-key}")
    private String validApiKey;

    @Value("${designer.security.bearer-token}")
    private String validBearerToken;

    @Test
    @DisplayName("Given a request without auth header, when compiling tokens, then return 401 Unauthorized")
    void whenNoAuthHeaderProvided_thenReturns401() throws Exception {
        CompileTokensRequest request = new CompileTokensRequest("DEV_TOOLS");

        mockMvc.perform(post("/api/v1/design-tokens/compile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Given a request with invalid auth credentials, when compiling tokens, then return 403 Forbidden")
    void whenInvalidAuthHeaderProvided_thenReturns403() throws Exception {
        CompileTokensRequest request = new CompileTokensRequest("DEV_TOOLS");

        mockMvc.perform(post("/api/v1/design-tokens/compile")
                        .header(ApiKeyAuthInterceptor.API_KEY_HEADER, "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Given an empty request body, when compiling tokens, then return 400 Bad Request")
    void whenEmptyRequestBodyProvided_thenReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/design-tokens/compile")
                        .header(ApiKeyAuthInterceptor.API_KEY_HEADER, validApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Given a request with blank archetype, when compiling tokens, then return 400 Bad Request")
    void whenBlankArchetypeProvided_thenReturns400() throws Exception {
        CompileTokensRequest request = new CompileTokensRequest("   ");

        mockMvc.perform(post("/api/v1/design-tokens/compile")
                        .header(ApiKeyAuthInterceptor.API_KEY_HEADER, validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Archetype is required and cannot be blank"));
    }

    @Test
    @DisplayName("Given a valid request with API key, when compiling tokens, then return 200 OK with compiled tokens")
    void whenValidRequestWithApiKey_thenReturns200WithTokens() throws Exception {
        ContrastRule contrastRule = new ContrastRule("#F3F4F6", "#08090C", 16.5, true);
        DesignTokens tokens = new DesignTokens(
                ProductArchetype.DEV_TOOLS,
                Map.of("background", "#08090C", "text", "#F3F4F6"),
                List.of(TypographyConstraint.create("h1", "2rem", "5vw", "4rem")),
                contrastRule
        );

        when(compiler.compileTokens(anyString())).thenReturn(tokens);

        CompileTokensRequest request = new CompileTokensRequest("DEV_TOOLS");

        mockMvc.perform(post("/api/v1/design-tokens/compile")
                        .header(ApiKeyAuthInterceptor.API_KEY_HEADER, validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archetype").value("DEV_TOOLS"))
                .andExpect(jsonPath("$.colors.background").value("#08090C"))
                .andExpect(jsonPath("$.contrastRule.meetsWcagAaa").value(true));
    }

    @Test
    @DisplayName("Given a valid request with Bearer token, when compiling tokens, then return 200 OK")
    void whenValidRequestWithBearerToken_thenReturns200WithTokens() throws Exception {
        ContrastRule contrastRule = new ContrastRule("#FFFFFF", "#08090C", 18.0, true);
        DesignTokens tokens = new DesignTokens(
                ProductArchetype.DEFAULT,
                Map.of("background", "#08090C", "text", "#FFFFFF"),
                List.of(TypographyConstraint.create("h1", "2rem", "5vw", "4rem")),
                contrastRule
        );

        when(compiler.compileTokens(anyString())).thenReturn(tokens);

        CompileTokensRequest request = new CompileTokensRequest("DEFAULT");

        mockMvc.perform(post("/api/v1/design-tokens/compile")
                        .header(ApiKeyAuthInterceptor.AUTH_HEADER, "Bearer " + validBearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archetype").value("DEFAULT"));
    }
}
