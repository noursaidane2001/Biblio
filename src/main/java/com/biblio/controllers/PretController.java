package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.entities.Pret;
import com.biblio.entities.User;
import com.biblio.services.PretService;
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
@RequestMapping("/api/prets")
public class PretController {

    private final PretService pretService;
    private final UserDAO userDAO;

    public PretController(PretService pretService, UserDAO userDAO) {
        this.pretService = pretService;
        this.userDAO = userDAO;
    }

    @GetMapping("/mes-prets")
    @PreAuthorize("hasRole('USAGER')")
    public ResponseEntity<Map<String, Object>> mesPrets(@AuthenticationPrincipal UserDetails user) {
        List<Pret> prets = pretService.getPretsForUser(user.getUsername());
        List<Map<String, Object>> items = prets.stream().map(this::toDto).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("items", items);
        result.put("total", items.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/a-retirer")
    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
    public ResponseEntity<Map<String, Object>> pretsARetirer(@AuthenticationPrincipal UserDetails currentUser) {
        User bibliothecaire = userDAO.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        if (bibliothecaire.getBibliotheque() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Pas de bibliothèque associée");
            error.put("message", "Aucune bibliothèque n'est associée à votre compte");
            error.put("items", List.of());
            error.put("total", 0);
            return ResponseEntity.ok(error);
        }
        Long bibliothequeId = bibliothecaire.getBibliotheque().getId();
        List<Pret> prets = pretService.getEmpruntePourBibliotheque(bibliothequeId);
        List<Map<String, Object>> items = prets.stream().map(this::toDto).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("items", items);
        result.put("total", items.size());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/emprunte")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN')")
    public ResponseEntity<Map<String, Object>> marquerEmprunte(@PathVariable Long id) {
        Pret pret = pretService.marquerEmprunte(id);
        return ResponseEntity.ok(Map.of("success", true, "pret", toDto(pret)));
    }

    @PutMapping("/{id}/en-cours")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN')")
    public ResponseEntity<Map<String, Object>> mettreEnCours(@PathVariable Long id) {
        Pret pret = pretService.mettreEnCours(id);
        return ResponseEntity.ok(Map.of("success", true, "pret", toDto(pret)));
    }

    @PutMapping("/{id}/retour")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN')")
    public ResponseEntity<Map<String, Object>> retournerLivre(@PathVariable Long id) {
        Pret pret = pretService.retourner(id);
        return ResponseEntity.ok(Map.of("success", true, "pret", toDto(pret)));
    }

    @PutMapping("/{id}/cloture")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN')")
    public ResponseEntity<Map<String, Object>> cloturerPret(@PathVariable Long id) {
        Pret pret = pretService.cloturer(id);
        return ResponseEntity.ok(Map.of("success", true, "pret", toDto(pret)));
    }

    private Map<String, Object> toDto(Pret p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("statut", p.getStatut() != null ? p.getStatut().name() : null);
        map.put("dateReservation", p.getDateReservation());
        map.put("dateEmprunt", p.getDateEmprunt());
        map.put("dateRetourPrevu", p.getDateRetourPrevu());
        map.put("dateRetourEffectif", p.getDateRetourEffectif());
        map.put("dureeEmprunt", p.getDureeEmprunt());
        map.put("prolongations", p.getProlongations());
        map.put("penaliteRetard", p.getPenaliteRetard());
        map.put("retardJours", p.getJoursRetard());
        if (p.getRessource() != null) {
            map.put("ressource", Map.of(
                    "id", p.getRessource().getId(),
                    "titre", p.getRessource().getTitre(),
                    "auteur", p.getRessource().getAuteur()
            ));
        }
        if (p.getUtilisateur() != null) {
            map.put("utilisateur", Map.of(
                    "id", p.getUtilisateur().getId(),
                    "nom", p.getUtilisateur().getNom(),
                    "prenom", p.getUtilisateur().getPrenom(),
                    "email", p.getUtilisateur().getEmail()
            ));
        }
        if (p.getBibliotheque() != null) {
            map.put("bibliotheque", p.getBibliotheque().getNom());
        }
        return map;
    }
}
