package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserController {
    private final UserDAO userDAO;

    public UserController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        // Réutiliser le currentUser déjà chargé par UserModelAttribute pour éviter une requête supplémentaire
        User user = (User) model.getAttribute("currentUser");
        if (user == null) {
            // Fallback si currentUser n'est pas présent (ne devrait pas arriver normalement)
            user = userDAO.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        }

        model.addAttribute("user", user);
        return "profile";
    }
}

