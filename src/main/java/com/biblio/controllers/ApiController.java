package com.biblio.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {
    @GetMapping("/api/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> me(@AuthenticationPrincipal UserDetails user) {
        return Map.of(
                "username", user != null ? user.getUsername() : null,
                "roles", user != null ? user.getAuthorities() : null
        );
    }
}
