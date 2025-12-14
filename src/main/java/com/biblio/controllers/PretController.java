package com.biblio.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prets")
public class PretController {

    @GetMapping("/mes-prets")
    @PreAuthorize("hasRole('USAGER')")
    public ResponseEntity<?> mesPrets(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "owner", user != null ? user.getUsername() : null,
                "items", List.of()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN')")
    public ResponseEntity<?> creerPret(@RequestBody Map<String, Object> pret) {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/{id}/retour")
    @PreAuthorize("hasAnyRole('BIBLIOTHECAIRE','ADMIN')")
    public ResponseEntity<?> retournerLivre(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> supprimerPret(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "deleted", id));
    }
}
