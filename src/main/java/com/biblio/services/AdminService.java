package com.biblio.services;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    
    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Récupère tous les utilisateurs
     */
    public List<User> getAllUsers() {
        return userDAO.findAllOrderByDateInscriptionDesc();
    }

    /**
     * Récupère un utilisateur par son ID
     */
    public User getUserById(Long id) {
        return userDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + id));
    }

    /**
     * Crée un nouvel utilisateur (admin ou bibliothécaire)
     */
    @Transactional
    public User createUser(String nom, String prenom, String email, String password, 
                          Role role, Boolean emailVerifie, Boolean actif) {
        if (userDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        if (role == Role.USAGER) {
            throw new IllegalArgumentException("Les administrateurs ne peuvent créer que des comptes ADMIN ou BIBLIOTHECAIRE");
        }

        User user = new User();
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setMotDePasse(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEmailVerifie(emailVerifie != null ? emailVerifie : true);
        user.setActif(actif != null ? actif : true);
        user.setDateInscription(LocalDateTime.now());

        User savedUser = userDAO.save(user);
        logger.info("Utilisateur créé par admin: {} (ID: {}, Rôle: {})", email, savedUser.getId(), role);
        
        return savedUser;
    }

    /**
     * Supprime un utilisateur
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + id));
        
        logger.info("Suppression de l'utilisateur: {} (ID: {}, Rôle: {})", 
                user.getEmail(), user.getId(), user.getRole());
        
        userDAO.delete(user);
    }

    /**
     * Active ou désactive un utilisateur
     */
    @Transactional
    public User toggleUserStatus(Long id) {
        User user = userDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + id));
        
        user.setActif(!user.getActif());
        User updatedUser = userDAO.save(user);
        
        logger.info("Statut de l'utilisateur {} modifié: {}", updatedUser.getEmail(), 
                updatedUser.getActif() ? "ACTIF" : "INACTIF");
        
        return updatedUser;
    }

    /**
     * Récupère les logs système (simulés pour l'instant)
     * Dans une vraie application, vous utiliseriez un système de logging comme Logback ou Log4j2
     */
    public List<Map<String, Object>> getSystemLogs(int limit) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        // Simuler des logs système
        // Dans une vraie application, vous liriez depuis un fichier de log ou une base de données
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < Math.min(limit, 20); i++) {
            Map<String, Object> log = new HashMap<>();
            log.put("timestamp", now.minusMinutes(i * 5));
            log.put("level", i % 3 == 0 ? "INFO" : (i % 3 == 1 ? "WARN" : "ERROR"));
            log.put("message", "Log système " + (i + 1) + " - Action effectuée");
            log.put("user", "admin@example.com");
            logs.add(log);
        }
        
        return logs;
    }

    /**
     * Récupère les statistiques du système
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userDAO.count();
        long adminCount = userDAO.countByRole(Role.ADMIN);
        long bibliothecaireCount = userDAO.countByRole(Role.BIBLIOTHECAIRE);
        long usagerCount = userDAO.countByRole(Role.USAGER);
        
        stats.put("totalUsers", totalUsers);
        stats.put("admins", adminCount);
        stats.put("bibliothecaires", bibliothecaireCount);
        stats.put("usagers", usagerCount);
        stats.put("activeUsers", userDAO.findAll().stream()
                .filter(User::getActif)
                .count());
        stats.put("verifiedUsers", userDAO.findAll().stream()
                .filter(User::getEmailVerifie)
                .count());
        
        return stats;
    }
}
