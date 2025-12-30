package com.biblio.controllers;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bibliotheque-admin")
@PreAuthorize("hasRole('ADMIN')")
public class BibliothequeAdminPageController {
    private final UserDAO userDAO;

    public BibliothequeAdminPageController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userDAO.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        model.addAttribute("user", user);
        return "dashboard-bibliotheque-admin";
    }
}
