package com.biblio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        
        String path = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        boolean isApiRequest = path.startsWith("/api/");
        boolean acceptsJson = acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE);
        
        // Pour les requêtes API ou qui acceptent JSON, retourner du JSON
        if (isApiRequest || acceptsJson) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("error", "Unauthorized");
            body.put("message", authException.getMessage() != null ? authException.getMessage() : "Full authentication is required to access this resource");
            body.put("path", path);
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", HttpServletResponse.SC_UNAUTHORIZED);

            objectMapper.writeValue(response.getOutputStream(), body);
        } else {
            // Pour les requêtes web (Thymeleaf), rediriger vers la page de login
            response.sendRedirect("/login?error=unauthorized");
        }
    }
}
