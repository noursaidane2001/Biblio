package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.dto.CreateBibliothequeRequest;
import com.biblio.dto.CreateUserRequest;
import com.biblio.entities.Bibliotheque;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import com.biblio.services.AdminService;
import com.biblio.services.BibliothequeService;
import com.biblio.services.UserLogService;
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
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final BibliothequeService bibliothequeService;
    private final UserDAO userDAO;
    private final UserLogService userLogService;

    public AdminController(AdminService adminService, BibliothequeService bibliothequeService, UserDAO userDAO, UserLogService userLogService) {
        this.adminService = adminService;
        this.bibliothequeService = bibliothequeService;
        this.userDAO = userDAO;
        this.userLogService = userLogService;
    }

    /**
     * GET /api/admin/users
     * Liste tous les utilisateurs
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        try {
            var paged = adminService.getUsersPage(page, size);
            List<Map<String, Object>> users = paged.getContent().stream().map(this::userToMap).collect(Collectors.toList());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("users", users);
            result.put("page", paged.getNumber());
            result.put("size", paged.getSize());
            result.put("totalElements", paged.getTotalElements());
            result.put("totalPages", paged.getTotalPages());
            result.put("hasNext", paged.hasNext());
            result.put("hasPrevious", paged.hasPrevious());
            result.put("total", paged.getTotalElements());
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
            // Vérifier que le rôle est ADMIN ou BIBLIOTHECAIRE (pas USAGER ni SUPER_ADMIN)
            if (request.role() == Role.USAGER || request.role() == Role.SUPER_ADMIN) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Invalid role");
                error.put("message", "Les super administrateurs ne peuvent créer que des comptes ADMIN ou BIBLIOTHECAIRE");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            User user = adminService.createUser(
                    request.nom(),
                    request.prenom(),
                    request.email(),
                    request.password(),
                    request.role(),
                    request.bibliothequeId(),
                    request.emailVerifie(),
                    request.actif()
            );

            if (currentUser != null) {
                User actor = userDAO.findByEmail(currentUser.getUsername()).orElse(null);
                userLogService.log(actor, "CREATE_USER", "Création utilisateur " + user.getEmail(), "INFO");
            }

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
            if (currentUser != null) {
                User actor = userDAO.findByEmail(currentUser.getUsername()).orElse(null);
                userLogService.log(actor, "DELETE_USER", "Suppression utilisateur ID " + id, "WARN");
            }
            
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
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (currentUser != null) {
                User currentUserEntity = userDAO.findByEmail(currentUser.getUsername())
                        .orElseThrow(() -> new IllegalArgumentException("Utilisateur actuel non trouvé"));
                if (currentUserEntity.getId().equals(id) && currentUserEntity.isSuperAdmin()) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Cannot disable yourself");
                    error.put("message", "Le super administrateur ne peut pas désactiver son propre compte");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }
            User user = adminService.toggleUserStatus(id);
            if (currentUser != null) {
                User actor = userDAO.findByEmail(currentUser.getUsername()).orElse(null);
                userLogService.log(actor, "TOGGLE_STATUS", "Changement de statut pour " + user.getEmail() + " -> " + (user.getActif() ? "ACTIF" : "INACTIF"), "INFO");
            }
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
     * Récupère les logs des utilisateurs
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false) Long userId) {
        try {
            List<Map<String, Object>> logs = userLogService.getRecentLogs(limit, userId);
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

    /**
     * GET /api/admin/bibliotheques
     * Liste paginée de toutes les bibliothèques
     */
    @GetMapping("/bibliotheques")
    public ResponseEntity<Map<String, Object>> getAllBibliotheques(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        try {
            var pageable = org.springframework.data.domain.PageRequest.of(
                    Math.max(page, 0),
                    Math.max(size, 1),
                    org.springframework.data.domain.Sort.by("nom").ascending()
            );
            var paged = bibliothequeService.getAllPaged(pageable);

            List<Map<String, Object>> bibliotheques = paged.getContent().stream()
                    .map(this::bibliothequeToMap)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bibliotheques", bibliotheques);
            result.put("page", paged.getNumber());
            result.put("size", paged.getSize());
            result.put("totalElements", paged.getTotalElements());
            result.put("totalPages", paged.getTotalPages());
            result.put("hasNext", paged.hasNext());
            result.put("hasPrevious", paged.hasPrevious());
            result.put("total", paged.getTotalElements());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch bibliotheques");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /api/admin/bibliotheques/{id}
     * Met à jour une bibliothèque (SUPER_ADMIN)
     */
    @PutMapping("/bibliotheques/{id}")
    public ResponseEntity<Map<String, Object>> updateBibliotheque(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        try {
            Bibliotheque updated = bibliothequeService.updateBibliotheque(
                    id,
                    (String) updates.get("nom"),
                    (String) updates.get("adresse"),
                    (String) updates.get("ville"),
                    (String) updates.get("telephone"),
                    updates.get("capaciteStock") != null ? ((Number) updates.get("capaciteStock")).intValue() : null,
                    updates.get("actif") != null ? (Boolean) updates.get("actif") : null
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Bibliothèque mise à jour avec succès");
            result.put("bibliotheque", bibliothequeToMap(updated));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to update bibliotheque");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to update bibliotheque");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/admin/bibliotheques/actives
     * Liste toutes les bibliothèques actives
     */
    @GetMapping("/bibliotheques/actives")
    public ResponseEntity<Map<String, Object>> getActiveBibliotheques() {
        try {
            List<Map<String, Object>> bibliotheques = bibliothequeService.getAllActives().stream()
                    .map(this::bibliothequeToMap)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bibliotheques", bibliotheques);
            result.put("total", bibliotheques.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch bibliotheques");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/admin/bibliotheques
     * Crée une nouvelle bibliothèque
     */
    @PostMapping("/bibliotheques")
    public ResponseEntity<Map<String, Object>> createBibliotheque(
            @Valid @RequestBody CreateBibliothequeRequest request) {
        try {
            Bibliotheque bibliotheque = bibliothequeService.createBibliotheque(
                    request.nom(),
                    request.adresse(),
                    request.ville(),
                    request.telephone(),
                    request.capaciteStock(),
                    request.latitude(),
                    request.longitude()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Bibliothèque créée avec succès");
            result.put("bibliotheque", bibliothequeToMap(bibliotheque));
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to create bibliotheque");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to create bibliotheque");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /api/admin/bibliotheques/{id}
     * Supprime une bibliothèque
     */
    @DeleteMapping("/bibliotheques/{id}")
    public ResponseEntity<Map<String, Object>> deleteBibliotheque(@PathVariable Long id) {
        try {
            bibliothequeService.deleteBibliotheque(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Bibliothèque supprimée avec succès");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Bibliotheque not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to delete bibliotheque");
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

    private Map<String, Object> bibliothequeToMap(Bibliotheque bibliotheque) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", bibliotheque.getId());
        map.put("nom", bibliotheque.getNom());
        map.put("adresse", bibliotheque.getAdresse());
        map.put("ville", bibliotheque.getVille());
        map.put("telephone", bibliotheque.getTelephone());
        map.put("capaciteStock", bibliotheque.getCapaciteStock());
        map.put("actif", bibliotheque.getActif());
        map.put("latitude", bibliotheque.getLatitude());
        map.put("longitude", bibliotheque.getLongitude());
        return map;
    }
}
