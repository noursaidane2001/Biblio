package com.biblio.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;

/**
 * Contrôleur MVC pour les pages Thymeleaf de gestion des ressources
 */
@Controller
@RequestMapping("/ressources")
public class RessourceMvcController {

    private final UserDAO userDAO;

    public RessourceMvcController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * GET /ressources/new
     * Affiche le formulaire de création d'une nouvelle ressource
     * Accessible uniquement aux bibliothécaires
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
    public String showNewRessourceForm(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        // Récupérer l'utilisateur connecté
        User user = userDAO.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));
        
        model.addAttribute("user", user);
        return "new-ressource";
    }

    /**
     * GET /ressources/list
     * Affiche la liste de toutes les ressources
     * Accessible à tous les utilisateurs authentifiés
     */
    @GetMapping("/list")
    public String showRessourcesList(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        // Récupérer l'utilisateur connecté
        User user = userDAO.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));
        
        model.addAttribute("user", user);
        return "list-ressources";
    }
}