package com.biblio.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String signup,
            Model model) {
        
        // Si l'utilisateur est déjà authentifié, rediriger vers le dashboard
        if (userDetails != null) {
            return "redirect:/dashboard";
        }
        
        // Ajouter les messages pour les toasts
        if (error != null) {
            model.addAttribute("errorMessage", "Email ou mot de passe incorrect");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Vous avez été déconnecté avec succès");
        }
        if (signup != null && "success".equals(signup)) {
            model.addAttribute("successMessage", "Inscription réussie ! Vous pouvez maintenant vous connecter.");
        }
        
        return "login";
    }
}

