package com.biblio.controllers;

import com.biblio.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class RegistrationController {
    private final AuthService authService;

    public RegistrationController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/auth/signup-form")
    public String registerForm(
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {
        try {
            authService.register(nom, prenom, email, password);
            model.addAttribute("success", "Compte créé. Vous pouvez vous connecter.");
            return "login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<Map<String, String>> registerJson(@RequestBody Map<String, String> request) {
        try {
            String nom = request.get("nom");
            String prenom = request.get("prenom");
            String email = request.get("email");
            String password = request.get("password");

            if (nom == null || prenom == null || email == null || password == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Tous les champs sont requis"));
            }

            Map<String, String> response = authService.register(nom, prenom, email, password);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
