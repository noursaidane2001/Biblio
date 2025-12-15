package com.biblio.controllers;

import com.biblio.entities.Reservation;
import com.biblio.services.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bibliothecaire")
@PreAuthorize("hasRole('BIBLIOTHECAIRE')")
public class BibliothecaireController {

    private final ReservationService reservationService;

    public BibliothecaireController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/reservations/en-attente")
    public ResponseEntity<Map<String, Object>> getReservationsEnAttente(
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            List<Reservation> reservations = reservationService.listerEnAttentePourBibliotheque(currentUser.getUsername());
            List<Map<String, Object>> items = reservations.stream()
                    .map(this::reservationToMap)
                    .collect(Collectors.toList());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("reservations", items);
            result.put("total", items.size());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch reservations");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Internal server error");
            error.put("message", "Erreur lors de la récupération des réservations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/reservations/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirmReservation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            String commentaire = body != null ? (String) body.getOrDefault("commentaire", null) : null;
            Reservation r = reservationService.confirmerReservation(id, currentUser.getUsername(), commentaire);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("reservation", reservationToMap(r));
            return ResponseEntity.ok(result);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Confirmation failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Internal server error");
            error.put("message", "Erreur lors de la confirmation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/reservations/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectReservation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            String raison = body != null ? (String) body.getOrDefault("raison", null) : null;
            Reservation r = reservationService.rejeterReservation(id, currentUser.getUsername(), raison);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("reservation", reservationToMap(r));
            return ResponseEntity.ok(result);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Rejection failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Internal server error");
            error.put("message", "Erreur lors du rejet");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private Map<String, Object> reservationToMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        Map<String, Object> ressource = new HashMap<>();
        if (r.getRessource() != null) {
            ressource.put("id", r.getRessource().getId());
            ressource.put("titre", r.getRessource().getTitre());
            ressource.put("auteur", r.getRessource().getAuteur());
        }
        map.put("ressource", ressource);
        Map<String, Object> usager = new HashMap<>();
        if (r.getUsager() != null) {
            usager.put("id", r.getUsager().getId());
            usager.put("nom", r.getUsager().getNom());
            usager.put("prenom", r.getUsager().getPrenom());
            usager.put("email", r.getUsager().getEmail());
        }
        map.put("usager", usager);
        map.put("statut", r.getStatut() != null ? r.getStatut().name() : null);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        map.put("dateDemande", r.getDateDemande() != null ? r.getDateDemande().format(fmt) : null);
        map.put("dateConfirmation", r.getDateConfirmation() != null ? r.getDateConfirmation().format(fmt) : null);
        map.put("deadlineRetrait", r.getDeadlineRetrait() != null ? r.getDeadlineRetrait().format(fmt) : null);
        return map;
    }
}
