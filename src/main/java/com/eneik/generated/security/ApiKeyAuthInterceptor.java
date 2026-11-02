package com.eneik.generated.security;

import com.eneik.generated.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-API-KEY";
    public static final String AUTH_HEADER = "Authorization";

    private final ObjectMapper objectMapper;
    private final String expectedApiKey;
    private final String expectedBearerToken;

    public ApiKeyAuthInterceptor(
            ObjectMapper objectMapper,
            @Value("${designer.security.api-key}") String expectedApiKey,
            @Value("${designer.security.bearer-token}") String expectedBearerToken
    ) {
        this.objectMapper = objectMapper;
        this.expectedApiKey = expectedApiKey;
        this.expectedBearerToken = expectedBearerToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader(API_KEY_HEADER);
        String authHeader = request.getHeader(AUTH_HEADER);

        if (apiKey == null && authHeader == null) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing authentication credentials");
            return false;
        }

        boolean apiKeyValid = expectedApiKey != null && expectedApiKey.equals(apiKey);
        boolean tokenValid = expectedBearerToken != null && ("Bearer " + expectedBearerToken).equals(authHeader);

        if (!apiKeyValid && !tokenValid) {
            writeErrorResponse(response, HttpStatus.FORBIDDEN, "Invalid authentication credentials");
            return false;
        }

        return true;
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse error = ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
