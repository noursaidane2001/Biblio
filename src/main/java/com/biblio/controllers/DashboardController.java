package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import com.biblio.enums.Role;
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

    /**
     * Méthode utilitaire pour obtenir l'URL du dashboard selon le rôle
     */
    public static String getDashboardUrlForRole(Role role) {
        return switch (role) {
            case ADMIN -> "/admin/dashboard";
            case BIBLIOTHECAIRE -> "/bibliothecaire/dashboard";
            case USAGER -> "/usager/dashboard";
        };
    }

    /**
     * Redirige vers le dashboard approprié selon le rôle de l'utilisateur
     */
    @GetMapping("/dashboard")
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

        // Rediriger vers le dashboard approprié selon le rôle
        String dashboardUrl = getDashboardUrlForRole(user.getRole());
        return "redirect:" + dashboardUrl;
    }

    /**
     * Dashboard pour les usagers
     */
    @GetMapping("/usager/dashboard")
    public String usagerDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String success,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String error,
            Model model,
            jakarta.servlet.http.HttpServletRequest request) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = (User) model.getAttribute("currentUser");
        if (user == null) {
            user = userDAO.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        }

        // Vérifier que l'utilisateur est bien un usager
        if (!user.isUsager()) {
            return "redirect:" + getDashboardUrlForRole(user.getRole());
        }

        model.addAttribute("user", user);
        addMessagesToModel(model, request, success, error);
        
        return "dashboard-usager";
    }

    /**
     * Dashboard pour les bibliothécaires
     */
    @GetMapping("/bibliothecaire/dashboard")
    public String bibliothecaireDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String success,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String error,
            Model model,
            jakarta.servlet.http.HttpServletRequest request) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = (User) model.getAttribute("currentUser");
        if (user == null) {
            user = userDAO.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        }

        // Vérifier que l'utilisateur est bien un bibliothécaire
        if (!user.isBibliothecaire()) {
            return "redirect:" + getDashboardUrlForRole(user.getRole());
        }

        model.addAttribute("user", user);
        addMessagesToModel(model, request, success, error);
        
        return "dashboard-bibliothecaire";
    }

    /**
     * Dashboard pour les administrateurs
     */
    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String success,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String error,
            Model model,
            jakarta.servlet.http.HttpServletRequest request) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = (User) model.getAttribute("currentUser");
        if (user == null) {
            user = userDAO.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        }

        // Vérifier que l'utilisateur est bien un administrateur
        if (!user.isAdmin()) {
            return "redirect:" + getDashboardUrlForRole(user.getRole());
        }

        model.addAttribute("user", user);
        addMessagesToModel(model, request, success, error);
        
        return "dashboard-admin";
    }

    /**
     * Méthode utilitaire pour ajouter les messages au modèle
     */
    private void addMessagesToModel(Model model, jakarta.servlet.http.HttpServletRequest request,
                                   String success, String error) {
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
    }
}

