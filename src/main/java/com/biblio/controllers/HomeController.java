package com.biblio.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.biblio.security.JwtService;

import com.biblio.services.BibliothequeService;
import com.biblio.entities.Bibliotheque;
import java.util.List;

@Controller
public class HomeController {
    private final JwtService jwtService;
    private final BibliothequeService bibliothequeService;

    public HomeController(JwtService jwtService, BibliothequeService bibliothequeService) {
        this.jwtService = jwtService;
        this.bibliothequeService = bibliothequeService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user != null) {
            model.addAttribute("username", user.getUsername());
            // Utiliser generateAccessToken() au lieu de generateToken()
            model.addAttribute("token", jwtService.generateAccessToken(user));
        } else {
            // Pour les utilisateurs non authentifiés (internautes)
            List<Bibliotheque> bibliotheques = bibliothequeService.getAllActives();
            model.addAttribute("bibliotheques", bibliotheques);
        }
        return "index";
    }
}
