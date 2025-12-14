package com.biblio.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security 100% STATELESS avec JWT
 * 
 * - Pas de sessions HTTP (SessionCreationPolicy.STATELESS)
 * - CSRF désactivé (JWT est immune à CSRF)
 * - OAuth2 génère des JWT
 * - Toutes les authentifications passent par JWT
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            CustomOAuth2UserService oAuth2UserService,
            OAuth2JwtSuccessHandler oAuth2JwtSuccessHandler,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) throws Exception {
        http
                // CSRF désactivé (JWT est immune à CSRF)
                .csrf(csrf -> csrf.disable())
                
                // Headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                
                // Session Management - STATELESS obligatoire
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Exception Handling - Retourner JSON au lieu de rediriger
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                
                // Autorizations
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics
                        .requestMatchers(
                                "/api/auth/**",           // Login, register, refresh
                                "/oauth2/**",             // OAuth2 authorization
                                "/login/oauth2/**",       // OAuth2 callback
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/h2-console/**"
                        ).permitAll()
                        // Endpoints protégés par rôles
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/bibliothecaire/**").hasRole("BIBLIOTHECAIRE")
                        // Tous les autres endpoints API nécessitent une authentification
                        .requestMatchers("/api/**").authenticated()
                        // Endpoints web (pour compatibilité avec les templates Thymeleaf)
                        .requestMatchers("/", "/login", "/register", "/register/verify", "/oauth2-callback").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/bibliothecaire/**").hasRole("BIBLIOTHECAIRE")
                        .requestMatchers("/usager/**").authenticated() // Accessible à tous les utilisateurs authentifiés
                        .anyRequest().authenticated())
                
                // OAuth2 Login - Génère des JWT
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                        .userService(oAuth2UserService))
                        .successHandler(oAuth2JwtSuccessHandler))
                
                // Authentication Provider - Spring détectera automatiquement les beans UserDetailsService et PasswordEncoder
                // Pas besoin de créer explicitement DaoAuthenticationProvider, Spring le fera automatiquement
                
                // JWT Filter - Doit être avant UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Spring Security 7.0 configure automatiquement DaoAuthenticationProvider
    // à partir des beans UserDetailsService et PasswordEncoder disponibles
    // Pas besoin de créer explicitement le bean AuthenticationProvider

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
