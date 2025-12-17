package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.dto.CreateUserRequest;
import com.biblio.entities.Bibliotheque;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import com.biblio.services.AdminService;
import com.biblio.services.BibliothequeService;
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
@RequestMapping("/api/bibliotheque-admin")
@PreAuthorize("hasRole('ADMIN')")
public class BibliothequeAdminController {
    private final AdminService adminService;
    private final BibliothequeService bibliothequeService;
    private final UserDAO userDAO;

    public BibliothequeAdminController(AdminService adminService, BibliothequeService bibliothequeService, UserDAO userDAO) {
        this.adminService = adminService;
        this.bibliothequeService = bibliothequeService;
        this.userDAO = userDAO;
    }

    /**
     * GET /api/bibliotheque-admin/bibliotheque
     * Récupère la bibliothèque de l'administrateur connecté
     */
    @GetMapping("/bibliotheque")
    public ResponseEntity<Map<String, Object>> getMyBibliotheque(
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            User admin = userDAO.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            
            if (admin.getBibliotheque() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "No bibliotheque");
                error.put("message", "Vous n'êtes associé à aucune bibliothèque");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bibliotheque", bibliothequeToMap(admin.getBibliotheque()));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * PUT /api/bibliotheque-admin/bibliotheque
     * Met à jour la bibliothèque de l'administrateur connecté
     */
    @PutMapping("/bibliotheque")
    public ResponseEntity<Map<String, Object>> updateMyBibliotheque(
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            User admin = userDAO.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            
            if (admin.getBibliotheque() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "No bibliotheque");
                error.put("message", "Vous n'êtes associé à aucune bibliothèque");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Long bibliothequeId = admin.getBibliotheque().getId();
            Bibliotheque updated = bibliothequeService.updateBibliotheque(
                    bibliothequeId,
                    (String) updates.get("nom"),
                    (String) updates.get("adresse"),
                    (String) updates.get("ville"),
                    (String) updates.get("telephone"),
                    updates.get("capaciteStock") != null ? ((Number) updates.get("capaciteStock")).intValue() : null,
                    updates.get("actif") != null ? (Boolean) updates.get("actif") : null,
                    updates.get("latitude") != null ? ((Number) updates.get("latitude")).doubleValue() : null,
                    updates.get("longitude") != null ? ((Number) updates.get("longitude")).doubleValue() : null
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
        }
    }

    /**
     * GET /api/bibliotheque-admin/utilisateurs
     * Liste tous les utilisateurs (ADMIN et BIBLIOTHECAIRE) de la bibliothèque de l'administrateur
     */
    @GetMapping("/utilisateurs")
    public ResponseEntity<Map<String, Object>> getMyUtilisateurs(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        try {
            User admin = userDAO.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            
            if (admin.getBibliotheque() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "No bibliotheque");
                error.put("message", "Vous n'êtes associé à aucune bibliothèque");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Long bibliothequeId = admin.getBibliotheque().getId();
            var pageable = org.springframework.data.domain.PageRequest.of(
                    Math.max(page, 0),
                    Math.max(size, 1),
                    org.springframework.data.domain.Sort.by("dateInscription").descending()
            );
            var paged = userDAO.findByBibliotheque_IdAndRole(bibliothequeId, Role.BIBLIOTHECAIRE, pageable);

            List<Map<String, Object>> utilisateursList = paged.getContent().stream()
                    .map(this::userToMap)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("utilisateurs", utilisateursList);
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
            error.put("error", "Failed to fetch utilisateurs");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/bibliotheque-admin/bibliothecaires
     * Liste tous les bibliothécaires de la bibliothèque de l'administrateur
     */
    @GetMapping("/bibliothecaires")
    public ResponseEntity<Map<String, Object>> getMyBibliothecaires(
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            User admin = userDAO.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            
            if (admin.getBibliotheque() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "No bibliotheque");
                error.put("message", "Vous n'êtes associé à aucune bibliothèque");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Long bibliothequeId = admin.getBibliotheque().getId();

            List<User> bibliothecaires = userDAO.findAll().stream()
                    .filter(u -> u.getRole() == Role.BIBLIOTHECAIRE
                            && u.getBibliotheque() != null
                            && u.getBibliotheque().getId() == bibliothequeId
                    )
                    .collect(Collectors.toList());

            List<Map<String, Object>> bibliothecairesList = bibliothecaires.stream()
                    .map(this::userToMap)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bibliothecaires", bibliothecairesList);
            result.put("total", bibliothecairesList.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch bibliothecaires");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/bibliotheque-admin/utilisateurs
     * Crée un nouvel utilisateur (ADMIN ou BIBLIOTHECAIRE) pour la bibliothèque de l'administrateur
     */
    @PostMapping("/utilisateurs")
    public ResponseEntity<Map<String, Object>> createUtilisateur(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            User admin = userDAO.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            
            if (admin.getBibliotheque() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "No bibliotheque");
                error.put("message", "Vous n'êtes associé à aucune bibliothèque");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            if (request.role() != Role.BIBLIOTHECAIRE) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Invalid role");
                error.put("message", "Vous ne pouvez créer que des bibliothécaires pour votre bibliothèque");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Forcer la bibliothèque de l'admin
            User user = adminService.createUser(
                    request.nom(),
                    request.prenom(),
                    request.email(),
                    request.password(),
                    request.role(),
                    admin.getBibliotheque().getId(), // Utiliser la bibliothèque de l'admin
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
            error.put("error", "Failed to create utilisateur");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to create utilisateur");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/bibliotheque-admin/bibliothecaires
     * Crée un nouveau bibliothécaire pour la bibliothèque de l'administrateur (compatibilité)
     */
    @PostMapping("/bibliothecaires")
    public ResponseEntity<Map<String, Object>> createBibliothecaire(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        // Rediriger vers l'endpoint utilisateurs
        return createUtilisateur(request, currentUser);
    }

    /**
     * DELETE /api/bibliotheque-admin/utilisateurs/{id}
     * Supprime un utilisateur (ADMIN ou BIBLIOTHECAIRE) de la bibliothèque
     */
    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<Map<String, Object>> deleteUtilisateur(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            User admin = userDAO.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            
            if (admin.getBibliotheque() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "No bibliotheque");
                error.put("message", "Vous n'êtes associé à aucune bibliothèque");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Empêcher l'auto-suppression
            if (admin.getId().equals(id)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Cannot delete yourself");
                error.put("message", "Vous ne pouvez pas supprimer votre propre compte");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            User utilisateur = userDAO.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

            // Vérifier que l'utilisateur appartient à la même bibliothèque
            if (utilisateur.getBibliotheque() == null 
                    || utilisateur.getBibliotheque().getId() != admin.getBibliotheque().getId()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Forbidden");
                error.put("message", "Vous ne pouvez supprimer que les utilisateurs de votre bibliothèque");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }


            if (utilisateur.getRole() != Role.BIBLIOTHECAIRE) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Forbidden");
                error.put("message", "Vous ne pouvez supprimer que les bibliothécaires de votre bibliothèque");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            adminService.deleteUser(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Utilisateur supprimé avec succès");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * DELETE /api/bibliotheque-admin/bibliothecaires/{id}
     * Supprime un bibliothécaire de la bibliothèque (compatibilité)
     */
    @DeleteMapping("/bibliothecaires/{id}")
    public ResponseEntity<Map<String, Object>> deleteBibliothecaire(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return deleteUtilisateur(id, currentUser);
    }

    /**
     * PUT /api/bibliotheque-admin/utilisateurs/{id}/toggle-status
     * Active ou désactive un utilisateur (ADMIN ou BIBLIOTHECAIRE)
     */
    @PutMapping("/utilisateurs/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUtilisateurStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            User admin = userDAO.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            
            if (admin.getBibliotheque() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "No bibliotheque");
                error.put("message", "Vous n'êtes associé à aucune bibliothèque");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            User utilisateur = userDAO.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

            // Vérifier que l'utilisateur appartient à la même bibliothèque
            if (utilisateur.getBibliotheque() == null 
                    || utilisateur.getBibliotheque().getId() != admin.getBibliotheque().getId()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Forbidden");
                error.put("message", "Vous ne pouvez modifier que les utilisateurs de votre bibliothèque");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }


            if (utilisateur.getRole() != Role.BIBLIOTHECAIRE) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Forbidden");
                error.put("message", "Vous ne pouvez modifier que les bibliothécaires de votre bibliothèque");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            User updated = adminService.toggleUserStatus(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Statut de l'utilisateur modifié avec succès");
            result.put("user", userToMap(updated));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * PUT /api/bibliotheque-admin/bibliothecaires/{id}/toggle-status
     * Active ou désactive un bibliothécaire (compatibilité)
     */
    @PutMapping("/bibliothecaires/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleBibliothecaireStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return toggleUtilisateurStatus(id, currentUser);
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
