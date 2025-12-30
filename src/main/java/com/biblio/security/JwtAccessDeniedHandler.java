package com.biblio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        
        String path = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        boolean isApiRequest = path.startsWith("/api/");
        boolean acceptsJson = acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE);
        
        // Pour les requêtes API ou qui acceptent JSON, retourner du JSON
        if (isApiRequest || acceptsJson) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("error", "Forbidden");
            body.put("message", accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Access denied. You don't have permission to access this resource.");
            body.put("path", path);
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", HttpServletResponse.SC_FORBIDDEN);

            objectMapper.writeValue(response.getOutputStream(), body);
        } else {
            // Pour les requêtes web (Thymeleaf), rediriger vers une page d'erreur
            response.sendRedirect("/login?error=forbidden");
        }
    }
}
