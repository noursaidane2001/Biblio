package com.biblio.controllers;

import com.biblio.dto.AuthResponse;
import com.biblio.dto.LoginRequest;
import com.biblio.dto.RefreshTokenRequest;
import com.biblio.dto.RegisterRequest;
import com.biblio.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/login
     * Authentifie un utilisateur avec email/password et retourne access + refresh tokens
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.authenticate(request.email(), request.password());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("accessToken", response.accessToken());
            result.put("refreshToken", response.refreshToken());
            result.put("tokenType", response.tokenType());
            result.put("expiresIn", response.expiresIn());
            result.put("user", response.user());
            return ResponseEntity.ok(result);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Authentication failed");
            error.put("message", "Email ou mot de passe incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (org.springframework.security.authentication.DisabledException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Account disabled");
            error.put("message", "Votre compte est désactivé ou votre email n'est pas vérifié. Veuillez vérifier votre email.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (org.springframework.security.authentication.LockedException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Account locked");
            error.put("message", "Votre compte est verrouillé. Veuillez contacter l'administrateur.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Authentication failed");
            error.put("message", e.getMessage() != null ? e.getMessage() : "Identifiants invalides");
            error.put("exceptionType", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * POST /api/auth/register
     * Crée un nouveau compte et retourne access + refresh tokens
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(
                    request.nom(),
                    request.prenom(),
                    request.email(),
                    request.password()
            );
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("accessToken", response.accessToken());
            result.put("refreshToken", response.refreshToken());
            result.put("tokenType", response.tokenType());
            result.put("expiresIn", response.expiresIn());
            result.put("user", response.user());
            result.put("message", "Compte créé avec succès ! Veuillez vérifier votre email pour activer votre compte.");
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Registration failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            // Logger l'erreur pour le debugging
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Registration failed");
            error.put("message", e.getMessage() != null ? e.getMessage() : "Erreur lors de l'inscription");
            error.put("details", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/auth/refresh
     * Rafraîchit les tokens en utilisant un refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authService.refreshToken(request.refreshToken());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("accessToken", response.accessToken());
            result.put("refreshToken", response.refreshToken());
            result.put("tokenType", response.tokenType());
            result.put("expiresIn", response.expiresIn());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Token refresh failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Token refresh failed");
            error.put("message", "Erreur lors du renouvellement du token");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/auth/me
     * Retourne les informations de l'utilisateur authentifié (protégé par JWT)
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Unauthorized");
            error.put("message", "Non authentifié");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        try {
            Map<String, Object> userInfo = authService.getUserInfo(userDetails);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("user", userInfo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Internal server error");
            error.put("message", "Erreur lors de la récupération des informations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/auth/logout
     * Logout (stateless - le client doit simplement supprimer les tokens)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Déconnexion réussie. Veuillez supprimer les tokens côté client.");
        return ResponseEntity.ok(result);
    }
}
