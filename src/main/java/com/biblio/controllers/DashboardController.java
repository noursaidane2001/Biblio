package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final UserDAO userDAO;

    public DashboardController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public static String getDashboardUrlForRole(com.biblio.enums.Role role) {
        if (role == null) return "/dashboard";
        return switch (role) {
            case USAGER -> "/usager/dashboard";
            case BIBLIOTHECAIRE -> "/bibliothecaire/dashboard";
            case ADMIN -> "/bibliotheque-admin/dashboard";
            case SUPER_ADMIN -> "/super-admin/dashboard";
        };
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String success,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String error,
            Model model,
            jakarta.servlet.http.HttpServletRequest request) {
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
        
        // Récupérer les messages depuis la session
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            String successMsg = (String) session.getAttribute("successMessage");
            if (successMsg != null) {
                model.addAttribute("successMessage", successMsg);
                session.removeAttribute("successMessage"); // Nettoyer après utilisation
            }
            
            String errorMsg = (String) session.getAttribute("errorMessage");
            if (errorMsg != null) {
                model.addAttribute("errorMessage", errorMsg);
                session.removeAttribute("errorMessage"); // Nettoyer après utilisation
            }
        }
        
        // Récupérer depuis query params si présents
        if (success != null && !success.isEmpty() && !model.containsAttribute("successMessage")) {
            model.addAttribute("successMessage", success);
        }
        if (error != null && !error.isEmpty() && !model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", error);
        }
        
        return "dashboard";
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public String profile(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = (User) model.getAttribute("currentUser");
        if (user == null) {
            user = userDAO.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        }
        model.addAttribute("user", user);
        return "profile";
    }
}

