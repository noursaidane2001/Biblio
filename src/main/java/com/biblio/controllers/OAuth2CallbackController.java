package com.biblio.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuth2CallbackController {

    /**
     * Page de callback OAuth2
     * Cette page récupère les tokens depuis l'URL (passés par OAuth2JwtSuccessHandler)
     * et les stocke dans localStorage avant de rediriger vers /dashboard
     */
    @GetMapping("/oauth2-callback")
    public String oauth2Callback() {
        return "oauth2-callback";
    }
}
