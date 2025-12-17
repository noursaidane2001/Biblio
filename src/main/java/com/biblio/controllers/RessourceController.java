package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.dto.CreateRessourceRequest;
import com.biblio.entities.Ressource;
import com.biblio.entities.User;
import com.biblio.services.RessourceService;
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
@RequestMapping("/api/ressources")
public class RessourceController {
    private final RessourceService ressourceService;
    private final UserDAO userDAO;

    public RessourceController(RessourceService ressourceService, UserDAO userDAO) {
        this.ressourceService = ressourceService;
        this.userDAO = userDAO;
    }

    /**
     * POST /api/ressources
     * Crée une nouvelle ressource (accessible aux bibliothécaires uniquement)
     * La ressource est automatiquement associée à la bibliothèque du bibliothécaire
     */
    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
    public ResponseEntity<Map<String, Object>> createRessource(
            @Valid @RequestBody CreateRessourceRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            Ressource ressource = ressourceService.createRessource(
                    request.titre(),
                    request.auteur(),
                    request.isbn(),
                    request.categorie(),
                    request.typeRessource(),
                    request.description(),
                    request.editeur(),
                    request.datePublication(),
                    request.nombreExemplaires(),
                    request.exemplairesDisponibles(),
                    request.imageCouverture(),
                    currentUser.getUsername()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Ressource créée avec succès");
            result.put("ressource", ressourceToMap(ressource));
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to create ressource");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to create ressource");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/ressources
     * Liste toutes les ressources (filtrées par bibliothèque pour les bibliothécaires)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAllRessources(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            List<Ressource> ressourcesList;

            if (currentUser != null) {
                User user = userDAO.findByEmail(currentUser.getUsername()).orElse(null);
                if (user != null && user.isBibliothecaire() && user.getBibliotheque() != null) {
                    ressourcesList = ressourceService.getRessourcesByBibliotheque(user.getBibliotheque().getId());
                } else {
                    ressourcesList = ressourceService.getAllRessources();
                }
            } else {
                // Pour les utilisateurs non authentifiés ou autres cas, on retourne tout ou rien selon la politique.
                // Ici on retourne tout pour l'instant (comportement par défaut)
                ressourcesList = ressourceService.getAllRessources();
            }

            List<Map<String, Object>> ressources = ressourcesList.stream()
                    .map(this::ressourceToMap)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("ressources", ressources);
            result.put("total", ressources.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch ressources");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/ressources/{id}
     * Récupère une ressource par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getRessourceById(@PathVariable Long id) {
        try {
            Ressource ressource = ressourceService.getById(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("ressource", ressourceToMap(ressource));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Ressource not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * GET /api/ressources/bibliotheque/{bibliothequeId}
     * Liste toutes les ressources d'une bibliothèque
     */
    @GetMapping("/bibliotheque/{bibliothequeId}")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getRessourcesByBibliotheque(@PathVariable Long bibliothequeId) {
        try {
            List<Map<String, Object>> ressources = ressourceService.getRessourcesByBibliotheque(bibliothequeId).stream()
                    .map(this::ressourceToMap)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("ressources", ressources);
            result.put("total", ressources.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch ressources");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private Map<String, Object> ressourceToMap(Ressource ressource) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ressource.getId());
        map.put("titre", ressource.getTitre());
        map.put("auteur", ressource.getAuteur());
        map.put("isbn", ressource.getIsbn());
        map.put("categorie", ressource.getCategorie() != null ? ressource.getCategorie().name() : null);
        map.put("typeRessource", ressource.getTypeRessource() != null ? ressource.getTypeRessource().name() : null);
        map.put("description", ressource.getDescription());
        map.put("editeur", ressource.getEditeur());
        map.put("datePublication", ressource.getDatePublication());
        map.put("nombreExemplaires", ressource.getNombreExemplaires());
        map.put("exemplairesDisponibles", ressource.getExemplairesDisponibles());
        map.put("imageCouverture", ressource.getImageCouverture());
        map.put("popularite", ressource.getPopularite());
        map.put("dateAjout", ressource.getDateAjout());
        if (ressource.getBibliotheque() != null) {
            map.put("bibliotheque", Map.of(
                    "id", ressource.getBibliotheque().getId(),
                    "nom", ressource.getBibliotheque().getNom()
            ));
        }
        return map;
    }
}
