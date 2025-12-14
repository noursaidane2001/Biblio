package com.biblio.services;

import com.biblio.dao.UserDAO;
import com.biblio.dto.AuthResponse;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import com.biblio.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final long accessExpirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserDAO userDAO,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            AuthenticationManager authenticationManager,
            EmailService emailService,
            @Value("${jwt.expiration}") long accessExpirationMs) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.accessExpirationSeconds = accessExpirationMs / 1000; // Convertir en secondes
    }

    /**
     * Authentifie un utilisateur et retourne access + refresh tokens
     */
    public AuthResponse authenticate(String email, String password) {
        // Charger l'utilisateur pour vérifier son statut avant l'authentification
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Email ou mot de passe incorrect"));
        
        // Vérifier explicitement que l'utilisateur est activé (actif ET email vérifié)
        if (!user.isEnabled()) {
            if (!user.getActif()) {
                throw new org.springframework.security.authentication.DisabledException("Votre compte est désactivé. Veuillez contacter l'administrateur.");
            }
            if (!user.getEmailVerifie()) {
                throw new org.springframework.security.authentication.DisabledException("Votre email n'est pas vérifié. Veuillez vérifier votre email avant de vous connecter.");
            }
            throw new org.springframework.security.authentication.DisabledException("Votre compte est désactivé ou votre email n'est pas vérifié.");
        }
        
        // Authentifier et récupérer l'Authentication qui contient déjà le UserDetails
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        // Utiliser le principal de l'Authentication (évite de recharger l'utilisateur)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User authenticatedUser = (User) userDetails;
        
        // Vérification supplémentaire après authentification
        if (!authenticatedUser.isEnabled()) {
            throw new org.springframework.security.authentication.DisabledException("Votre compte est désactivé ou votre email n'est pas vérifié.");
        }
        
        // Générer access + refresh tokens
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        // Construire les infos utilisateur
        Map<String, Object> userInfo = getUserInfoMap(authenticatedUser);

        return AuthResponse.of(accessToken, refreshToken, accessExpirationSeconds, userInfo);
    }

    /**
     * Enregistre un nouvel utilisateur, envoie un email de vérification et retourne les tokens
     */
    @Transactional
    public AuthResponse register(String nom, String prenom, String email, String password) {
        if (userDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }

        // Générer un token de vérification unique
        String verificationToken = generateVerificationToken();

        User user = new User();
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setMotDePasse(passwordEncoder.encode(password));
        user.setEmailVerifie(false); // L'email n'est pas vérifié au départ
        user.setActif(true); // Le compte est actif mais non vérifié
        user.setRole(Role.USAGER);
        user.setTokenVerification(verificationToken);

        userDAO.save(user);

        // Générer access + refresh tokens même si l'email n'est pas vérifié
        // (l'utilisateur pourra utiliser l'API mais certaines fonctionnalités peuvent être limitées)
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // Envoyer l'email de vérification (ne bloque pas si l'envoi échoue)
        try {
            emailService.sendVerificationEmail(email, nom, prenom, verificationToken);
        } catch (Exception e) {
            // Logger l'erreur mais ne pas bloquer l'inscription
            // L'utilisateur pourra demander un nouvel email de vérification plus tard
            logger.warn("Impossible d'envoyer l'email de vérification à {} : {}", email, e.getMessage());
        }
        
        Map<String, Object> userInfo = getUserInfoMap(user);

        return AuthResponse.of(accessToken, refreshToken, accessExpirationSeconds, userInfo);
    }
    
    /**
     * Vérifie le token d'email et active le compte utilisateur.
     */
    @Transactional
    public boolean verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        User user = userDAO.findByTokenVerification(token).orElse(null);
        if (user == null) {
            return false;
        }
        
        // Vérifier si l'email n'est pas déjà vérifié
        if (user.getEmailVerifie()) {
            return true; // Déjà vérifié
        }
        
        // Activer le compte et supprimer le token
        user.setEmailVerifie(true);
        user.setTokenVerification(null);
        userDAO.save(user);
        
        return true;
    }
    
    /**
     * Génère un token de vérification sécurisé.
     */
    private String generateVerificationToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Rafraîchit les tokens en utilisant un refresh token.
     * Génère un nouveau access token ET un nouveau refresh token (rotation).
     */
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Refresh token invalide ou expiré");
        }

        // Extraire le username depuis le refresh token
        String username = jwtService.extractUsernameFromRefreshToken(refreshToken);
        
        // Charger UserDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        User user = (User) userDetails;
        
        // Générer NOUVEAU access token ET NOUVEAU refresh token (rotation)
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        
        Map<String, Object> userInfo = getUserInfoMap(user);

        return AuthResponse.of(newAccessToken, newRefreshToken, accessExpirationSeconds, userInfo);
    }

    /**
     * Retourne les informations de l'utilisateur pour l'endpoint /me
     */
    public Map<String, Object> getUserInfo(UserDetails userDetails) {
        User user = (User) userDetails;
        return getUserInfoMap(user);
    }

    /**
     * Construit un Map avec les informations utilisateur
     */
    private Map<String, Object> getUserInfoMap(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getUsername()); // getUsername() retourne l'email
        userInfo.put("nom", user.getNom());
        userInfo.put("prenom", user.getPrenom());
        userInfo.put("role", user.getRole().name());
        userInfo.put("emailVerifie", user.getEmailVerifie());
        userInfo.put("actif", user.getActif());
        if (user.getBibliotheque() != null) {
            userInfo.put("bibliotheque_id", user.getBibliotheque().getId());
        }
        return userInfo;
    }
}
