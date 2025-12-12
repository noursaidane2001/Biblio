package com.biblio.services;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import com.biblio.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {
    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserDAO userDAO,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            AuthenticationManager authenticationManager) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
    }

    public Map<String, String> authenticate(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtService.generateToken(userDetails);

        return Map.of("token", token, "username", userDetails.getUsername());
    }

    public Map<String, String> register(String nom, String prenom, String email, String password) {
        if (userDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }

        User user = new User();
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setMotDePasse(passwordEncoder.encode(password));
        user.setEmailVerifie(true);
        user.setActif(true);
        user.setRole(Role.USAGER);

        userDAO.save(user);

        return Map.of("status", "created", "message", "Compte créé avec succès");
    }

    public Map<String, String> generateTokenForAuthenticatedUser(UserDetails userDetails) {
        String token = jwtService.generateToken(userDetails);
        return Map.of("token", token, "username", userDetails.getUsername());
    }
}

