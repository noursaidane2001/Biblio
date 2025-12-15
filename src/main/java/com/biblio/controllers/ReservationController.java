package com.biblio.controllers;

import com.biblio.entities.Reservation;
import com.biblio.enums.StatutReservation;
import com.biblio.services.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USAGER')")
    public ResponseEntity<Map<String, Object>> creerReservation(@RequestBody Map<String, Object> body,
                                                                Authentication authentication) {
        try {
            Long ressourceId = body.get("ressourceId") != null ? Long.valueOf(body.get("ressourceId").toString()) : null;
            if (ressourceId == null) {
                return ResponseEntity.badRequest().body(error("ressourceId manquant"));
            }
            Reservation reservation = reservationService.creerReservation(ressourceId, authentication.getName());
            return ResponseEntity.ok(success("reservation", toDto(reservation)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @GetMapping("/mes")
    @PreAuthorize("hasRole('USAGER')")
    public ResponseEntity<Map<String, Object>> mesReservations(Authentication authentication) {
        try {
            List<Map<String, Object>> data = reservationService.listerReservationsUsager(authentication.getName())
                    .stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            Map<String, Object> result = success("reservations", data);
            result.put("total", data.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('USAGER')")
    public ResponseEntity<Map<String, Object>> annuler(@PathVariable Long id, Authentication authentication) {
        try {
            Reservation reservation = reservationService.annulerParUsager(id, authentication.getName());
            return ResponseEntity.ok(success("reservation", toDto(reservation)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> enAttente(Authentication authentication) {
        try {
            List<Map<String, Object>> data = reservationService.listerEnAttentePourBibliotheque(authentication.getName())
                    .stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            Map<String, Object> result = success("reservations", data);
            result.put("total", data.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirmer")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmer(@PathVariable Long id,
                                                         @RequestBody(required = false) Map<String, Object> body,
                                                         Authentication authentication) {
        try {
            String commentaire = body != null ? (String) body.get("commentaire") : null;
            Reservation reservation = reservationService.confirmerReservation(id, authentication.getName(), commentaire);
            return ResponseEntity.ok(success("reservation", toDto(reservation)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> rejeter(@PathVariable Long id,
                                                       @RequestBody(required = false) Map<String, Object> body,
                                                       Authentication authentication) {
        try {
            String raison = body != null ? (String) body.get("raison") : null;
            Reservation reservation = reservationService.rejeterReservation(id, authentication.getName(), raison);
            return ResponseEntity.ok(success("reservation", toDto(reservation)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/marquer-emprunte")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> marquerEmprunte(@PathVariable Long id,
                                                               Authentication authentication) {
        try {
            Reservation reservation = reservationService.marquerEmprunte(id, authentication.getName());
            return ResponseEntity.ok(success("reservation", toDto(reservation)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    private Map<String, Object> toDto(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("statut", r.getStatut() != null ? r.getStatut().name() : null);
        map.put("dateDemande", r.getDateDemande());
        map.put("dateConfirmation", r.getDateConfirmation());
        map.put("dateExpiration", r.getDateExpiration());
        map.put("deadlineRetrait", r.getDeadlineRetrait());
        map.put("commentaire", r.getCommentaire());
        if (r.getRessource() != null) {
            map.put("ressource", Map.of(
                    "id", r.getRessource().getId(),
                    "titre", r.getRessource().getTitre(),
                    "auteur", r.getRessource().getAuteur(),
                    "categorie", r.getRessource().getCategorie() != null ? r.getRessource().getCategorie().name() : null,
                    "typeRessource", r.getRessource().getTypeRessource() != null ? r.getRessource().getTypeRessource().name() : null
            ));
        }
        if (r.getUsager() != null) {
            map.put("usager", Map.of(
                    "id", r.getUsager().getId(),
                    "nom", r.getUsager().getNom(),
                    "prenom", r.getUsager().getPrenom(),
                    "email", r.getUsager().getEmail()
            ));
        }
        if (r.getBibliotheque() != null) {
            map.put("bibliotheque", Map.of(
                    "id", r.getBibliotheque().getId(),
                    "nom", r.getBibliotheque().getNom()
            ));
        }
        return map;
    }

    private Map<String, Object> success(String key, Object value) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put(key, value);
        return res;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("error", message);
        return res;
    }
}
