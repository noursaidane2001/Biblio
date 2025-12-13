package com.biblio.controllers;

import com.biblio.services.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class RegistrationController {
    private final AuthService authService;

    public RegistrationController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Affiche la page d'inscription.
     * Redirige vers le dashboard si l'utilisateur est déjà authentifié.
     */
    @GetMapping("/register")
    public String registerPage(@AuthenticationPrincipal UserDetails userDetails) {
        // Si l'utilisateur est déjà authentifié, rediriger vers le dashboard
        if (userDetails != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    /**
     * Traite le formulaire d'inscription.
     * Crée le compte et envoie un email de vérification.
     */
    @PostMapping("/auth/register-form")
    public String registerForm(
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            HttpServletRequest request) {
        try {
            authService.register(nom, prenom, email, password);
            
            // Stocker le message de succès dans la session
            jakarta.servlet.http.HttpSession session = request.getSession(true);
            session.setAttribute("successMessage", 
                "Inscription réussie ! Veuillez vérifier votre email pour activer votre compte.");
            
            // Rediriger vers la page de login
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors de l'inscription. Veuillez réessayer.");
            return "register";
        }
    }

    /**
     * Vérifie le token d'email et active le compte.
     */
    @GetMapping("/register/verify")
    public String verifyEmail(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        
        if (token == null || token.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Token de vérification invalide.");
            return "email-verification";
        }
        
        try {
            boolean verified = authService.verifyEmail(token);
            if (verified) {
                // Stocker le message de succès dans la session
                jakarta.servlet.http.HttpSession session = request.getSession(true);
                session.setAttribute("successMessage", 
                    "Email vérifié avec succès ! Vous pouvez maintenant vous connecter.");
                return "redirect:/login";
            } else {
                model.addAttribute("errorMessage", 
                    "Token de vérification invalide ou expiré. Veuillez vous réinscrire ou contacter le support.");
                return "email-verification";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", 
                "Erreur lors de la vérification. Veuillez réessayer ou contacter le support.");
            return "email-verification";
        }
    }
}
