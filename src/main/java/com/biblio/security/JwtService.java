package com.biblio.security;

import com.biblio.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String accessSecret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.expiration}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
        this.accessSecretKey = Keys.hmacShaKeyFor(getSecretKeyBytes(accessSecret));
        this.refreshSecretKey = Keys.hmacShaKeyFor(getSecretKeyBytes(refreshSecret));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Génère un access token avec les claims personnalisés : username, nom, prenom, role, userId
     */
    public String generateAccessToken(UserDetails userDetails) {
        User user = (User) userDetails;
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("username", user.getUsername()); // getUsername() retourne l'email
        claims.put("nom", user.getNom());
        claims.put("prenom", user.getPrenom());
        String fullName = ((user.getPrenom() != null ? user.getPrenom() : "") + " " + (user.getNom() != null ? user.getNom() : "")).trim();
        claims.put("name", fullName); // Nom complet
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());
        claims.put("roles", "ROLE_" + user.getRole().name());
        
        return buildToken(claims, userDetails.getUsername(), accessExpirationMs, accessSecretKey);
    }

    /**
     * Génère un refresh token avec seulement le type et le subject
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return buildToken(claims, userDetails.getUsername(), refreshExpirationMs, refreshSecretKey);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs, SecretKey secretKey) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration);
        
        // Ajouter tous les claims personnalisés
        extraClaims.forEach(builder::claim);
        
        return builder.signWith(secretKey).compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject, accessSecretKey);
    }

    public String extractUsernameFromRefreshToken(String token) {
        return extractClaim(token, Claims::getSubject, refreshSecretKey);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration, accessSecretKey);
    }

    public <T> T extractClaim(String token, String claimName, Class<T> claimType) {
        final Claims claims = extractAllClaims(token, accessSecretKey);
        return claims.get(claimName, claimType);
    }

    public Object extractClaim(String token, String claimName) {
        final Claims claims = extractAllClaims(token, accessSecretKey);
        return claims.get(claimName);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Vérifie si un refresh token est valide
     */
    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token, refreshSecretKey);
            String type = claims.get("type", String.class);
            return "refresh".equals(type) && !isTokenExpired(token, refreshSecretKey);
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver, SecretKey secretKey) {
        final Claims claims = extractAllClaims(token, secretKey);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, SecretKey secretKey) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return isTokenExpired(token, accessSecretKey);
    }

    private boolean isTokenExpired(String token, SecretKey secretKey) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration, secretKey);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private byte[] getSecretKeyBytes(String secret) {
        // S'assurer que la clé fait au moins 256 bits (32 bytes)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Padding avec des zéros si nécessaire (pour le développement uniquement)
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return padded;
        }
        return keyBytes;
    }
}
