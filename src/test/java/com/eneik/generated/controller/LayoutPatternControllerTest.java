package com.eneik.generated.controller;

import com.eneik.generated.security.ApiKeyAuthInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LayoutPatternControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${designer.security.api-key}")
    private String validApiKey;

    @Value("${designer.security.bearer-token}")
    private String validBearerToken;

    @Test
    @DisplayName("Given unauthenticated request to layout patterns, then return 401 Unauthorized")
    void whenUnauthenticated_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/layout-patterns")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Given invalid credentials to layout patterns, then return 403 Forbidden")
    void whenInvalidCredentials_thenReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/layout-patterns")
                        .header(ApiKeyAuthInterceptor.API_KEY_HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Given valid credentials, when getting layout patterns, then return 200 OK with patterns list")
    void whenAuthenticated_thenReturnsPatterns() throws Exception {
        mockMvc.perform(get("/api/v1/layout-patterns")
                        .header(ApiKeyAuthInterceptor.API_KEY_HEADER, validApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @DisplayName("Given valid credentials and existing pattern id, then return 200 OK with pattern details")
    void whenAuthenticatedAndPatternExists_thenReturnsPattern() throws Exception {
        mockMvc.perform(get("/api/v1/layout-patterns/swiss-asymmetry-7-5")
                        .header(ApiKeyAuthInterceptor.API_KEY_HEADER, validApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("swiss-asymmetry-7-5"))
                .andExpect(jsonPath("$.name").value("Swiss Editorial Asymmetry 7/5"));
    }

    @Test
    @DisplayName("Given valid credentials and non-existing pattern id, then return 404 Not Found")
    void whenAuthenticatedAndPatternNotFound_thenReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/layout-patterns/non-existent-pattern-xyz")
                        .header(ApiKeyAuthInterceptor.API_KEY_HEADER, validApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Given valid credentials, when getting media assets, then return 200 OK with media assets list")
    void whenAuthenticated_thenReturnsMediaAssets() throws Exception {
        mockMvc.perform(get("/api/v1/media-assets")
                        .header(ApiKeyAuthInterceptor.AUTH_HEADER, "Bearer " + validBearerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].category").exists());
    }
}
