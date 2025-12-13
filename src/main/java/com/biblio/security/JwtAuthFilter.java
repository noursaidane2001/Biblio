package com.biblio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Ne pas appliquer le filtre sur les endpoints publics (optimisation)
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        
        // 1. Essayer de lire depuis le header Authorization
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        }
        
        // 2. Si pas de token dans le header, essayer depuis un cookie
        if (token == null) {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("jwt_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        
        // 3. Si toujours pas de token, essayer depuis un paramètre de requête (pour OAuth2 callback)
        if (token == null) {
            token = request.getParameter("token");
        }
        
        // Si aucun token trouvé, continuer sans authentification
        if (token == null || !StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    handleError(response, "Token invalide ou expiré", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            handleError(response, "Token expiré", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (io.jsonwebtoken.security.SignatureException | io.jsonwebtoken.MalformedJwtException e) {
            handleError(response, "Token invalide ou malformé", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            logger.debug("JWT token validation failed", e);
            handleError(response, "Erreur lors de la validation du token", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        // Ne pas considérer /dashboard comme public - il nécessite une authentification
        return path.startsWith("/api/auth/") ||
               path.startsWith("/oauth2/") ||
               path.startsWith("/login/oauth2/") ||
               path.startsWith("/login") ||
               path.startsWith("/register") ||
               path.equals("/oauth2-callback") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/webjars/") ||
               path.equals("/") ||
               path.equals("/h2-console");
    }

    private void handleError(HttpServletResponse response, String message, int status) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "JWT Authentication Failed");
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
