package com.biblio.security;

import com.biblio.entities.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuth2JwtSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final String frontendRedirectUri;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuth2JwtSuccessHandler(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}") String frontendRedirectUri) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            handleError(response, "Email non disponible depuis OAuth2");
            return;
        }

        // Charger UserDetails complet depuis la DB
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        User user = (User) userDetails;

        // Générer access + refresh tokens JWT
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Vérifier si la requête accepte JSON (pour les apps mobiles)
        String acceptHeader = request.getHeader("Accept");
        boolean acceptsJson = acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE);

        if (acceptsJson) {
            // Retourner JSON pour les apps mobiles
            sendJsonResponse(response, accessToken, refreshToken, user);
        } else {
            // Rediriger vers la page de callback OAuth2 avec tokens dans l'URL
            // La page de callback stockera les tokens dans localStorage et redirigera vers /dashboard
            String redirectUrl = String.format("/oauth2-callback?token=%s&refresh=%s",
                    java.net.URLEncoder.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(refreshToken, java.nio.charset.StandardCharsets.UTF_8));
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }

    private void sendJsonResponse(HttpServletResponse response, String accessToken, String refreshToken, User user) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getUsername()); // getUsername() retourne l'email
        userInfo.put("nom", user.getNom());
        userInfo.put("prenom", user.getPrenom());
        userInfo.put("role", user.getRole().name());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("accessToken", accessToken);
        body.put("refreshToken", refreshToken);
        body.put("tokenType", "Bearer");
        body.put("expiresIn", 3600L); // 1 heure en secondes
        body.put("user", userInfo);

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private void handleError(HttpServletResponse response, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "OAuth2 authentication failed");
        body.put("message", message);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
