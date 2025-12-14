package com.biblio.controllers;

import com.biblio.controllers.DashboardController;
import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    private final UserDAO userDAO;

    public LoginController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GetMapping("/login")
    public String loginPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String signup,
            Model model) {
        
        // Si l'utilisateur est déjà authentifié, rediriger vers son dashboard personnalisé
        if (userDetails != null) {
            User user = userDAO.findByEmail(userDetails.getUsername())
                    .orElse(null);
            if (user != null) {
                return "redirect:" + DashboardController.getDashboardUrlForRole(user.getRole());
            }
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

