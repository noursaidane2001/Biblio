package com.biblio.security;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserDAO userDAO, @Lazy PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Génère un mot de passe aléatoire pour les utilisateurs OAuth2.
     * Ce mot de passe n'est jamais utilisé mais permet de respecter les contraintes de validation.
     */
    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return passwordEncoder.encode(Base64.getEncoder().encodeToString(bytes));
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Charger les informations OAuth2 depuis Google
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // Extraire les attributs Google
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");
        String picture = (String) attributes.get("picture");
        
        if (email == null) {
            throw new OAuth2AuthenticationException("Email non disponible depuis Google");
        }
        
        // Vérifier si l'utilisateur existe déjà
        User user = userDAO.findByEmail(email).orElse(null);
        
        if (user == null) {
            // Créer un nouvel utilisateur OAuth2
            user = new User();
            user.setEmail(email);
            
            // S'assurer que les valeurs respectent les contraintes de validation
            // Le nom et prénom doivent contenir entre 2 et 50 caractères
            String prenom = (givenName != null && givenName.trim().length() >= 2) 
                ? givenName.trim() 
                : (email != null ? email.split("@")[0] : "Utilisateur");
            if (prenom.length() > 50) {
                prenom = prenom.substring(0, 50);
            }
            
            String nom = (familyName != null && familyName.trim().length() >= 2) 
                ? familyName.trim() 
                : (prenom != null && prenom.trim().length() >= 2 ? prenom.trim() : "Utilisateur");
            if (nom.length() > 50) {
                nom = nom.substring(0, 50);
            }
            
            user.setPrenom(prenom);
            user.setNom(nom);
            user.setEmailVerifie(true);
            user.setActif(true);
            user.setRole(Role.USAGER);
            // Générer un mot de passe aléatoire (non utilisé, mais requis pour la validation)
            // Les utilisateurs OAuth2 s'authentifient uniquement via Google
            user.setMotDePasse(generateRandomPassword());
            
            userDAO.save(user);
        } else {
            // Mettre à jour les informations si elles ont changé
            boolean updated = false;
            if (givenName != null && givenName.trim().length() >= 2 && !givenName.trim().equals(user.getPrenom())) {
                String prenom = givenName.trim();
                if (prenom.length() > 50) {
                    prenom = prenom.substring(0, 50);
                }
                user.setPrenom(prenom);
                updated = true;
            }
            if (familyName != null && familyName.trim().length() >= 2 && !familyName.trim().equals(user.getNom())) {
                String nom = familyName.trim();
                if (nom.length() > 50) {
                    nom = nom.substring(0, 50);
                }
                user.setNom(nom);
                updated = true;
            }
            if (updated) {
                userDAO.save(user);
            }
        }
        
        // Créer les authorities basées sur le rôle
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        
        // Retourner un DefaultOAuth2User avec les authorities
        return new DefaultOAuth2User(
            Collections.singleton(authority),
            attributes,
            "email" // nameAttributeKey
        );
    }
}
