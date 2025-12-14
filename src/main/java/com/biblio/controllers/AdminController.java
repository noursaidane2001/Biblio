package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.dto.CreateUserRequest;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import com.biblio.services.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final UserDAO userDAO;

    public AdminController(AdminService adminService, UserDAO userDAO) {
        this.adminService = adminService;
        this.userDAO = userDAO;
    }

    /**
     * GET /api/admin/users
     * Liste tous les utilisateurs
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            List<Map<String, Object>> users = adminService.getAllUsers().stream()
                    .map(this::userToMap)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("users", users);
            result.put("total", users.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch users");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/admin/users/{id}
     * Récupère un utilisateur par son ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        try {
            User user = adminService.getUserById(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("user", userToMap(user));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * POST /api/admin/users
     * Crée un nouvel utilisateur (admin ou bibliothécaire)
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            // Vérifier que le rôle est ADMIN ou BIBLIOTHECAIRE (pas USAGER)
            if (request.role() == Role.USAGER) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Invalid role");
                error.put("message", "Les administrateurs ne peuvent créer que des comptes ADMIN ou BIBLIOTHECAIRE");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            User user = adminService.createUser(
                    request.nom(),
                    request.prenom(),
                    request.email(),
                    request.password(),
                    request.role(),
                    request.emailVerifie(),
                    request.actif()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Utilisateur créé avec succès");
            result.put("user", userToMap(user));
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to create user");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to create user");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /api/admin/users/{id}
     * Supprime un utilisateur
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            // Empêcher l'auto-suppression
            User currentUserEntity = userDAO.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur actuel non trouvé"));
            
            if (currentUserEntity.getId().equals(id)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Cannot delete yourself");
                error.put("message", "Vous ne pouvez pas supprimer votre propre compte");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            adminService.deleteUser(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Utilisateur supprimé avec succès");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to delete user");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /api/admin/users/{id}/toggle-status
     * Active ou désactive un utilisateur
     */
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable Long id) {
        try {
            User user = adminService.toggleUserStatus(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Statut de l'utilisateur modifié avec succès");
            result.put("user", userToMap(user));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * GET /api/admin/logs
     * Récupère les logs système
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(required = false, defaultValue = "100") int limit) {
        try {
            List<Map<String, Object>> logs = adminService.getSystemLogs(limit);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("logs", logs);
            result.put("total", logs.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch logs");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/admin/stats
     * Récupère les statistiques du système
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = adminService.getSystemStats();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("stats", stats);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch stats");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("nom", user.getNom());
        userMap.put("prenom", user.getPrenom());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().name());
        userMap.put("roleDisplay", user.getRole().getDisplayName());
        userMap.put("actif", user.getActif());
        userMap.put("emailVerifie", user.getEmailVerifie());
        userMap.put("dateInscription", user.getDateInscription());
        if (user.getBibliotheque() != null) {
            userMap.put("bibliotheque", Map.of(
                    "id", user.getBibliotheque().getId(),
                    "nom", user.getBibliotheque().getNom()
            ));
        }
        return userMap;
    }
}
