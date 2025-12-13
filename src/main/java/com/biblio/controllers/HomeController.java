package com.biblio.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.biblio.security.JwtService;

@Controller
public class HomeController {
    private final JwtService jwtService;

    public HomeController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user != null) {
            model.addAttribute("username", user.getUsername());
            // Utiliser generateAccessToken() au lieu de generateToken()
            model.addAttribute("token", jwtService.generateAccessToken(user));
        }
        return "index";
    }
}
